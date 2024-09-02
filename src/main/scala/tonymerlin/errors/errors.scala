package tonymerlin
package errors

sealed abstract class RailwayError(msg: String)

case object IntNotPositive extends RailwayError("The value must be positive")

case object IntLessThanTwo extends RailwayError("The minimum value is two")
