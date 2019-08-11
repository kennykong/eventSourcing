package com.kenny.service

import scalaz._
import Scalaz._
import com.kenny.event._
import org.joda.time.DateTime

object common {
  type Amount = BigDecimal
  type Error = String

  val today = DateTime.now()
}

import common._

case class Balance(amount: Amount = 0)

case class Account(no: String, name: String, dateOfOpening: DateTime = today, dateOfClosing: Option[DateTime] = None,
                   balance: Balance = Balance(0)) extends Aggregate {
  def id = no
  def isClosed = dateOfClosing.isDefined
}


