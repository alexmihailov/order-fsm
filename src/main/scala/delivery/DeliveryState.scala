package com.witcher
package delivery

sealed trait DeliveryState
object Idle extends DeliveryState
object WaitResponse extends DeliveryState
object WaitDelay extends DeliveryState
object SuccessExit extends DeliveryState
object FailedExit extends DeliveryState