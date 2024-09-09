package tonymerlin
package http
package v1

import org.http4s._
import org.http4s.circe._
import cats.effect._
// import org.http4s.implicits._
import cats.effect.kernel.Async
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe._
import io.circe.literal._
import io.circe.generic.auto._
import io.circe.syntax._
import data._
import data.Railway.given
import tonymerlin.errors.RailwayError
import tonymerlin.StateReport.report
import tonymerlin.data.Backward
import tonymerlin.data.LineReport

trait Routes[F[_]: Async] extends Codec {
  val dsl = Http4sDsl[F]; import dsl._

  def routes(railway: Valid[Railway]): HttpRoutes[F] =
    HttpRoutes
      .of[F] { case GET -> Root / "status" =>
        railway.map(StateReport.report) match
          case Right(report) => Ok(report.asJson)
          case Left(error)   => InternalServerError(error.asJson)
      }
}

trait Codec {
  // import io.circe.generic.semiauto._

  implicit val intPositiveEncoder: Encoder[IntPositive] =
    Encoder[Int].contramap(_.value)

  implicit val directionEncoder: Encoder[Direction] =
    Encoder[String].contramap {
      case Forward  => "forward"
      case Backward => "backward"
    }

}
