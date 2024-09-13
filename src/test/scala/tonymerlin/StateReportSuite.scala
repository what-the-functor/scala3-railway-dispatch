package tonymerlin
package data

import munit.ScalaCheckSuite
import org.scalacheck.*

import Prop.*

class StateReportSuite extends ScalaCheckSuite with Fixtures {
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
