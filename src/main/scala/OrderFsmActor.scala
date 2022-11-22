package com.witcher.order

import akka.actor.FSM

class OrderFsmActor extends FSM[OrderState, OrderData] {

  startWith(New, Uninitialized)

  when(New) {
    case Event(OrderConfirmation, Uninitialized) => goto(Ordered).using(OrderDetail)
  }
  when(Ordered){
    case Event(e, s) => stay()
  }
  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay()
  }
  initialize()
}
