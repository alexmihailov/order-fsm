package com.witcher.order

import akka.actor.FSM.{CurrentState, SubscribeTransitionCallBack, Transition}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import java.util.UUID

class OrderFsmActorTest extends TestKit(ActorSystem("order-system"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Order FSM actor" in {
    val orderFsmActor = system.actorOf(Props(classOf[OrderFsmActor], testActor))
    val itemIds = List.fill(3) { UUID.randomUUID() }

    // switch New state after creation
    orderFsmActor ! SubscribeTransitionCallBack(testActor)
    expectMsg(CurrentState(orderFsmActor, New))

    orderFsmActor ! OrderConfirmation(itemIds)
    expectMsg(StoreServiceRequest(orderFsmActor, itemIds))  // send StoreServiceRequest msg
    expectMsg(Transition(orderFsmActor, New, Ordered))      // switch to the Ordered state

    orderFsmActor ! CancelOrder
    expectMsg(Transition(orderFsmActor, Ordered, Canceled)) // switch to the Canceled state
  }
}
