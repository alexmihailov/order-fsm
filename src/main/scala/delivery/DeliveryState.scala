package com.witcher
package delivery

/**
 * Базовый интерфейс состояний актора доставки.
 */
sealed trait DeliveryState

/**
 * Ожидания запрос на отправку данных в службу доставки.
 */
object Idle extends DeliveryState

/**
 * Ожидание ответа от службы доставки.
 */
object WaitResponse extends DeliveryState

/**
 * Ожидание промежутка времени для повторного запроса.
 */
object WaitDelay extends DeliveryState

/**
 * Успешно получили ответ от службы доставки.
 */
object SuccessExit extends DeliveryState

/**
 * Получили ошибку от службы доставки или не смогли получить ответ.
 */
object FailedExit extends DeliveryState
