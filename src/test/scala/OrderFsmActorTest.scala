package com.witcher.order

import akka.actor.FSM.{CurrentState, SubscribeTransitionCallBack, Transition}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class OrderFsmActorTest extends TestKit(ActorSystem("order-system"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Order actor created" must {
    "NEW state" in {
      val orderFsmActor = system.actorOf(Props(classOf[OrderFsmActor]))
      orderFsmActor ! SubscribeTransitionCallBack(testActor)
      expectMsg(CurrentState(orderFsmActor, New))
    }
  }
}
