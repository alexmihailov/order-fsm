package com.witcher
package integration.notify

import java.util.UUID

sealed trait NotifyServiceEvent

/**
 * Оповещение администратора системы.
 *
 * @param orderId идентификатор заказа
 */
case class ManagerNotify(orderId: UUID) extends NotifyServiceEvent

/**
 * Оповещение пользователя о прибытии заказа в пункт выдачи.
 *
 * @param orderId идентификатор заказа
 */
case class UserNotify(orderId: UUID) extends NotifyServiceEvent
