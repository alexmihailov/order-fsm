package com.witcher
package delivery

/**
 * Базовый интерфейс для событий доставки.
 */
sealed trait DeliveryEvent

/**
 * Остановить актор доставки.
 */
case object StopDelivery extends DeliveryEvent
