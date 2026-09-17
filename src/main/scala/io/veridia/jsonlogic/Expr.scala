package io.veridia.jsonlogic

/** The parsed JSON Logic AST: a literal, a `var` reference, or an operator call. */
enum Expr:
  case Const(value: AnyRef)
  case Var(path: String, default: Option[AnyRef])
  case Op(name: String, args: List[Expr])
