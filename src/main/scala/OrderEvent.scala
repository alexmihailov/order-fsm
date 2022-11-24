package com.witcher.order

import akka.actor.ActorRef

import java.util.UUID

/**
 * Базовый интерфейс для событий заказа.
 */
sealed trait OrderEvent

/**
 * Подтверждение дальнейшей обработки заказа.
 *
 * @param ids идентификаторы товаров в заказе
 */
case class OrderConfirmation(ids: List[UUID]) extends OrderEvent

/**
 *  Пользователь отменил заказ.
 */
case object CancelOrder extends OrderEvent

/**
 * Запрос в сервис складов для проверки доступности и резервации товаров.
 *
 * @param reply кому прислать ответ
 * @param ids идентификаторы товаров в заказе
 */
case class StoreServiceRequest(reply: ActorRef, ids: List[UUID]) extends OrderEvent

/**
 * Ответ от сервиса складов по резервации товаров.
 *
 * @param success успех резервирования
 */
case class StoreServiceResponse(success: Boolean) extends OrderEvent

/**
 * Отменить резервацию товаров.
 *
 * @param ids идентификаторы товаров в заказе для отмены резервации
 */
case class CancelReservation(ids: List[UUID]) extends OrderEvent

/**
 * Заказ оплачен пользователем.
 */
case object OrderPayed extends OrderEvent

/**
 * Запрос для размещения заказа в системе доставки.
 */
case object DeliveryServiceRequest extends OrderEvent

/**
 * Ответ системы доставки о размещении заказа.
 *
 * @param success успех размещения в системе доставки
 */
case class DeliveryServiceResponse(success: Boolean) extends OrderEvent

/**
 * Заказ прибыл в пункт выдачи.
 */
case object OrderInPickupPoint extends OrderEvent

/**
 * Оповещение пользователя о прибытии заказа в пункт выдачи.
 *
 * @param orderId идентификатор заказа
 */
case class UserNotify(orderId: UUID) extends OrderEvent

/**
 * Пользователь получил заказ.
 */
case object OrderReceived extends OrderEvent

/**
 * Остановка автомата.
 */
case object StopOrder extends OrderEvent

/**
 * Оповещение администратора системы.
 *
 * @param orderId идентификатор заказа
 */
case class ManagerNotify(orderId: UUID) extends OrderEvent