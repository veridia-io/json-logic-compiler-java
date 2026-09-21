package io.veridia.jsonlogic.helpers

object ToDouble:
  def eval(o: Any): Double = o match
    case null => 0d
    case b: java.lang.Boolean => if b.booleanValue() then 1d else 0d
    case n: Number => n.doubleValue()
    case s: String =>
      val text = if s.isBlank then "0" else s
      try text.toDouble
      catch case _: NumberFormatException => 0d
    case _ => 0d
