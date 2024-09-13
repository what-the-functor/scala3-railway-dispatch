package tonymerlin

import cats.effect.kernel.Sync
import cats.effect.std.Queue
import cats.syntax.all.*

import data.*

trait Algebra[F[_]: Sync] {
  def status: Railway => RailwayReport
  def enqueue(req: PickupReq, pickups: Queue[F, Pickup]): F[Valid[Pickup]]

  /** Move the railway state forward by one step */
  def step(railway: Railway, queue: Queue[F, Pickup]): F[Unit] = Sync[F].unit
}

object Algebra {
  def apply[F[_]: Sync] =
    new Algebra[F] {
      val status: Railway => RailwayReport = StateReport.report

      def enqueue(req: PickupReq, pickups: Queue[F, Pickup]): F[Valid[Pickup]] = {
        val PickupReq(a, b) = req
        Pickup.from(a, b).traverse(pickup => pickups.offer(pickup).map(_ => pickup))
      }
    }
}
