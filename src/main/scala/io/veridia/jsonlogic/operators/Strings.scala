package io.veridia.jsonlogic.operators

import io.veridia.jsonlogic.{CompiledExpression, Operator}

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.google.re2j.{Pattern, PatternSyntaxException}

import java.util as ju

/** `contains`/`starts_with`/`ends_with` and `regex_match` (RE2, ReDoS-safe by construction, with
  * fixed input/pattern length caps and a compiled-pattern cache).
  */
object Strings:

  private def stringOp(sym: String, test: (String, String) => Boolean): Operator = new Operator:
    def key(): String = sym
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() != 2 then (_ => java.lang.Boolean.FALSE)
      else
        val leftExpr = args.get(0)
        val rightExpr = args.get(1)
        ctx =>
          val left = leftExpr.eval(ctx)
          val right = rightExpr.eval(ctx)
          if left == null || right == null then java.lang.Boolean.FALSE
          else
            // String.valueOf returns whatever a non-null object's own toString() returns, which a
            // pathological override can make null — re-check post-conversion, not just pre-.
            val leftStr = String.valueOf(left)
            val rightStr = String.valueOf(right)
            if leftStr == null || rightStr == null then java.lang.Boolean.FALSE
            else java.lang.Boolean.valueOf(test(leftStr, rightStr))

  val Contains: Operator = stringOp("contains", _.contains(_))
  val StartsWith: Operator = stringOp("starts_with", _.startsWith(_))
  val EndsWith: Operator = stringOp("ends_with", _.endsWith(_))

  private val MaxPatternLength = 256
  private val MaxInputLength = 1024

  val RegexMatch: Operator = new Operator:
    private val compiledPatterns: Cache[String, Pattern] =
      Caffeine.newBuilder().maximumSize(1024).build[String, Pattern]()

    def key(): String = "regex_match"

    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() != 2 then (_ => java.lang.Boolean.FALSE)
      else
        val leftExpr = args.get(0)
        val rightExpr = args.get(1)
        ctx =>
          val left = leftExpr.eval(ctx)
          val right = rightExpr.eval(ctx)
          if left == null || right == null then java.lang.Boolean.FALSE
          else
            val leftStr = String.valueOf(left)
            val rightStr = String.valueOf(right)
            if leftStr == null || rightStr == null then java.lang.Boolean.FALSE
            else java.lang.Boolean.valueOf(regexMatch(leftStr, rightStr))

    private def regexMatch(input: String, pattern: String): Boolean =
      if pattern.length > MaxPatternLength || input.length > MaxInputLength then false
      else
        try
          var compiled = compiledPatterns.getIfPresent(pattern)
          if compiled == null then
            compiled = Pattern.compile(pattern)
            compiledPatterns.put(pattern, compiled)
          compiled.matcher(input).find()
        catch case _: PatternSyntaxException => false // invalid regex -> fail closed

  val all: List[Operator] = List(Contains, StartsWith, EndsWith, RegexMatch)
