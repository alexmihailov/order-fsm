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
    val orderId = UUID.randomUUID()
    val orderFsmActor = system.actorOf(Props(classOf[OrderFsmActor], orderId, testActor, testActor, testActor))
    val itemIds = List.fill(3) { UUID.randomUUID() }

    // Создаем заказ со статусом NEW.
    orderFsmActor ! SubscribeTransitionCallBack(testActor)
    expectMsg(CurrentState(orderFsmActor, New))

    // Отправка подтверждения дальнейшей обработки заказа.
    orderFsmActor ! OrderConfirmation(itemIds)
    // Нужно проверить, имеется ли нужные товары на складах и зарезервировать их.
    expectMsg(StoreServiceRequest(orderFsmActor, itemIds))
    // Заказ переходит в статус ORDERED из статуса NEW.
    expectMsg(Transition(orderFsmActor, New, Ordered))

    // Отправка успеха резервации товаров.
    orderFsmActor ! StoreServiceResponse(true)
    // Заказ переходит в статус RESERVED из статуса ORDERED.
    expectMsg(Transition(orderFsmActor, Ordered, Reserved))

    // Пользователь оплатил заказ.
    orderFsmActor ! OrderPayed
    // Отправить данные по заказу в систему доставки.
    expectMsg(DeliveryServiceRequest(orderFsmActor, orderId))
    // Заказ переходит в статус PAYED из статуса RESERVED.
    expectMsg(Transition(orderFsmActor, Reserved, Payed))

    // Система доставки разместила заказ.
    orderFsmActor ! DeliveryServiceResponse(true)
    // Заказ переходит в статус SHIPPED из статуса PAYED.
    expectMsg(Transition(orderFsmActor, Payed, Shipped))

    // Заказ прибыл в пункт выдачи.
    orderFsmActor ! OrderInPickupPoint
    // Нотификация пользователя о прибытии заказа в пункт выдачи.
    expectMsg(UserNotify(orderId))
    // Заказ переходит в статус DELIVERED из статуса SHIPPED.
    expectMsg(Transition(orderFsmActor, Shipped, Delivered))

    // Пользователь получил заказ.
    orderFsmActor ! OrderReceived
    // Заказ переходит в статус COMPLETED из статуса DELIVERED.
    expectMsg(Transition(orderFsmActor, Delivered, Completed))
  }

// TODO: отдельно проверять кейсы отмены заказа
//  orderFsmActor ! CancelOrder
//  expectMsg(Transition(orderFsmActor, Ordered, Canceled)) // switch to the Canceled state

// TODO: Если получена ошибка при отправке заказа в службу доставки, то нужно повторить запрос N раз через M промежутки времени.
//  Если и в этом случаем получаем ошибку, то направить уведомление администратору.
}
