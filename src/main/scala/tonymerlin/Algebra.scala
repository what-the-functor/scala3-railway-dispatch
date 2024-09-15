package tonymerlin

import cats.effect.kernel.Sync
import cats.syntax.all.*

import data.*
import cats.effect.std.Dequeue

trait Algebra[F[_]: Sync] {
  def status: Railway => RailwayReport
  def enqueue(req: PickupReq, pickups: Dequeue[F, Pickup]): F[Valid[Pickup]]

  /** Move the railway state forward by one step */
  def step(railway: Railway, pickups: Dequeue[F, Pickup]): F[Railway]
}

object Algebra {
  def apply[F[_]: Sync] =
    new Algebra[F] {
      val status: Railway => RailwayReport = StateReport.report

      // TODO validate terminals
      def enqueue(req: PickupReq, pickups: Dequeue[F, Pickup]): F[Valid[Pickup]] = {
        val PickupReq(a, b) = req
        Pickup.from(a, b).traverse(pickup => pickups.offer(pickup).map(_ => pickup))
      }

      def step(railway: Railway, pickups: Dequeue[F, Pickup]): F[Railway] =
        railway.step(pickups)
    }
}
