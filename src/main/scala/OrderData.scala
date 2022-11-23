package com.witcher.order

import java.util.UUID

/**
 * Базовый интерфейс для данных о товаре.
 */
sealed trait OrderData

/**
 * Начальные данные заказа.
 *
 * @param id идентификатор заказа.
 */
case class Uninitialized(id: UUID) extends OrderData

/**
 * Данные заказа.
 *
 * @param id идентификатор заказа.
 * @param itemIds идентификаторы товаров в заказе.
 */
case class OrderDetail(id: UUID, itemIds: List[UUID]) extends OrderData
