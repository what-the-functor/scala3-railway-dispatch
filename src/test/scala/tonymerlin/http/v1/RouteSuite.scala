package tonymerlin
package http.v1

import cats.*
import cats.effect.*
import cats.effect.std.Queue
import cats.syntax.either.*
import io.circe.*
import io.circe.literal.*
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.HttpApp
import org.http4s.circe.*
import org.http4s.implicits.*

import tonymerlin.data.*

object DummyAlgebra extends Algebra[IO] {
  val status = StateReport.report

  def enqueue(req: PickupReq, pickups: Queue[IO, Pickup]): IO[Valid[Pickup]] = {
    val PickupReq(a, b) = req
    IO.pure(Pickup.from(a, b))
  }
}

object fixture extends Routes[IO]

class RouteSuite extends CatsEffectSuite {
  val railwayF: IO[Railway] =
    IO.fromEither(Line.of(1, 2).map(Railway(_)).leftMap(_ => new Throwable))

  val emptyQueue: IO[Queue[IO, Pickup]] = Queue.unbounded[IO, Pickup]

  val service: IO[HttpApp[IO]] =
    railwayF.flatMap { railway =>
      emptyQueue.map(fixture.routes(DummyAlgebra, railway, _).orNotFound)
    }

  test("status")({
    val response: IO[Response[IO]] =
      service.flatMap(_.run(Request(Method.GET, uri"/status")))

    assertIO(response.map(_.status), Status.Ok)
  })

  test("pickup") {
    val request: Request[IO] =
      Request(Method.POST, uri"/pickup").withEntity(json"""{"a": "1", "b": "2"}""")
    val response: IO[Response[IO]] =
      service.flatMap(_.run(request))

    assertIO(response.map(_.status), Status.Accepted)
  }
}
