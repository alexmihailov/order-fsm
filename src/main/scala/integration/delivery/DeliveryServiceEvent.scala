package com.witcher
package integration.delivery

import akka.actor.ActorRef

import java.util.UUID

sealed trait DeliveryServiceEvent
case class DeliveryRequest(reply: ActorRef, orderId: UUID) extends DeliveryServiceEvent
case class DeliveryResponse(success: Boolean) extends DeliveryServiceEvent

