package io.veridia.jsonlogic

import io.veridia.jsonlogic.operators.*

import scala.jdk.CollectionConverters.*

final class JsonLogicCompiler:
  private val registry = new OperatorRegistry
  registerDefaultOperators()

  def compile(expr: Expr): CompiledExpression = expr match
    case Expr.Const(value) =>
      _ => value

    case Expr.Var(path, default) =>
      val defaultValue: AnyRef = default.orNull
      if path.isEmpty then
        ctx => if ctx != null then ctx else defaultValue
      else
        val parts = PathResolver.split(path)
        if parts.length == 1 then
          val key = parts(0)
          ctx =>
            val resolved = PathResolver.resolveSingle(ctx, key)
            if resolved != null then resolved.asInstanceOf[AnyRef] else defaultValue
        else
          ctx =>
            val resolved = PathResolver.resolve(ctx, parts)
            if resolved != null then resolved.asInstanceOf[AnyRef] else defaultValue

    case Expr.Op(name, args) =>
      val operator = registry.get(name)
      val compiledArgs: java.util.List[CompiledExpression] = args.map(compile).asJava
      operator.compile(compiledArgs)

  private def registerDefaultOperators(): Unit =
    (Equality.all ++ NumericOps.all ++ Arithmetic.all ++ Logic.all ++
      ArrayPredicates.all ++ Strings.all ++ Presence.all ++ DateTime.all)
      .foreach(registry.register)

  def registerOperator(impl: Operator): Unit = registry.register(impl)
