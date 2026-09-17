package io.veridia.jsonlogic.operators

import io.veridia.jsonlogic.{CompiledExpression, Operator}
import io.veridia.jsonlogic.helpers.ToBoolean

import java.util as ju

/** `==`/`!=` — loose, cross-type equality: numeric-vs-string/boolean coercion, `null` treated as
  * `0`, matching JSON-Logic's historical JS-derived semantics rather than strict same-type
  * equality.
  */
object Equality:

  private def looseEquals(rawLeft: Any, rawRight: Any): Boolean =
    val left: AnyRef = if rawLeft == null then java.lang.Double.valueOf(0.0) else rawLeft.asInstanceOf[AnyRef]
    val right: AnyRef = if rawRight == null then java.lang.Double.valueOf(0.0) else rawRight.asInstanceOf[AnyRef]
    (left, right) match
      case (a: Number, b: Number) => a.doubleValue() == b.doubleValue()
      case (a: Number, b: String) => numberEqualsString(a, b)
      case (a: Number, b: java.lang.Boolean) => numberEqualsBoolean(a, b)
      case (a: String, b: String) => a == b
      case (a: String, b: Number) => numberEqualsString(b, a)
      case (a: String, b: java.lang.Boolean) => stringEqualsBoolean(a, b)
      case (a: java.lang.Boolean, b: java.lang.Boolean) => a.booleanValue() == b.booleanValue()
      case (a: java.lang.Boolean, b: Number) => numberEqualsBoolean(b, a)
      case (a: java.lang.Boolean, b: String) => stringEqualsBoolean(b, a)
      case _ => !ToBoolean.eval(left) && !ToBoolean.eval(right)

  private def numberEqualsString(left: Number, right: String): Boolean =
    val text = if right.isBlank then "0" else right
    try text.toDouble == left.doubleValue()
    catch case _: NumberFormatException => false

  private def numberEqualsBoolean(left: Number, right: java.lang.Boolean): Boolean =
    left.doubleValue() == (if right.booleanValue() then 1.0 else 0.0)

  private def stringEqualsBoolean(left: String, right: java.lang.Boolean): Boolean =
    ToBoolean.eval(left) == right.booleanValue()

  val Eq: Operator = new Operator:
    def key(): String = "=="
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      ctx => java.lang.Boolean.valueOf(looseEquals(args.get(0).eval(ctx), args.get(1).eval(ctx)))

  val Neq: Operator = new Operator:
    def key(): String = "!="
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      ctx => java.lang.Boolean.valueOf(!looseEquals(args.get(0).eval(ctx), args.get(1).eval(ctx)))

  val all: List[Operator] = List(Eq, Neq)
