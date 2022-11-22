package com.witcher.order

sealed trait OrderData
case object Uninitialized extends OrderData
case object OrderDetail extends OrderData