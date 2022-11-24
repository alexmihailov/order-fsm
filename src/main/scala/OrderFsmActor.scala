package com.witcher.order

import akka.actor.{ActorRef, FSM}

import java.util.UUID
import scala.concurrent.duration.FiniteDuration

class OrderFsmActor(
  orderId: UUID,
  val reservedTimeout: FiniteDuration,
  val storeServiceActor: ActorRef,
  val deliveryServiceActor: ActorRef,
  val notifyServiceActor: ActorRef,
) extends FSM[OrderState, OrderData] {

  // Указываем начальное состояние и начальные данные.
  startWith(New, Uninitialized(orderId))

  // Функции переходов.
  when(New) {
    case Event(OrderConfirmation(itemIds), Uninitialized(id)) =>
      goto(Ordered).using(OrderDetail(id, itemIds))
  }
  when(Ordered) {
    case Event(CancelOrder, _) => goto(Canceled)
    case Event(StoreServiceResponse(success), _) =>
      if (success) goto(Reserved) else goto(Canceled)
  }
  when(Reserved, stateTimeout = reservedTimeout) {
    case Event(OrderPayed, _) => goto(Payed)
    case Event(CancelOrder | StateTimeout, _) => goto(Canceled)
  }
  when(Payed) {
    case Event(DeliveryServiceResponse(success), _) =>
      if (success) goto(Shipped) else goto(Payed)
  }
  when(Shipped) {
    case Event(OrderInPickupPoint, _) => goto(Delivered)
  }
  when(Delivered) {
    case Event(OrderReceived, _) => goto(Completed)
  }
  when(Completed) {
    case Event(_, _) => stay()
  }
  when(Canceled) {
    case Event(_, _) => stay()
  }
  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay()
  }

  // Действия при переходе.
  // Вначале будет выполнено действие при переходе, а затем установлено новое состояние,
  // т.е. выполняется onTransition, а затем отправляется akka.actor.FSM.Transition наблюдателю.
  onTransition {
    case New -> Ordered => nextStateData match {
      case OrderDetail(_, itemIds) =>
        storeServiceActor ! StoreServiceRequest(self, itemIds)
      case _ => // nothing to do
    }
    case Reserved -> Payed => stateData match {
      case OrderDetail(id, _) =>
        deliveryServiceActor ! DeliveryServiceRequest(self, id)
      case _ => // nothing to do
    }
    case Reserved -> Canceled => stateData match {
      case OrderDetail(_, itemIds) =>
        storeServiceActor ! CancelReservation(itemIds)
      case _ => // nothing to do
    }
    case Reserved -> Reserved => stateData match {
      case OrderDetail(id, _) =>
        deliveryServiceActor ! DeliveryServiceRequest(self, id)
      case _ => // nothing to do
    }
    case Shipped -> Delivered => stateData match {
      case OrderDetail(id, _) =>
        notifyServiceActor ! UserNotify(id)
      case _ => // nothing to do
    }
  }

  initialize()
}
