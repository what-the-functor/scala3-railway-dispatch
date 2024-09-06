package tonymerlin
package data

import cats.*
import cats.syntax.all.*
import tonymerlin.errors.IntLessThanTwo
import tonymerlin.errors.IntNotPositive

import scala.collection.immutable.ArraySeq

/** A positive [Int] */
final case class IntPositive private (val value: Int) extends AnyVal

object IntPositive {

  /** Create a positive int from an [Int] or else [scala.util.Left] of [tonymerlin.errors.IntNotPositive]
    */
  def from(value: Int): Valid[IntPositive] =
    Right(new IntPositive(value))
      .filterOrElse(_.value > 0, IntNotPositive)

  /** [IntMinimumTwo] is known to be positive, so we can create a positive int from it
    */
  def from(knownPositive: IntMinimumTwo): IntPositive =
    IntPositive(knownPositive.value)

  def inRangeTo(knownPositive: IntMinimumTwo): IndexedSeq[IntPositive] =
    (1 to knownPositive.value).map(IntPositive(_))

  val one: IntPositive = IntPositive(1)
}

/** An [Int] which has a minimum value of 2 */
final case class IntMinimumTwo private (val value: Int) extends AnyVal

object IntMinimumTwo {

  /** Create a [scala.util.Right] of [IntMinimumTwo] or else [scala.util.Left] of [IntLessThanTwo]
    */
  def from(value: Int): Valid[IntMinimumTwo] =
    Right(new IntMinimumTwo(value))
      .filterOrElse(_.value >= 2, IntLessThanTwo)
}

/** Indicates the direction that the [Train] will go on the next step
  */
sealed trait Direction
case object Forward  extends Direction
case object Backward extends Direction

/** A terminal
  */
final case class Terminal(name: IntPositive)
object Terminal {
  def from(name: Int): Valid[Terminal] =
    IntPositive.from(name).map(Terminal(_))
}

/** A train */
final case class Train(location: Terminal, direction: Direction)

/** A railway line.  There is a minimum of two terminals */
sealed trait Line {
  def train: Train
  def first: Terminal
  def last: Terminal
}

/** The simplest railway line, which consists of two terminals */
final case class LineOfTwo(train: Train, first: Terminal, last: Terminal) extends Line

/** A railway line with more than two terminals */
final case class LineOfN(
    train: Train,
    first: Terminal,
    last: Terminal,
    path: ArraySeq[Terminal]
) extends Line

object Line {
  def apply(first: Terminal, last: Terminal): Line =
    LineOfTwo(Train(first, Forward), first, last)

  def of(first: Int, last: Int): Valid[Line] =
    for
      n1 <- IntPositive.from(first)
      n2 <- IntPositive.from(last)
    yield Line(Terminal(n1), Terminal(n2))

  def apply(first: Terminal, second: Terminal, rest: Terminal*): Line = {
    val path = ArraySeq.from(first :: second :: rest.toList)

    LineOfN(Train(first, Forward), first, path.last, path)
  }

  /** Create a [scala.util.Right] of [Line] or else [scala.util.Left] of [IntNotPositive]
    */
  def to(last: IntMinimumTwo): Line =
    last.value match {
      case 2 =>
        Line(Terminal(IntPositive.one), Terminal(IntPositive.from(last)))
      case n => {
        val path = ArraySeq.from(IntPositive.inRangeTo(last).map(Terminal(_)))
        LineOfN(Train(path.head, Forward), path.head, path.last, path)
      }
    }

  implicit val lineReportable: Reportable[Line, LineReport] =
    Reportable.of[Line, LineReport](line => LineReport(line.train.location, line.train.direction))
}

case class Railway private (first: Line, all: ArraySeq[Line])
object Railway {
  def apply(line: Line): Railway =
    Railway(line, ArraySeq(line))

  def apply(first: Line, rest: Line*): Railway =
    Railway(first, ArraySeq.from(first :: rest.toList))

  implicit def railwayReportable(implicit
      lineReportable: Reportable[Line, LineReport]
  ): Reportable[Railway, RailwayReport] =
    Reportable.of { case Railway(first, all) =>
      RailwayReport(
        lineReportable.report(first),
        all.tail.map(lineReportable.report): _*
      )
    }
}

trait Report[+A]
case class LineReport(trainLocation: Terminal, trainDirection: Direction) extends Report[Line]
case class RailwayReport(first: LineReport, rest: LineReport*)            extends Report[Railway]

trait Reportable[A, B <: Report[A]] {
  def report(value: A): B
}
object Reportable {
  def apply[A, B <: Report[A]](implicit r: Reportable[A, B]): Reportable[A, B] =
    r

  def of[A, B <: Report[A]](f: A => B): Reportable[A, B] =
    new Reportable[A, B] {
      def report(value: A): B = f(value)
    }
}
