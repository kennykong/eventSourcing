package com.kenny.event

import org.joda.time.DateTime

import scalaz._
import Scalaz._

object Common {
  type AggregateId = String
  type Error = String
}

import com.kenny.event.Common._

/**
 * The `Event` abstraction. `A` points to the next event in chain
 */
trait Event[A] {
  def at: DateTime
}

/**
 * All aggregates need to have an id
 */
trait Aggregate {
  def id: AggregateId
}

/**
 * Snapshot of some aggregate
 */
trait Snapshot[A <: Aggregate] {
  def updateState(e: Event[_], initial: Map[String, A]): Map[String, A]

  def snapshot(es: List[Event[_]]): String \/ Map[String, A] =
    es.reverse.foldLeft(Map.empty[String, A]) { (a, e) => updateState(e, a) }.right
}




