package com.witcher
package delivery

import akka.actor.ActorRef

import java.util.UUID

sealed trait DeliveryEvent
case object StopDelivery extends DeliveryEvent
