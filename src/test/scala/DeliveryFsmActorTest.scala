package com.witcher.order

import akka.actor.FSM.{CurrentState, SubscribeTransitionCallBack, Transition}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funspec.AnyFunSpecLike
import org.scalatest.matchers.must.Matchers

import java.util.UUID
import scala.concurrent.duration.DurationInt

class DeliveryFsmActorTest extends TestKit(ActorSystem("order-system"))
  with ImplicitSender
  with AnyFunSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  describe("Delivery FSM actor") {
    val config = DeliveryFsmActor.Config(3, 3.seconds, 2.seconds)
    val orderId = UUID.randomUUID()
    describe("in success cases short case") {
      val deliveryFsmActor = system.actorOf(Props(classOf[DeliveryFsmActor], config, testActor, testActor, orderId))

      it("must create order in IDLE state") {
        deliveryFsmActor ! SubscribeTransitionCallBack(testActor)
        expectMsg(CurrentState(deliveryFsmActor, Idle))
      }

      it("must send order data to the delivery system") {
        deliveryFsmActor ! DeliveryServiceRequest
        expectMsg(DeliveryRequest(deliveryFsmActor, orderId))
      }

      it("must moves to the WAIT_RESPONSE state from the IDLE state") {
        expectMsg(Transition(deliveryFsmActor, Idle, WaitResponse))
      }

      it("must send response to the order actor") {
        deliveryFsmActor ! DeliveryResponse(true)
        expectMsg(DeliveryServiceResponse(true))
      }

      it("must moves to the SUCCESS_EXIT state from the WAIT_RESPONSE state") {
        expectMsg(Transition(deliveryFsmActor, WaitResponse, SuccessExit))
      }
    }
    describe("in success cases with retry") {
      val deliveryFsmActor = system.actorOf(Props(classOf[DeliveryFsmActor], config, testActor, testActor, orderId))

      it("must create order in IDLE state") {
        deliveryFsmActor ! SubscribeTransitionCallBack(testActor)
        expectMsg(CurrentState(deliveryFsmActor, Idle))
      }

      it("must send order data to the delivery system - 1") {
        deliveryFsmActor ! DeliveryServiceRequest
        expectMsg(DeliveryRequest(deliveryFsmActor, orderId))
      }

      it("must moves to the WAIT_RESPONSE state from the IDLE state") {
        expectMsg(Transition(deliveryFsmActor, Idle, WaitResponse))
      }

      it("must moves to the WAIT_DELAY state from the WAIT_RESPONSE state") {
        deliveryFsmActor ! DeliveryResponse(false)
        expectMsg(Transition(deliveryFsmActor, WaitResponse, WaitDelay))
      }

      it("must send order data to the delivery system - 2") {
        expectMsg(config.requestsDelay + 1.seconds, DeliveryRequest(deliveryFsmActor, orderId))
      }

      it("must moves to the WAIT_RESPONSE state from the WAIT_DELAY state") {
        expectMsg(Transition(deliveryFsmActor, WaitDelay, WaitResponse))
      }

      it("must send order data to the delivery system - 3") {
        expectMsg(config.requestTimeout + 1.seconds, DeliveryRequest(deliveryFsmActor, orderId))
      }

      it("must moves to the WAIT_RESPONSE state from the WAIT_RESPONSE state") {
        expectMsg(config.requestTimeout + 1.seconds, Transition(deliveryFsmActor, WaitResponse, WaitResponse))
      }

      it("must send response to the order actor") {
        expectMsg(config.requestTimeout + 1.seconds, DeliveryServiceResponse(false))
      }

      it("must moves to the FAILED_EXIT state from the WAIT_RESPONSE state") {
        expectMsg(Transition(deliveryFsmActor, WaitResponse, FailedExit))
      }
    }
  }
}
