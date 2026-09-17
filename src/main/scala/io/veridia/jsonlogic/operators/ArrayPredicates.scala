package io.veridia.jsonlogic.operators

import io.veridia.jsonlogic.{CompiledExpression, Operator}
import io.veridia.jsonlogic.helpers.ToBoolean

import java.util as ju

/** `all`/`some`/`none` (each receives `"item"` as the loop variable) and `in` (substring /
  * list-membership / map-key-membership, by container type).
  */
object ArrayPredicates:

  val All: Operator = new Operator:
    def key(): String = "all"
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      val arrayExpr = args.get(0)
      val condExpr = args.get(1)
      ctx =>
        arrayExpr.eval(ctx) match
          case list: ju.List[?] if !list.isEmpty =>
            // One reusable child context per eval; the "item" slot is overwritten each iteration
            // rather than allocating a fresh map per element.
            val childCtx = new ju.HashMap[String, Object](2)
            var ok = true
            val it = list.iterator()
            while ok && it.hasNext do
              childCtx.put("item", it.next().asInstanceOf[Object])
              if !ToBoolean.eval(condExpr.eval(childCtx)) then ok = false
            java.lang.Boolean.valueOf(ok)
          case _ => java.lang.Boolean.FALSE // non-array or empty array -> false

  private def existential(sym: String, isSome: Boolean): Operator = new Operator:
    def key(): String = sym
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      val arrayExpr = args.get(0)
      val condExpr = args.get(1)
      ctx =>
        arrayExpr.eval(ctx) match
          case list: ju.List[?] if !list.isEmpty =>
            val childCtx = new ju.HashMap[String, Object](2)
            var found = false
            val it = list.iterator()
            while !found && it.hasNext do
              childCtx.put("item", it.next().asInstanceOf[Object])
              if ToBoolean.eval(condExpr.eval(childCtx)) then found = true
            java.lang.Boolean.valueOf(if isSome then found else !found)
          case _ => java.lang.Boolean.valueOf(!isSome) // non-array or empty array

  val SomeOp: Operator = existential("some", isSome = true)
  val NoneOp: Operator = existential("none", isSome = false)

  val In: Operator = new Operator:
    def key(): String = "in"
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() < 2 then (_ => java.lang.Boolean.FALSE)
      else
        val needleExpr = args.get(0)
        val containerExpr = args.get(1)
        ctx =>
          val needle = needleExpr.eval(ctx)
          containerExpr.eval(ctx) match
            case null => java.lang.Boolean.FALSE
            case container: String =>
              needle match
                case n: String => java.lang.Boolean.valueOf(container.contains(n))
                case _ => java.lang.Boolean.FALSE
            case container: ju.List[?] =>
              needle match
                case n: Number =>
                  // Numeric loose equality, matching Equality: a Long needle (e.g. date_diff's
                  // result) must match an Integer literal element (how JSON ints parse) — raw
                  // List.contains() uses equals(), and Long(2).equals(Integer(2)) is false.
                  var found = false
                  val it = container.iterator()
                  while !found && it.hasNext do
                    it.next() match
                      case item: Number if n.doubleValue() == item.doubleValue() => found = true
                      case _ => ()
                  java.lang.Boolean.valueOf(found)
                case _ => java.lang.Boolean.valueOf(container.contains(needle))
            case container: ju.Map[?, ?] => java.lang.Boolean.valueOf(container.containsKey(needle))
            case _ => java.lang.Boolean.FALSE

  val all: List[Operator] = List(All, SomeOp, NoneOp, In)
