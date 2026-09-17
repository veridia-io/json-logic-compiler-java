package io.veridia.jsonlogic.values

import io.circe.{Json, JsonNumber}

import java.util as ju

/** Converts a parsed circe `Json` literal into the dynamic `Any`-typed value model the rest of the
  * engine (and every Java caller) expects: `null`, `java.lang.Boolean`, a `java.lang.Number`
  * subtype, `String`, `java.util.ArrayList[AnyRef]`, or `java.util.LinkedHashMap[String, AnyRef]`.
  * Java collections, not Scala's, so a literal array/object round-trips through the public API
  * exactly the way a caller-supplied `Map`/`List` context object already does.
  */
object Values:

  /** Mimics Jackson's default numeric-type inference (no `USE_BIG_DECIMAL_FOR_FLOATS`/
    * `USE_BIG_INTEGER_FOR_INTS`), which this engine's contract depended on before the rewrite: a
    * fractional/exponent literal becomes `Double`; an integral literal becomes the smallest of
    * `Integer`/`Long`/`BigInteger` that fits.
    */
  def jsonNumberToValue(n: JsonNumber): AnyRef =
    val text = n.toString
    val isIntegral = !text.exists(c => c == '.' || c == 'e' || c == 'E')
    if !isIntegral then java.lang.Double.valueOf(n.toDouble)
    else
      n.toLong match
        case Some(l) if l >= Int.MinValue && l <= Int.MaxValue => java.lang.Integer.valueOf(l.toInt)
        case Some(l) => java.lang.Long.valueOf(l)
        case None =>
          n.toBigDecimal.map(_.toBigInt.bigInteger).getOrElse(java.math.BigInteger.ZERO)

  def jsonToValue(json: Json): AnyRef =
    json.fold(
      jsonNull = null,
      jsonBoolean = b => java.lang.Boolean.valueOf(b),
      jsonNumber = jsonNumberToValue,
      jsonString = s => s,
      jsonArray = arr =>
        val list = new ju.ArrayList[AnyRef](arr.size)
        arr.foreach(el => list.add(jsonToValue(el)))
        list,
      jsonObject = obj =>
        val map = new ju.LinkedHashMap[String, AnyRef]()
        obj.toList.foreach { case (k, v) => map.put(k, jsonToValue(v)) }
        map
    )

  /** Mimics Jackson's very lenient `JsonNode.asText()`, used for a `var` path that may be a bare
    * number (array-index access, e.g. `{"var": 0}`), not just a string.
    */
  def jsonScalarAsText(json: Json): String =
    json.asString
      .orElse(json.asNumber.map(_.toString))
      .orElse(json.asBoolean.map(_.toString))
      .getOrElse("")
