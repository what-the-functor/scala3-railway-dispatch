package tonymerlin
package http
package v1

import cats.effect.kernel.Concurrent
import cats.effect.kernel.Sync
import cats.syntax.all.*
import cats.instances.all.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*

import data.*
import errors.*
import cats.effect.std.Dequeue
import cats.effect.kernel.Ref

trait Routes[F[_]: Sync] extends Codec[F] {
  val dsl = Http4sDsl[F]
  import dsl._

  def routes(algebra: Algebra[F], railwayRef: Ref[F, Railway], pickups: Dequeue[F, Pickup]): HttpRoutes[F] =
    HttpRoutes
      .of[F] {
        case GET -> Root / "status" =>
          for
            railway <- railwayRef.get
            qSize   <- pickups.size
            resp    <- Ok((qSize, algebra.status(railway)).asJson)
          yield resp

        case req @ POST -> Root / "pickup" =>
          for
            pickupReq <- req.as[PickupReq]
            maybeValidReq = Either
              .right[RailwayError, PickupReq](pickupReq)
              .ensure(InvalidPickupSource)(req => req.a >= 1 && req.a <= 32)
              .ensure(InvalidPickupDestination)(req => req.b >= 1 && req.b <= 32)
              .ensure(InvalidPickupNoDiff)(req => req.a != req.b)
            errorOrPickup <- maybeValidReq.flatTraverse(algebra.enqueue.curried(pickups))
            resp <-
              errorOrPickup match
                case Right(_)    => Accepted()
                case Left(error) => BadRequest(error.asJson)
          yield resp

        case POST -> Root / "step" =>
          for
            railway <- railwayRef.get
            updated <- algebra.step(railway, pickups)
            _       <- railwayRef.update(_ => updated)
            resp <-
              if railway == updated then Accepted("Unchanged")
              else Accepted("Changed")
          yield resp
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

  implicit val pickupDecoder: Decoder[PickupReq]                = deriveDecoder
  implicit val pickupEntityDecoder: EntityDecoder[F, PickupReq] = jsonOf[F, PickupReq]
}
