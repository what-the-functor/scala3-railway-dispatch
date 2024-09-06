package tonymerlin
package compat

import org.scalacheck.Gen
import _root_.cats.Monad

package scalacheck {
  object cats {

    given Monad[Gen] with
      def flatMap[A, B](fa: Gen[A])(f: A => Gen[B]): Gen[B] = fa.flatMap(f)

      def tailRecM[A, B](a: A)(f: A => Gen[Either[A, B]]): Gen[B] =
        Gen.tailRecM(a)(f)

      def pure[A](x: A): Gen[A] = Gen.const(x)
  }
}
