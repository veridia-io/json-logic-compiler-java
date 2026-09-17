package io.veridia.jsonlogic.operators

import io.veridia.jsonlogic.{CompiledExpression, Operator}

import java.util as ju

/** `exists`/`not_exists` — a `null` (missing or explicit) value is "does not exist". */
object Presence:

  private def presence(sym: String, negate: Boolean): Operator = new Operator:
    def key(): String = sym
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() != 1 then throw new IllegalArgumentException(s"Operator '$sym' expects exactly 1 argument")
      val arg = args.get(0)
      ctx =>
        val exists = arg.eval(ctx) != null
        java.lang.Boolean.valueOf(if negate then !exists else exists)

  val Exists: Operator = presence("exists", negate = false)
  val NotExists: Operator = presence("not_exists", negate = true)

  val all: List[Operator] = List(Exists, NotExists)
