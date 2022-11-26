package com.witcher
package integration.delivery

import akka.actor.Actor
import akka.event.LoggingReceive

class DeliveryServiceActor extends Actor {
  override def receive: Receive = LoggingReceive {
    case DeliveryRequest(reply, _) => reply ! DeliveryResponse(true)
  }
}
