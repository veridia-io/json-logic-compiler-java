package io.veridia.jsonlogic.helpers

// Truthiness rules derived from json-logic-java (MIT License) — see NOTICE.md.
object ToBoolean:
  def eval(value: Any): Boolean = value match
    case null => false
    case b: java.lang.Boolean => b.booleanValue()
    case n: Number =>
      val d = n.doubleValue()
      !d.isNaN && d != 0.0
    case s: String => s.nonEmpty
    case c: java.util.Collection[?] => !c.isEmpty
    case a if a.getClass.isArray => java.lang.reflect.Array.getLength(a) > 0
    case _ => true
