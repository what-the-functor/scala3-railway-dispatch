package tonymerlin
package data

import cats.data.EitherT
import cats.syntax.all.*
import org.scalacheck.*
import tonymerlin.compat.scalacheck.cats.given
import tonymerlin.data.*
import tonymerlin.data.Valid
import tonymerlin.errors.RailwayError

trait Fixtures {
  val lineGen: Gen[Valid[Line]] =
    EitherT(Gen.choose(2, 32).map(IntMinimumTwo.from)).map(Line.to).value

  def lineGenM(n: Int): Gen[Valid[List[Line]]] =
    Gen
      .containerOfN[List, Either[RailwayError, Line]](n, lineGen)
      .map(_.sequence)

  def railwayGenN(n: Int): Gen[Valid[Railway]] =
    EitherT(lineGenM(n)).map(lines => Railway(lines.head, lines.tail: _*)).value
}
