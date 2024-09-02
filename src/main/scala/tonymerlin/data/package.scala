package tonymerlin

import errors.RailwayError

package object data {
  type Valid[A] = Either[RailwayError, A]
}
