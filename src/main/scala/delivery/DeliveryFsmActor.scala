package com.witcher
package delivery

import integration.delivery.{DeliveryRequest, DeliveryResponse}
import order.{DeliveryServiceRequest, DeliveryServiceResponse}

import akka.actor.{ActorRef, LoggingFSM}

import java.util.UUID
import scala.concurrent.duration.FiniteDuration

/**
 * Актор для отправки сообщений в службу доставки.
 * В случае ошибки запрос будет повторяться, пока не кончится количество попыток.
 */
class DeliveryFsmActor(
   private val config: DeliveryFsmActor.Config,
   private val deliveryServiceActor: ActorRef,
   private val orderFsmActor: ActorRef,
   private val orderId: UUID
) extends LoggingFSM[DeliveryState, Int] {

  startWith(Idle, 0)

  when(Idle) {
    case Event(DeliveryServiceRequest, retryCount) => goto(WaitResponse).using(retryCount + 1)
  }
  when(WaitResponse, stateTimeout = config.requestTimeout) {
    case Event(DeliveryResponse(success), retryCount) => if (success) {
      goto(SuccessExit)
    } else if (retryCount < config.maxRetryCount) {
      goto(WaitDelay)
    } else {
      goto(FailedExit)
    }
    case Event(StateTimeout, retryCount) => if (retryCount < config.maxRetryCount) {
      goto(WaitResponse).using(retryCount + 1)
    } else {
      goto(FailedExit)
    }
  }
  when(WaitDelay, stateTimeout = config.requestsDelay) {
    case Event(StateTimeout, retryCount) => goto(WaitResponse).using(retryCount + 1)
  }
  when(SuccessExit) {
    case Event(_, _) => stop()
  }
  when(FailedExit) {
    case Event(_, _) => stop()
  }

  onTransition {
    case _ -> WaitResponse => deliveryServiceActor ! DeliveryRequest(self, orderId)
    case WaitResponse -> SuccessExit =>
      orderFsmActor ! DeliveryServiceResponse(true)
      self ! StopDelivery

    case WaitResponse -> FailedExit =>
      orderFsmActor ! DeliveryServiceResponse(false)
      self ! StopDelivery
  }
}

object DeliveryFsmActor {
  case class Config(maxRetryCount: Int, requestsDelay: FiniteDuration, requestTimeout: FiniteDuration)
}
