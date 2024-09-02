package tonymerlin
package compat

import cats.Functor
import org.scalacheck.Gen

object scalacheck {
  object cats {

    implicit val genFunctor: Functor[Gen] =
      new Functor[Gen] {
        def map[A, B](fa: Gen[A])(f: A => B): Gen[B] = fa.map(f)
      }
  }
}
