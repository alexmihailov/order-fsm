package com.witcher
package integration.delivery

import akka.actor.Actor

class DeliveryServiceActor extends Actor {
  override def receive: Receive = {
    case DeliveryRequest(reply, _) => reply ! DeliveryResponse(true)
  }
}
