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
  ): ActorRef =
    system.actorOf(Props(classOf[OrderFsmActor], orderId, reservedTimeout,
      storeServiceActor, deliveryServiceActor, notifyServiceActor))

  describe("Order FSM actor") {
    describe("in success cases") {
      val orderId = UUID.randomUUID()
      val itemIds = List.fill(3) { UUID.randomUUID() }
      val orderFsmActor = createOrderFsmActor(orderId, 1.seconds, testActor, testActor, testActor)

      it("must create order in NEW status") {
        orderFsmActor ! SubscribeTransitionCallBack(testActor)
        expectMsg(CurrentState(orderFsmActor, New))
      }

      it("must check if the desired goods are in store and reserve them") {
        orderFsmActor ! OrderConfirmation(itemIds)
        expectMsg(StoreServiceRequest(orderFsmActor, itemIds))
      }

      it("must moves to the ORDERED status from the NEW status") {
        expectMsg(Transition(orderFsmActor, New, Ordered))
      }

      it("must moves to the RESERVED status from the ORDERED status") {
        orderFsmActor ! StoreServiceResponse(true)
        expectMsg(Transition(orderFsmActor, Ordered, Reserved))
      }

      it("must send order data to the delivery system") {
        orderFsmActor ! OrderPayed
        expectMsg(DeliveryServiceRequest(orderFsmActor, orderId))
      }

      it("must moves to the PAYED status from the RESERVED status") {
        expectMsg(Transition(orderFsmActor, Reserved, Payed))
      }

      it("must moves to the SHIPPED status from the PAYED status") {
        orderFsmActor ! DeliveryServiceResponse(true)
        expectMsg(Transition(orderFsmActor, Payed, Shipped))
      }

      it("must send a notification to the user about the arrival of the order at the pickup point") {
        orderFsmActor ! OrderInPickupPoint
        expectMsg(UserNotify(orderId))
      }

      it("must moves to the DELIVERED status from the SHIPPED status") {
        expectMsg(Transition(orderFsmActor, Shipped, Delivered))
      }

      it("must moves to the COMPLETED status from the DELIVERED status") {
        orderFsmActor ! OrderReceived
        expectMsg(Transition(orderFsmActor, Delivered, Completed))
      }
    }
    describe("must moves to the CANCELLED status from the ORDERED status") {
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
    describe("must moves to the CANCELLED status from the RESERVED status") {
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
  }
}
