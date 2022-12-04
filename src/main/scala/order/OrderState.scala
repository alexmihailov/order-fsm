package com.witcher
package order

/**
 * Базовый интерфейс для отображающий состояние заказа.
 */
sealed trait OrderState

/**
 * Создан новый заказ.
 */
case object New extends OrderState

/**
 * Заказ принят в обработку.
 */
case object Ordered extends OrderState

/**
 * Товар в заказе зарезервирован.
 */
case object Reserved extends OrderState

/**
 * Заказ оплачен.
 */
case object Payed extends OrderState

/**
 * Заказ отправлен в пункт выдачи.
 */
case object Shipped extends OrderState

/**
 * Заказ прибыл в пункт выдачи.
 */
case object Delivered extends OrderState

/**
 * Заказ получен.
 */
case object Completed extends OrderState

/**
 * Отмена заказа.
 */
case object Canceled extends OrderState

