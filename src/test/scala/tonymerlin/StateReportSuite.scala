package tonymerlin
package data

import errors.*
import org.scalacheck.Gen.*
import org.scalacheck.Prop.*
import cats.*
import cats.data.*
import cats.syntax.all.*
import cats.instances.all.*
import munit.ScalaCheckSuite
import org.scalacheck.*
import tonymerlin.compat.scalacheck.cats.given

class StateReportSuite extends ScalaCheckSuite {
  val lineGen: Gen[Valid[Line]] =
    EitherT(Gen.choose(2, 32).map(IntMinimumTwo.from)).map(Line.to).value

  def lineGenM(n: Int): Gen[Valid[List[Line]]] =
    Gen
      .containerOfN[List, Either[RailwayError, Line]](n, lineGen)
      .map(_.sequence)

  def railwayGenN(n: Int): Gen[Valid[Railway]] =
    EitherT(lineGenM(n)).map(lines => Railway(lines.head, lines.tail: _*)).value

  property("Line state is reportable") {
    forAll(lineGen) { (line: Valid[Line]) =>
      val result: Valid[LineReport] = line.map(StateReport.report(_))

      ("result = " + result) |: Prop.all(
        (result.map(_.trainLocation) == line.map(_.train.location)) :| "Incorrect train location",
        (result.map(_.trainDirection) == line.map(_.train.direction)) :| "Incorrect train direction"
      )
    }
  }

  property("State of railway is reportable") {
    forAll(railwayGenN(32)) { (rw: Valid[Railway]) =>
      val result: Valid[RailwayReport] =
        rw.map(StateReport.report(_))

      val lineReportExpectations: Valid[(List[LineReport], List[LineReport])] =
        for
          railway <- rw
          report  <- result
          expected = railway.all.map(StateReport.report(_)).toList
        yield (report.first :: report.rest.toList, expected)

      (lineReportExpectations.forall { case (result, expected) => result == expected }) :| "Incorrect line reports"
    }
  }
}
