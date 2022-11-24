package com.witcher.order

import akka.actor.ActorRef

import java.util.UUID

sealed trait DeliveryEvent
case class DeliveryRequest(reply: ActorRef, orderId: UUID) extends DeliveryEvent
case class DeliveryResponse(success: Boolean) extends DeliveryEvent
case object StopDelivery extends DeliveryEvent
