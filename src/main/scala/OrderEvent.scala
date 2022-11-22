package com.witcher.order

sealed trait OrderEvent
case object OrderConfirmation extends OrderEvent
