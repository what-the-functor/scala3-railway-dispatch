package tonymerlin
package data

import org.scalacheck.Gen.*
import org.scalacheck.Prop.*
import cats.*
import cats.data.*
import munit.ScalaCheckSuite
import org.scalacheck.*
import tonymerlin.compat.scalacheck.cats.*

class StateReportSuite extends ScalaCheckSuite {
  val lineGen: Gen[Valid[Line]] =
    EitherT(Gen.choose(2, 32).map(IntMinimumTwo.from)).map(Line.to).value

  val railwayGen: Gen[Valid[Railway]] =
    EitherT(lineGen).map(Railway(_)).value

  property("State of railway is reportable") {
    forAll(railwayGen) { (rw: Valid[Railway]) =>
      val expected: Valid[Report[Railway]] =
        Terminal.from(1).map(x => RailwayReport(LineReport(x, Forward)))

      val result: Valid[Report[Railway]] =
        rw.map(StateReport.report(_))

      assertEquals(result, expected)
    }
  }
}
