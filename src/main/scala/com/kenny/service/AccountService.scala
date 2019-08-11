package com.kenny.service


import com.kenny.command.Commands
import com.kenny.event._
import com.kenny.service.common._
import org.joda.time.DateTime
import scalaz._
import Scalaz._

import com.kenny.interpreter.RepositoryBackedAccountInterpreter._

/**
 * event service impl
 */
case class Opened(no: String, name: String, openingDate: Option[DateTime], at: DateTime = today) extends Event[Account]

case class Closed(no: String, closeDate: Option[DateTime], at: DateTime = today) extends Event[Account]

case class Debited(no: String, amount: Amount, at: DateTime = today) extends Event[Account]

case class Credited(no: String, amount: Amount, at: DateTime = today) extends Event[Account]


/**
 * snapshot service impl
 */
object AccountSnapshot extends Snapshot[Account] {

  def updateState(e: Event[_], initial: Map[String, Account]) = e match {
    case o@Opened(no, name, odate, _) =>
      initial + (no -> Account(no, name, odate.get))

    case c@Closed(no, cdate, _) =>
      initial + (no -> initial(no).copy(dateOfClosing = Some(cdate.getOrElse(today))))

    case d@Debited(no, amount, _) =>
      val a = initial(no)
      initial + (no -> a.copy(balance = Balance(a.balance.amount - amount)))

    case r@Credited(no, amount, _) =>
      val a = initial(no)
      initial + (no -> a.copy(balance = Balance(a.balance.amount + amount)))
  }
}

/**
 * commands service impl
 */
trait AccountCommands extends Commands[Account] {

  import scala.language.implicitConversions

  private implicit def liftEvent[A](event: Event[A]): Command[A] = Free.liftF(event)

  def open(no: String, name: String, openingDate: Option[DateTime]): Command[Account] =
    Opened(no, name, openingDate, today)

  def close(no: String, closeDate: Option[DateTime]): Command[Account] =
    Closed(no, closeDate, today)

  def debit(no: String, amount: Amount): Command[Account] =
    Debited(no, amount, today)

  def credit(no: String, amount: Amount): Command[Account] =
    Credited(no, amount, today)
}


/**
 * invoke stub
 */
object Scripts extends AccountCommands {

  val composite =
    for {
      a <- open("a-123", "kenny-123", Some(today))
      _ <- credit(a.no, 10000)
      _ <- credit(a.no, 30000)
      d <- debit(a.no, 23000)
    } yield d
  val compositeFail =
    for {
      a <- open("a-124", "kenny-124", Some(today))
      _ <- credit(a.no, 10000)
      _ <- credit(a.no, 30000)
      d <- debit(a.no, 50000)
    } yield d

  def transfer(from: String, to: String, amount: Amount): Command[Unit] = for {
    _ <- debit(from, amount)
    _ <- credit(to, amount)
  } yield ()

  def balance(no: String): Option[Account] = {
    val events = eventLog.events(no)
    val r = events.map(AccountSnapshot.snapshot).getOrElse(null).getOrElse(null).get(no)
    r
  }


}



