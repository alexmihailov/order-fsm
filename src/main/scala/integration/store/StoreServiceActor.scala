package com.witcher
package integration.store

import order.CancelReservation
import akka.actor.Actor

class StoreServiceActor extends Actor {

  override def receive: Receive = {
    case StoreServiceRequest(reply, _) => reply ! StoreServiceResponse(true)
    case CancelReservation(_) =>
  }
}
