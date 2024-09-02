package tonymerlin

import tonymerlin.data.{Report, Reportable}

object StateReport {
  def report[A: Reportable](value: A): Report[A] =
    Reportable[A].report(value)
}
