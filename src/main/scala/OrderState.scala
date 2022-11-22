package com.witcher.order

sealed trait OrderState
case object New extends OrderState
case object Ordered extends OrderState
case object Reserved extends OrderState
case object Payed extends OrderState
case object Shipped extends OrderState
case object Delivered extends OrderState
case object Completed extends OrderState
case object Canceled extends OrderState

