package com.witcher
package ws

import delivery.DeliveryFsmActor
import integration.delivery.DeliveryServiceActor
import integration.notify.NotifyServiceActor
import integration.store.StoreServiceActor
import order.{OrderConfirmation, OrderFsmActor, OrderState}

import akka.{Done, NotUsed}
import akka.actor.FSM.{CurrentState, SubscribeTransitionCallBack, Transition}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives.{handleWebSocketMessages, path}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.io.StdIn

object WsMain {

  implicit val system: ActorSystem = ActorSystem("order-system")
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = system.dispatcher

  private val globalConfig = ConfigFactory.load()

  private val storeService = system.actorOf(Props(classOf[StoreServiceActor]), "store-service")
  private val deliveryService = system.actorOf(Props(classOf[DeliveryServiceActor]), "delivery-service")
  private val notifyService = system.actorOf(Props(classOf[NotifyServiceActor]), "notify-service")
  private val orderFsm = createOrderFsmActor()

  private val websocketRoute = path("order") {
    handleWebSocketMessages(orderFsmWatch)
  }

  private val commands: Map[String, Runnable] = Map (
    "order-confirmation" -> (() => orderFsm ! OrderConfirmation(List.fill(3) {UUID.randomUUID()}))
  )

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

    def run(x: Option[Runnable]): Unit = x match {
      case Some(r) => r.run()
    }

    val source: Source[Message, NotUsed] = Source.actorRef(
      bufferSize = 10, overflowStrategy = OverflowStrategy.dropTail
    ).map((c : Any) => c match {
      case CurrentState(_, state : OrderState) => state.toString
      case Transition(_, from: OrderState, to: OrderState) => s"$from,$to"
      case _ => ""
    })
      .map((c : Any) => TextMessage(c.toString))
      .mapMaterializedValue { wsHandle =>
        orderFsm ! SubscribeTransitionCallBack(wsHandle)
        NotUsed
      }
    val sink: Sink[Message, Future[Done]] = Sink
      .foreach {
        case TextMessage.Strict(text) => run(commands.get(text))
      }
    Flow.fromSinkAndSource(sink, source)
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
    system.actorOf(
      Props(classOf[OrderFsmActor], UUID.randomUUID(), orderConfig, storeService, deliveryService, notifyService),
      "order-fsm"
    )
  }
}