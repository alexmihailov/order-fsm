package com.witcher
package integration.notify

import akka.actor.Actor

class NotifyServiceActor extends Actor {
  override def receive: Receive = {
    case ManagerNotify(id) =>
    case UserNotify(id) =>
  }
}
