package com.witcher.order

import akka.actor.ActorRef

import java.util.UUID

sealed trait OrderEvent
case class OrderConfirmation(ids: List[UUID]) extends OrderEvent
case object CancelOrder extends OrderEvent

case class StoreServiceRequest(reply: ActorRef, ids: List[UUID]) extends OrderEvent
case class StoreServiceResponse(success: Boolean) extends OrderEvent
