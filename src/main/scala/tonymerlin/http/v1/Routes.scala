package tonymerlin
package http
package v1

import cats.effect.kernel.Concurrent
import cats.effect.kernel.Sync
import cats.effect.std.Queue
import cats.syntax.all.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*

import data.*
import errors.RailwayError

trait Routes[F[_]: Sync] extends Codec[F] {
  val dsl = Http4sDsl[F]
  import dsl._

  def routes(algebra: Algebra[F], railway: Railway, pickups: Queue[F, Pickup]): HttpRoutes[F] =
    HttpRoutes
      .of[F] {
        case GET -> Root / "status" =>
          Ok(algebra.status(railway).asJson)

        case req @ POST -> Root / "pickup" =>
          for
            pickupReq <- req.as[PickupReq]
            errorOrPickup <- algebra.enqueue(pickupReq, pickups)
            resp <-
              errorOrPickup match
                case Right(_)    => Accepted()
                case Left(error) => BadRequest(error.asJson)
          yield resp

        case POST -> Root / "step" =>
          algebra
            .step(railway, pickups)
            .flatMap(_ => Accepted())
            .handleErrorWith(e => InternalServerError(e.getMessage))
      }
}

trait Codec[F[_]: Concurrent] {
  import _root_.io.circe.generic.semiauto._

  implicit val intPositiveEncoder: Encoder[IntPositive] =
    Encoder[Int].contramap(_.value)

  implicit val intPositiveDecoder: Decoder[Valid[IntPositive]] =
    Decoder[Int].map(IntPositive.from(_))

  implicit val terminalDecoder: Decoder[Valid[Terminal]] =
    Decoder[Int].map(Terminal.from(_))

  implicit val directionEncoder: Encoder[Direction] =
    Encoder[String].contramap {
      case Forward  => "forward"
      case Backward => "backward"
    }

  implicit val pickupDecoder: Decoder[PickupReq] = deriveDecoder
  implicit val pickupEntityDecoder: EntityDecoder[F, PickupReq] = jsonOf[F, PickupReq]
}
