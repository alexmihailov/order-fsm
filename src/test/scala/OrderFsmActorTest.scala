package com.witcher.order

import akka.actor.FSM.{CurrentState, SubscribeTransitionCallBack, Transition}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class OrderFsmActorTest extends TestKit(ActorSystem("order-system"))
  with ImplicitSender
  with AnyFunSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  private val emptyActor = system.actorOf(Props.empty)
  private def createOrderFsmActor(
    orderId: UUID = UUID.randomUUID(),
    reservedTimeout: FiniteDuration = 1.seconds,
    storeServiceActor: ActorRef = emptyActor,
    deliveryServiceActor: ActorRef = emptyActor,
    notifyServiceActor: ActorRef = emptyActor,
  ): ActorRef = {
    val deliveryConfig = DeliveryFsmActor.Config(5, 3.seconds, 2.seconds)
    val orderConfig = OrderFsmActor.Config(reservedTimeout, deliveryConfig)
    system.actorOf(Props(classOf[OrderFsmActor], orderId, orderConfig, storeServiceActor, deliveryServiceActor, notifyServiceActor))
  }

  describe("Order FSM actor") {
    describe("in success cases") {
      val orderId = UUID.randomUUID()
      val itemIds = List.fill(3) { UUID.randomUUID() }
      val orderFsmActor = createOrderFsmActor(orderId, 1.seconds, testActor, testActor, testActor)

      it("must create order in NEW state") {
        orderFsmActor ! SubscribeTransitionCallBack(testActor)
        expectMsg(CurrentState(orderFsmActor, New))
      }

      it("must check if the desired goods are in store and reserve them") {
        orderFsmActor ! OrderConfirmation(itemIds)
        expectMsg(StoreServiceRequest(orderFsmActor, itemIds))
      }

      it("must moves to the ORDERED state from the NEW state") {
        expectMsg(Transition(orderFsmActor, New, Ordered))
      }

      it("must moves to the RESERVED state from the ORDERED state") {
        orderFsmActor ! StoreServiceResponse(true)
        expectMsg(Transition(orderFsmActor, Ordered, Reserved))
      }

      it("must moves to the PAYED state from the RESERVED state") {
        orderFsmActor ! OrderPayed
        expectMsg(Transition(orderFsmActor, Reserved, Payed))
      }

      it("must send order data to the delivery system") {
        expectMsgType[DeliveryRequest](1.seconds)
      }

      it("must moves to the SHIPPED state from the PAYED state") {
        orderFsmActor ! DeliveryServiceResponse(true)
        expectMsg(Transition(orderFsmActor, Payed, Shipped))
      }

      it("must send a notification to the user about the arrival of the order at the pickup point") {
        orderFsmActor ! OrderInPickupPoint
        expectMsg(UserNotify(orderId))
      }

      it("must moves to the DELIVERED state from the SHIPPED state") {
        expectMsg(Transition(orderFsmActor, Shipped, Delivered))
      }

      it("must moves to the COMPLETED state from the DELIVERED state") {
        orderFsmActor ! OrderReceived
        expectMsg(Transition(orderFsmActor, Delivered, Completed))
      }
    }
    describe("must moves to the CANCELLED state from the ORDERED state") {
      val createActor = () => {
        val orderFsmActor = createOrderFsmActor()
        orderFsmActor ! OrderConfirmation(List.empty)
        orderFsmActor ! SubscribeTransitionCallBack(testActor)
        expectMsg(CurrentState(orderFsmActor, Ordered))
        orderFsmActor
      }
      it("if user canceling order") {
        val orderFsmActor = createActor()
        orderFsmActor ! CancelOrder
        expectMsg(Transition(orderFsmActor, Ordered, Canceled))
      }
      it ("if the items not available in warehouses") {
        val orderFsmActor = createActor()
        orderFsmActor ! StoreServiceResponse(false)
        expectMsg(Transition(orderFsmActor, Ordered, Canceled))
      }
    }
    describe("must moves to the CANCELLED state from the RESERVED state") {
      val itemIds = List.fill(2) { UUID.randomUUID() }
      val timeout = 2.seconds
      val createActor = () => {
        val orderFsmActor = createOrderFsmActor(storeServiceActor = testActor, reservedTimeout = timeout)
        orderFsmActor ! OrderConfirmation(itemIds)
        expectMsg(StoreServiceRequest(orderFsmActor, itemIds))
        orderFsmActor ! StoreServiceResponse(true)
        orderFsmActor ! SubscribeTransitionCallBack(testActor)
        expectMsg(CurrentState(orderFsmActor, Reserved))
        orderFsmActor
      }
      it("if user canceling order") {
        val orderFsmActor = createActor()
        orderFsmActor ! CancelOrder
        expectMsg(CancelReservation(itemIds))
        expectMsg(Transition(orderFsmActor, Reserved, Canceled))
      }
      it("if the user has not made a payment within 2 seconds") {
        val orderFsmActor = createActor()
        expectMsg(timeout + 1.seconds, CancelReservation(itemIds))
        expectMsg(Transition(orderFsmActor, Reserved, Canceled))
      }
    }
    it("must send a notification to the manager") {
      val itemIds = List.fill(2) { UUID.randomUUID() }
      val orderId = UUID.randomUUID()
      val orderFsmActor = createOrderFsmActor(orderId, notifyServiceActor = testActor)
      orderFsmActor ! OrderConfirmation(itemIds)
      orderFsmActor ! StoreServiceResponse(true)
      orderFsmActor ! OrderPayed
      orderFsmActor ! SubscribeTransitionCallBack(testActor)
      expectMsg(CurrentState(orderFsmActor, Payed))
      orderFsmActor ! DeliveryServiceResponse(false)
      expectMsg(ManagerNotify(orderId))
    }
  }
}
