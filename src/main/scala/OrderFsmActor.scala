package com.witcher.order

import akka.actor.{ActorRef, FSM}

class OrderFsmActor(
  val storeServiceActor: ActorRef
) extends FSM[OrderState, OrderData] {

  // Указываем начальное состояние и начальные данные.
  startWith(New, Uninitialized)

  // Функции переходов.
  when(New) {
    case Event(OrderConfirmation(ids), Uninitialized) =>
      goto(Ordered).using(OrderDetail(ids))
  }
  when(Ordered) {
    case Event(CancelOrder, _) => goto(Canceled)
    case Event(StoreServiceResponse(success), _) =>
      if (success) goto(Reserved) else goto(Canceled)
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
  // т.е. выполняется onTransition, а затем отправляется Transition сообщение наблюдателю.
  onTransition {
    case New -> Ordered => nextStateData match {
      case OrderDetail(ids) =>
        storeServiceActor ! StoreServiceRequest(self, ids)
      case _ => // nothing to do
    }
  }

  initialize()
}
