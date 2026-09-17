package io.veridia.jsonlogic

import io.circe.Json
import io.veridia.jsonlogic.values.Values

/** Walks a circe `Json` rule tree into an `Expr` AST. Distinguishes operator objects (exactly one
  * field) from literal objects (any other field count) and literal arrays, per JSON Logic's own
  * "one key = operator" convention.
  */
object LogicParser:

  def parse(json: Json): Expr =
    json.asObject match
      case Some(obj) =>
        obj.toList match
          case (op, value) :: Nil => parseField(op, value)
          case _ => Expr.Const(Values.jsonToValue(json)) // 0 or 2+ fields -> literal map
      case None => Expr.Const(Values.jsonToValue(json)) // scalar or literal array

  private def parseField(op: String, value: Json): Expr =
    if op == "var" then parseVar(value)
    else
      value.asArray match
        case Some(arr) => Expr.Op(op, arr.map(parse).toList)
        case None => Expr.Op(op, List(parse(value)))

  private def parseVar(value: Json): Expr =
    value.asArray match
      case Some(arr) if arr.nonEmpty =>
        val path = Values.jsonScalarAsText(arr.head)
        val default = if arr.size > 1 then Some(Values.jsonToValue(arr(1))) else None
        Expr.Var(path, default)
      case _ =>
        Expr.Var(Values.jsonScalarAsText(value), None)
