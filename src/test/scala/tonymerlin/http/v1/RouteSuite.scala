package tonymerlin

import cats.effect.*
import cats.effect.IO
import munit.CatsEffectSuite
import org.http4s.*
import org.http4s.implicits.*
import tonymerlin.data.Line
import tonymerlin.data.Railway
import tonymerlin.data.Valid
import tonymerlin.http.v1.Routes

class RouteSuite extends CatsEffectSuite {
  val railway: Valid[Railway] =
    Line.of(1, 2).map(Railway(_))

  object routes extends Routes[IO]
  val service: HttpApp[IO] = routes.routes(railway).orNotFound

  test("status route") {
    val response: IO[Response[IO]] =
      service.run(Request(Method.GET, uri"/status"))

    assertIO(response.map(_.status), Status.Ok)

  }
}
