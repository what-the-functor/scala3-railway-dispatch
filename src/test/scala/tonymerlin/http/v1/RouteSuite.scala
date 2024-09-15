package tonymerlin
package http.v1

import cats.*
import cats.effect.*
import cats.effect.std.Dequeue
import cats.syntax.either.*
import io.circe.*
import io.circe.literal.*
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.circe.*
import org.http4s.implicits.*
import tonymerlin.data.*

object DummyAlgebra extends Algebra[IO] {

  val status = StateReport.report

  def step(railway: Railway, pickups: Dequeue[IO, Pickup]): IO[Railway] = IO.pure(railway)

  def enqueue(req: PickupReq, pickups: Dequeue[IO, Pickup]): IO[Valid[Pickup]] = {
    val PickupReq(a, b) = req
    IO.pure(Pickup.from(a, b))
  }
}

object fixture extends Routes[IO]

class RouteSuite extends CatsEffectSuite {
  val railwayF: IO[Ref[IO, Railway]] =
    Ref.ofEffect(IO.fromEither(Line.of(1, 2).map(Railway(_)).leftMap(_ => new Throwable)))

  val emptyQueue: IO[Dequeue[IO, Pickup]] = Dequeue.unbounded[IO, Pickup]

  val service: IO[HttpApp[IO]] =
    railwayF.flatMap { railway =>
      emptyQueue.map(fixture.routes(DummyAlgebra, railway, _).orNotFound)
    }

  test("status")({
    val request: Request[IO] = Request(Method.GET, uri"/status")
    assertStatus(request, Status.Ok)
  })

  test("pickup - accepted") {
    val request: Request[IO] =
      Request(Method.POST, uri"/pickup").withEntity(json"""{"a": "1", "b": "2"}""")
    assertStatus(request, Status.Accepted)
  }

  test("pickup -  bad request") {
    val request: Request[IO] =
      Request(Method.POST, uri"/pickup").withEntity("""{"a": "-1", "b": "2"}""")
    assertStatus(request, Status.BadRequest)
  }

  test("step") {
    val request: Request[IO] = Request(Method.POST, uri"/step")
    assertStatus(request, Status.Accepted)
  }

  def assertStatus(req: Request[IO], status: Status): IO[Unit] =
    assertIO(service.flatMap(_.run(req)).map(_.status), status)
}
