package tonymerlin

import tonymerlin.data.{Report, Reportable}

object StateReport {
  def report[A, B <: Report[A]](value: A)(implicit r: Reportable[A, B]): B =
    Reportable[A, B].report(value)
}
