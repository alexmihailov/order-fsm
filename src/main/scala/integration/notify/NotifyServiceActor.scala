package com.witcher
package integration.notify

import akka.actor.Actor
import akka.event.LoggingReceive

/**
 * Актор эмулирующий работу системы нотификаций.
 */
class NotifyServiceActor extends Actor {
  override def receive: Receive = LoggingReceive {
    case ManagerNotify(id) =>
    case UserNotify(id) =>
  }
}
