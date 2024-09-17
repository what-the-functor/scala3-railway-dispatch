package tonymerlin
package data

import errors.IntLessThanTwo
import errors.IntNotPositive
import cats.*
import cats.data.NonEmptyList
import cats.data.NonEmptyMap
import cats.effect.*
import cats.effect.kernel.Sync
import cats.effect.std.Dequeue
import cats.instances.all.*
import cats.kernel.Order
import cats.syntax.all.*

import scala.collection.SortedMap
import scala.collection.immutable.ArraySeq

/** A positive [Int] */
final case class IntPositive private (val value: Int) extends AnyVal

object IntPositive {

  /** Create a positive int from an [Int] or else [scala.util.Left] of [tonymerlin.errors.IntNotPositive] */
  def from(value: Int): Valid[IntPositive] =
    Right(new IntPositive(value))
      .filterOrElse(_.value > 0, IntNotPositive)

  /** [IntMinimumTwo] is known to be positive, it can be used to create [IntPositive] */
  def from(knownPositive: IntMinimumTwo): IntPositive =
    IntPositive(knownPositive.value)

  /** Create [IntPositive]s from 1 to end (inclusive) */
  def inRangeTo(knownPositive: IntMinimumTwo): IndexedSeq[IntPositive] =
    (1 to knownPositive.value).map(IntPositive(_))

  /** [IntPositive] of 1 */
  val one: IntPositive = IntPositive(1)

  /** The previous [IntPositive] if it is positive, otherwise [scala.None] */
  def prev(x: IntPositive): Option[IntPositive] =
    Option(x).filter(_.value > 1).map(_.value - 1).flatMap(IntPositive.from(_).toOption)

  /** The next [IntPositive] if it is less than, or equal to the maximum value of [scala.Int], otherwise [scala.None] */
  def next(x: IntPositive): Option[IntPositive] =
    Option(x).filter(_.value < Int.MaxValue).map(_.value + 1).flatMap(IntPositive.from(_).toOption)

  /** Ordering instance for [IntPositive] */
  given Ordering[IntPositive] =
    new Ordering[IntPositive] {
      def compare(x: IntPositive, y: IntPositive): Int = x.value.compare(y.value)
    }

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

  given Ordering[Terminal] =
    new Ordering[Terminal] {
      def compare(x: Terminal, y: Terminal): Int = x.name.value.compare(y.name.value)
    }

  given Order[Terminal] =
    Order.fromOrdering[Terminal]
}

/** A train. Holds the state of the train's current terminal and direction of travel
  */
sealed trait Train {

  /** The train's current location */
  def location: Terminal

  /** The direction of travel */
  def direction: Direction

  /** Predicates whether or not the train is non-empty (carrying passengers) */
  def nonEmpty: Boolean

  /** Predicates whether or not the train is empty (inverse of nonEmpty) */
  lazy val isEmpty: Boolean = !nonEmpty

  /** Embark the pickup request */
  def embark(pickup: Pickup): NonEmptyTrain

  /** Dissembark any pickup requests which are at their destination */
  def dissembark: Train

  /** Move the train to the specified terminal, and set it's direction */
  def move(location: Terminal, direction: Direction): Train

  /** Move the train to the next terminal, based on the current direction */
  def move(line: Line): Option[Train] =
    direction match
      case Forward =>
        IntPositive.next(location.name).map(Terminal(_)).map {
          case t if t == line.last => move(t, Backward)
          case t                   => move(t, Forward)
        }
      case Backward =>
        IntPositive.prev(location.name).map(Terminal(_)).map {
          case t if t == line.first => move(t, Forward)
          case t                    => move(t, Backward)
        }
}

/** An empty train, not occupied by any passengers */
final case class EmptyTrain(location: Terminal, direction: Direction) extends Train {
  val nonEmpty: Boolean = false

  def embark(pickup: Pickup): NonEmptyTrain =
    NonEmptyTrain(location, direction, NonEmptyMap.one(pickup.b, NonEmptyList.one(pickup)))

  val dissembark: Train = this

  def move(newLocation: Terminal, newDirection: Direction): Train =
    this.copy(location = newLocation, direction = newDirection)

}

/** A non-empty train, which is carrying passengers */
final case class NonEmptyTrain(
    location: Terminal,
    direction: Direction,
    pickups: NonEmptyMap[Terminal, NonEmptyList[Pickup]]
) extends Train {
  val nonEmpty: Boolean = true

  def embark(pickup: Pickup): NonEmptyTrain =
    this.copy(pickups =
      this.pickups
        .lookup(pickup.b)
        .map(_ => pickups.updateWith(pickup.b)(pickup :: _))
        .getOrElse(pickups.add(pickup.b, NonEmptyList.one(pickup)))
    )

  def dissembark: Train =
    NonEmptyMap
      .fromMap(pickups - location)
      .map(updatedPickups => this.copy(pickups = updatedPickups))
      .getOrElse(EmptyTrain(location, direction))

  def move(newLocation: Terminal, newDirection: Direction): Train =
    this.copy(location = newLocation, direction = newDirection)

}

object Train {
  def apply(location: Terminal, direction: Direction): Train = EmptyTrain(location, direction)
}

/** A railway line.  There is a minimum of two terminals */
sealed trait Line {

  /** The train, each line has exactly one */
  def train: Train

  /** Direct access to the first terminal */
  def first: Terminal

  /** Direct access to the last terminal */
  def last: Terminal

  /** Embark the pickup on the line's train */
  def embark(pickup: Pickup): Line

  /** Dissembark any pick-up requests with destination matching the current terminal */
  def dissembark: Line

  /** Move the line's train by one terminal */
  def move: Option[Line]
}

/** The simplest railway line, which consists of two terminals */
final case class LineOfTwo(train: Train, first: Terminal, last: Terminal) extends Line {
  def embark(pickup: Pickup): Line = this.copy(train = this.train.embark(pickup))

  def dissembark: Line = this.copy(train = this.train.dissembark)

  def move: Option[Line] =
    train.move(this).map(moved => this.copy(train = moved))
}

/** A railway line with more than two terminals */
final case class LineOfN(
    train: Train,
    first: Terminal,
    last: Terminal,
    path: ArraySeq[Terminal]
) extends Line {
  def embark(pickup: Pickup): Line = this.copy(train = this.train.embark(pickup))

  def dissembark: Line = this.copy(train = this.train.dissembark)

  def move: Option[Line] =
    train.move(this).map(moved => this.copy(train = moved))

}

object Line {

  /** Create a new railway line with two terminals */
  def apply(first: Terminal, last: Terminal): Line =
    LineOfTwo(EmptyTrain(first, Forward), first, last)

  /** Create a new railway line with two terminals from the given terminal names */
  def of(first: Int, last: Int): Valid[Line] =
    for
      n1 <- IntPositive.from(first)
      n2 <- IntPositive.from(last)
    yield Line(Terminal(n1), Terminal(n2))

    /** Create a new railway line with more than two terminals */
  def apply(first: Terminal, second: Terminal, rest: Terminal*): Line = {
    val path = ArraySeq.from(first :: second :: rest.toList)

    LineOfN(EmptyTrain(first, Forward), first, path.last, path)
  }

  /** Create a [scala.util.Right] of [Line] or else [scala.util.Left] of [IntNotPositive]
    */
  def to(last: IntMinimumTwo): Line =
    last.value match {
      case 2 =>
        Line(Terminal(IntPositive.one), Terminal(IntPositive.from(last)))
      case n => {
        val path = ArraySeq.from(IntPositive.inRangeTo(last).map(Terminal(_)))
        LineOfN(EmptyTrain(path.head, Forward), path.head, path.last, path)
      }
    }

  /** Create a line of the maximum supported length */
  val max: Valid[Line] =
    IntMinimumTwo.from(32).map(to)

  /** Reportable instance for [LIne] */
  given Reportable[Line, LineReport] =
    Reportable.of[Line, LineReport] { line =>
      line.train match {
        case EmptyTrain(location, direction) => LineReport(location, direction, List())
        case NonEmptyTrain(location, direction, pickups) =>
          LineReport(location, direction, pickups.toList.flatMap(_.toList))
      }
    }
}

/** A railway, which consists of multiple [Lines]
  *
  * There is a minimum of one [Line]
  */
case class Railway private (first: Line, all: ArraySeq[Line]) {
  val roundTripDistance: Int = all.size * 2 - 2

  /** Handle the pick-up request, to embark the passenger on the train by index, optionally returning the updated
    * [Railway]
    *
    * Returns None, if the line index is out-of-bounds
    */
  def embarkByIndex(pickup: Pickup, index: Int): Option[Railway] =
    index
      .pure[Option]
      .filter(_ >= 0)
      .filter(_ < all.size)
      .map(i => all.updated(i, all(i).embark(pickup)))
      .map(lines => this.copy(first = lines(0), all = lines))

  /** Find indices of line to embark (first element), and line to move (second element) */
  def indicesFor(pickup: Pickup): (Option[Int], Option[Int]) = {

    val byDistance: SortedMap[Int, (Line, Int)] =
      SortedMap.from(
        all
          .to(LazyList)
          .zipWithIndex
          .map {
            case (line, i) if line.train.direction == pickup.direction =>
              val distance = pickup.a.name.value - line.train.location.name.value
              (if distance >= 0 then distance else roundTripDistance + distance, (line, i))

            case (line, i) if line.train.direction == Backward =>
              val distanceToFirst   = line.train.location.name.value - 1
              val distanceFromFirst = pickup.a.name.value - 1
              (distanceToFirst + distanceFromFirst, (line, i))

            case (line, i) =>
              val distanceToLast   = line.last.name.value - line.train.location.name.value
              val distanceFromLast = line.last.name.value - pickup.a.name.value
              (distanceToLast + distanceFromLast, (line, i))
          }
          .reverse
      )

    val immediatelyAvailable: Option[Int] =
      byDistance.get(0).map { case (_, i) => i }

    lazy val closest: Option[Int] = byDistance.headOption.map { case (_, (_, i)) => i }

    (immediatelyAvailable, immediatelyAvailable.orElse(closest))
  }

  /** Dissembark all pickups which are at their destination */
  def dissembark: Railway =
    this.copy(all = all.map(_.dissembark))

  /** Move trains by one terminal */
  def moveByIndices(indices: Int*): Railway = {
    val maybeMovedLines: List[Option[(Int, Line)]] = indices.toList.map(i => (i, all(i).move).sequence)

    val allUpdated: ArraySeq[Line] =
      maybeMovedLines.foldLeft(all) {
        case (lines, Some(i, line)) => lines.updated(i, line)
        case (lines, None)          => lines
      }

    this.copy(allUpdated(0), allUpdated)
  }

  /** Step the state of the railway by one step:
    *
    *   - All pickups at their destination are disembarked,
    *   - all pickups with immediately available trains are embarked,
    *   - all occupied trains are moved by one terminal,
    *   - and the closest trains to any waiting pickups are moved by one terminal
    */
  def step[F[_]: Sync](pickups: Dequeue[F, Pickup]): F[Railway] =
    for
      dissembarked <- Sync[F].pure(this.dissembark)
      occupiedIndices <- Sync[F].pure(dissembarked.all.to(LazyList).zipWithIndex.collect {
        case (l, i) if l.train.nonEmpty => i
      })
      pickupBatch <- pickups.tryTakeN(None)
      result <-
        pickupBatch.foldLeft(Sync[F].pure((dissembarked, Set[Int]()))) { case (railwayAndMoveIndicesF, pickup) =>
          val (maybeEmbarkIndex, toMoveIndex) = indicesFor(pickup)

          railwayAndMoveIndicesF
            .flatMap { case (railway, moveIndices) =>
              maybeEmbarkIndex
                .flatMap(railway.embarkByIndex.curried(pickup))
                .map(Sync[F].pure)
                .getOrElse(pickups.offer(pickup).map(_ => railway))
                .map((_, toMoveIndex.toSet ++ moveIndices))
            }
        }
      (embarkedOrWaiting, moveIndices) = result
    yield embarkedOrWaiting.moveByIndices((occupiedIndices ++ moveIndices)*)

}

object Railway {

  /** Create a railway with one line */
  def apply(line: Line): Railway =
    Railway(line, ArraySeq(line))

    /** Create a railway */
  def apply(first: Line, rest: Line*): Railway =
    Railway(first, ArraySeq.from(first :: rest.toList))

    /** Reportable instance, enables reporting the state of the [RailWay] */
  given railwayReportable(using reportable: Reportable[Line, LineReport]): Reportable[Railway, RailwayReport] =
    Reportable.of { case Railway(first, all) =>
      RailwayReport(reportable.report(first), all.tail.map(reportable.report): _*)
    }
}

/** Report the status */
trait Report[+A]
case class LineReport(trainLocation: Terminal, trainDirection: Direction, trainPickups: List[Pickup])
    extends Report[Line]
case class RailwayReport(first: LineReport, rest: LineReport*) extends Report[Railway]

/** Type class to enable reporting */
trait Reportable[A, B <: Report[A]]:
  extension (a: A) def report: B

object Reportable {

  /** Create a [Reportable] instance */
  def of[A, B <: Report[A]](f: A => B): Reportable[A, B] =
    new Reportable[A, B] {
      extension (value: A) def report: B = f(value)
    }
}

/** A pick-up request from the source terminal (by name) to the destination terminal (by name) */
case class PickupReq(a: Int, b: Int)

/** A pick-up from the source terminal, to the destination terminal */
case class Pickup(a: Terminal, b: Terminal) {

  /** The direction of the pickup */
  def direction: Direction = if a < b then Forward else Backward

}

object Pickup {

  /** Create a [Pickup] */
  def from(a: Int, b: Int) =
    for
      ta <- Terminal.from(a)
      tb <- Terminal.from(b)
    yield Pickup(ta, tb)

}
