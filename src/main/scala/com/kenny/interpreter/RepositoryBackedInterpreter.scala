package com.kenny.interpreter


import com.kenny.event._
import com.kenny.service._
import com.kenny.service.common._
import org.joda.time.DateTime
import scalaz._
import Scalaz._

import scalaz.\/._
import scalaz.concurrent.Task


trait RepositoryBackedInterpreter {

  def step: Event ~> Task

  def apply[A](action: Free[Event, A]): Task[A] = action.foldMap(step)
}


object RepositoryBackedAccountInterpreter extends RepositoryBackedInterpreter {

  import AccountSnapshot._
  import JSONProtocols._

  val eventLog = new InMemoryJSONEventStore {
    val eventJsonFormat = EventFormat
  }.apply[String]

  import eventLog._

  val step: Event ~> Task = new (Event ~> Task) {
    override def apply[A](action: Event[A]): Task[A] = handleCommand(action)
  }

  /**
   * command processor
   *
   * @param e
   * @tparam A
   * @return
   */
  private def handleCommand[A](e: Event[A]): Task[A] = e match {

    case o@Opened(no, name, odate, _) => Task {

      validateOpen(no).fold(
        err => throw new RuntimeException(err),
        _ => {
          val a = Account(no, name, odate.get)
          eventLog.put(no, o)
          a
        }
      )
    }

    case c@Closed(no, cdate, _) => Task {
      validateClose(no, cdate).fold(
        err => throw new RuntimeException(err),
        currentState => {
          eventLog.put(no, c)
          updateState(c, currentState)(no)
        }
      )
    }

    case d@Debited(no, amount, _) => Task {
      validateDebit(no, amount).fold(
        err => throw new RuntimeException(err),
        currentState => {
          eventLog.put(no, d)
          updateState(d, currentState)(no)
        }
      )
    }

    case r@Credited(no, amount, _) => Task {
      validateCredit(no).fold(
        err => throw new RuntimeException(err),
        currentState => {
          eventLog.put(no, r)
          updateState(r, currentState)(no)
        }
      )
    }
  }

  /**
   * define 3 exceptions
   */

  private def closed(a: Account): Error \/ Account =
    if (a.dateOfClosing isDefined) s"Account ${a.no} is closed".left
    else a.right

  private def beforeOpeningDate(a: Account, cd: Option[DateTime]): Error \/ Account =
    if (a.dateOfOpening isBefore cd.getOrElse(today))
      s"Cannot close at a date earlier than opening date ${a.dateOfOpening}".left
    else a.right

  private def sufficientFundsToDebit(a: Account, amount: Amount): Error \/ Account =
    if (a.balance.amount < amount) s"insufficient fund to debit $amount from ${a.no}".left
    else a.right


  /**
   * validate for 4 operations
   */

  private def validateOpen(no: String) = {
    val events = eventLog.get(no)
    if (events nonEmpty) s"Account with no = $no already exists".left
    else no.right
  }

  private def validateClose(no: String, cd: Option[DateTime]) = for {
    l <- events(no)
    s <- snapshot(l)
    a <- closed(s(no))
    _ <- beforeOpeningDate(a, cd)
  } yield s

  private def validateDebit(no: String, amount: Amount) = for {
    l <- events(no)
    s <- snapshot(l)
    a <- closed(s(no))
    _ <- sufficientFundsToDebit(a, amount)
  } yield s

  private def validateCredit(no: String) = for {
    l <- events(no)
    s <- snapshot(l)
    _ <- closed(s(no))
  } yield s


}
