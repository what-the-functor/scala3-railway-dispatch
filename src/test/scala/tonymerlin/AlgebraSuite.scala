package tonymerlin

import cats.effect.IO
import cats.effect.std.Dequeue
import cats.effect.unsafe.IORuntime
import munit.CatsEffectSuite
import munit.ScalaCheckEffectSuite
import org.scalacheck.Gen
import org.scalacheck.effect.PropF
import tonymerlin.data.Pickup
import tonymerlin.data.PickupReq

class AlgebraSuite extends munit.CatsEffectSuite with ScalaCheckEffectSuite {

  implicit val runtime: IORuntime = IORuntime.global

  val algebra = Algebra[IO]

  val pickupReqGen: Gen[PickupReq] =
    for
      a <- Gen.choose(1, 32)
      b <- Gen.choose(1, 32)
    yield PickupReq(a, b)

  test("Pickup is enqueued") {
    PropF.forAllF(pickupReqGen) { (req: PickupReq) =>
      for
        queue  <- Dequeue.unbounded[IO, Pickup]
        _      <- algebra.enqueue(req, queue)
        pickup <- queue.take
        expected = Pickup.from(req.a, req.b)
      yield assert(expected.forall(pickup == _))
    }
  }
}
