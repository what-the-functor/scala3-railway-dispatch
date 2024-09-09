package tonymerlin

import cats.effect._
import org.http4s.implicits._
import org.http4s.ember.server._
import com.comcast.ip4s._

import http.v1
import tonymerlin.data.Railway
import tonymerlin.data.RailwayReport
import tonymerlin.data.Line
import tonymerlin.data.LineOfTwo
import tonymerlin.errors.RailwayError
import tonymerlin.data.Valid

trait RailwayState {
  def status(railway: Railway): RailwayReport =
    StateReport.report(railway)
}

object V1Routes extends v1.Routes[IO]

object Main extends IOApp {

  val initialRailway: Valid[Railway] =
    Line.of(1, 2).map(Railway(_))

  def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(V1Routes.routes(initialRailway).orNotFound)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)

}
