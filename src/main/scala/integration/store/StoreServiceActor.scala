package com.witcher
package integration.store

import order.CancelReservation

import akka.actor.Actor
import akka.event.LoggingReceive

class StoreServiceActor extends Actor {

  override def receive: Receive = LoggingReceive {
    case StoreServiceRequest(reply, _) => reply ! StoreServiceResponse(true)
    case CancelReservation(_) =>
  }
}
