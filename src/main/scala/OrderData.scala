package com.witcher.order

import java.util.UUID

sealed trait OrderData
case object Uninitialized extends OrderData
case class OrderDetail(ids: List[UUID]) extends OrderData
