package tonymerlin
package errors

sealed abstract class RailwayError(val msg: String)

case object IntNotPositive extends RailwayError("The value must be positive")

case object IntLessThanTwo extends RailwayError("The minimum value is two")

case object InvalidPickupSource extends RailwayError("The pickup source terminal does not exist in this railway")

case object InvalidPickupDestiniation
    extends RailwayError("The pickup destination terminal does not exist in this railway")
