package com.witcher
package integration.delivery

import akka.actor.ActorRef

import java.util.UUID

/**
 * Базовый интерфейс для событий службы доставки.
 */
sealed trait DeliveryServiceEvent

/**
 * Запрос на доставку заказа.
 *
 * @param reply кому ответить
 * @param orderId идентификатор заказа
 */
case class DeliveryRequest(reply: ActorRef, orderId: UUID) extends DeliveryServiceEvent

/**
 * Ответ об успехе размещения заказа в службе доставки.
 *
 * @param success true - если заказ успешно размещен.
 */
case class DeliveryResponse(success: Boolean) extends DeliveryServiceEvent

