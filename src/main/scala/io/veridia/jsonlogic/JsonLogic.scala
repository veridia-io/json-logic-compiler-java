package io.veridia.jsonlogic

import io.circe.parser.parse as parseJson
import io.veridia.jsonlogic.cache.BoundedCache
import io.veridia.jsonlogic.helpers.ToBoolean
import io.veridia.jsonlogic.values.Values

/** Entry point for evaluating JSON Logic expressions. Manages parsing, compilation, and a small
  * cache of compiled expressions — construction and every public method are safe to call
  * concurrently, and callers are expected to share one instance across many evaluations.
  */
final class JsonLogic:
  private val compiler = new JsonLogicCompiler
  private val cache = new BoundedCache[String, CompiledExpression](1000)

  /** Parses and compiles a JSON Logic expression, returning a reusable [[CompiledExpression]].
    * Results are cached per instance, keyed by the raw expression string; callers on hot paths
    * should compile once and invoke `eval` directly against pre-parsed contexts.
    */
  def compile(str: String): CompiledExpression =
    if str == null || str.isBlank then (_ => null)
    else
      val cached = cache.getIfPresent(str)
      if cached != null then cached
      else
        val json = parseJson(str).getOrElse(throw new JsonLogicException(s"Invalid JSON Logic expression: $str"))
        val compiled = compiler.compile(LogicParser.parse(json))
        cache.put(str, compiled)
        compiled

  /** Applies a JSON Logic expression against the provided context.
    *
    * @param str JSON Logic expression as a string
    * @param ctx context object (`Map`/`List`/POJO-free structure) or a JSON string to evaluate against
    * @return the raw evaluation result, or `null` when the expression is blank
    */
  def apply(str: String, ctx: Object): Object =
    if str == null || str.isBlank then null
    else
      val context: Object = ctx match
        case null => null
        case s: String =>
          parseJson(s).map(Values.jsonToValue).getOrElse(throw new JsonLogicException(s"Invalid JSON context: $s"))
        case other => other
      compile(str).eval(context)

  /** Applies a JSON Logic expression and coerces the result to a boolean. */
  def check(str: String, ctx: Object): Boolean = ToBoolean.eval(apply(str, ctx))

  /** Registers a custom operator implementation for use during compilation.
    *
    * @return this instance, for chaining
    */
  def registerOperator(impl: Operator): JsonLogic =
    compiler.registerOperator(impl)
    this
