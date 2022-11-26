package com.witcher
package delivery

import akka.actor.ActorRef

import java.util.UUID

sealed trait DeliveryData
case object DeliveryUninitialized extends DeliveryData
case class DeliveryDetail(reply: ActorRef, orderId: UUID, retryCount: Int) extends DeliveryData
