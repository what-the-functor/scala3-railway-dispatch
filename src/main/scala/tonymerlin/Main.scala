package tonymerlin

import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
import org.http4s.ember.server.*
import org.http4s.implicits.*
import tonymerlin.data.*
import tonymerlin.http.v1.Routes

import http.v1
import cats.effect.std.Dequeue

object V1Routes extends Routes[IO]

object Main extends IOApp {

  val railwayOrError: Valid[Railway] =
    for
      first <- Line.max
      rest  <- (2 to 32).toList.traverse(_ => Line.max)
    yield Railway(first, rest: _*)

  def run(args: List[String]): IO[ExitCode] =
    for
      railwayRef <- Ref.ofEffect(
        IO.fromEither(railwayOrError.leftMap(e => new Throwable(s"Invalid railway: ${e.msg}")))
      )

      queue <- Dequeue.unbounded[IO, Pickup]
      server <-
        EmberServerBuilder
          .default[IO]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(V1Routes.routes(Algebra[IO], railwayRef, queue).orNotFound)
          .build
          .use(_ => IO.never)
          .as(ExitCode.Success)
    yield server
}
