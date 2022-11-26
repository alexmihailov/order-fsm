package com.witcher
package integration.store

import akka.actor.ActorRef

import java.util.UUID

sealed trait StoreServiceEvent
/**
 * Запрос в сервис складов для проверки доступности и резервации товаров.
 *
 * @param reply кому прислать ответ
 * @param ids идентификаторы товаров в заказе
 */
case class StoreServiceRequest(reply: ActorRef, ids: List[UUID]) extends StoreServiceEvent

/**
 * Ответ от сервиса складов по резервации товаров.
 *
 * @param success успех резервирования
 */
case class StoreServiceResponse(success: Boolean) extends StoreServiceEvent
