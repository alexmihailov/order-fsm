package com.witcher
package ws

import delivery.DeliveryFsmActor
import integration.delivery.DeliveryServiceActor
import integration.notify.NotifyServiceActor
import integration.store.StoreServiceActor
import order._

import akka.actor.FSM.{CurrentState, SubscribeTransitionCallBack, Transition}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.{Done, NotUsed}
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

import java.util.UUID
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

/**
 * Входная точка в приложение.
 * Запускает систему акторов и начинает слушать websocket.
 * Через websocket будут отправляться команды для актора обработки заказа.
 */
object WsMain {

  implicit val system: ActorSystem = ActorSystem("order-system")
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher

  private val globalConfig = ConfigFactory.load()

  private var storeService: ActorRef = ActorRef.noSender
  private var deliveryService: ActorRef = ActorRef.noSender
  private var notifyService: ActorRef = ActorRef.noSender
  private var orderFsm = createOrderFsmActor()

  private val websocketRoute = path("order") {
    handleWebSocketMessages(orderFsmWatch)
  }

  def main(args: Array[String]): Unit = {
    val bindingFuture = Http().newServerAt(
      globalConfig.as[String]("http.host"),
      globalConfig.as[Int]("http.port")
    ).bind(websocketRoute)
    println(s"Server now online. Please navigate to http://localhost:8080/order\nPress RETURN to stop...")
    StdIn.readLine()
    bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())
  }

  private def orderFsmWatch: Flow[Message, Message, Any] = {
    val source: Source[Message, NotUsed] = Source.actorRef(
      bufferSize = 10, overflowStrategy = OverflowStrategy.dropTail
    ).map((c : Any) => c match {
      case CurrentState(_, state : OrderState) => state.toString
      case Transition(_, from: OrderState, to: OrderState) => s"$from,$to"
      case _ => ""
    }).map(c => TextMessage(c))
      .mapMaterializedValue { wsHandle =>
        orderFsm ! SubscribeTransitionCallBack(wsHandle)
        NotUsed
      }
    val sink: Sink[Message, Future[Done]] = Sink
      .foreach { case TextMessage.Strict(command) => runCommand(command) }
    Flow.fromSinkAndSource(sink, source)
  }

  private def runCommand(command: String): Unit = {
    command match {
      case "order-confirmation" => orderFsm ! OrderConfirmation(List.fill(3) {UUID.randomUUID() })
      case "order-pay" => orderFsm ! OrderPayed
      case "order-in-pickup-point" => orderFsm ! OrderInPickupPoint
      case "order-received" => orderFsm ! OrderReceived
      case "order-restart" =>
        system.stop(orderFsm)
        system.stop(storeService)
        system.stop(deliveryService)
        system.stop(notifyService)
        orderFsm = createOrderFsmActor()
      case _ => system.log.error("Unknown command!")
    }
  }

  private def createOrderFsmActor(): ActorRef = {
    val deliveryConfig = DeliveryFsmActor.Config(
      maxRetryCount = globalConfig.as[Int]("order.delivery.max-retry-count"),
      requestsDelay = globalConfig.as[FiniteDuration]("order.delivery.requests-delay"),
      requestTimeout = globalConfig.as[FiniteDuration]("order.delivery.request-timeout")
    )
    val orderConfig = OrderFsmActor.Config(
      reservedTimeout = globalConfig.as[FiniteDuration]("order.reserved-timeout"),
      deliveryConfig
    )
    val timestamp = System.currentTimeMillis()
    storeService = system.actorOf(Props(classOf[StoreServiceActor]), s"store-service-$timestamp")
    deliveryService = system.actorOf(Props(classOf[DeliveryServiceActor]), s"delivery-service-$timestamp")
    notifyService = system.actorOf(Props(classOf[NotifyServiceActor]), s"notify-service-$timestamp")
    system.actorOf(
      Props(classOf[OrderFsmActor], UUID.randomUUID(), orderConfig, storeService, deliveryService, notifyService),
      s"order-fsm-$timestamp"
    )
  }
}
