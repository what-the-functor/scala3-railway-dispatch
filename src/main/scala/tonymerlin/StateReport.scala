package tonymerlin

import tonymerlin.data.{Report, Reportable}

object StateReport {
  def report[A, B <: Report[A]](value: A)(using r: Reportable[A, B]): B =
    r.report(value)
}
