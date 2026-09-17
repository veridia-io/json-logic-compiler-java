package io.veridia.jsonlogic

/** Resolves a `var` path against a root context object. Walks only `java.util.Map`/
  * `java.util.List`/`Object[]` — never a POJO's fields, matching the compiler's dynamic,
  * duck-typed evaluation model.
  */
object PathResolver:

  def split(path: String): Array[String] =
    if path == null || path.isEmpty then Array.empty else path.split("\\.")

  def resolve(root: Any, path: String): Any =
    if root == null then null
    else if path == null || path.isEmpty then root
    else resolve(root, split(path))

  def resolve(root: Any, parts: Array[String]): Any =
    if root == null then null
    else if parts == null || parts.isEmpty then root
    else
      var current: Any = root
      var i = 0
      while current != null && i < parts.length do
        current = step(current, parts(i))
        i += 1
      current

  def resolveSingle(root: Any, key: String): Any =
    if root == null then null else step(root, key)

  private def step(current: Any, part: String): Any = current match
    case m: java.util.Map[?, ?] => m.asInstanceOf[java.util.Map[String, AnyRef]].get(part)
    case l: java.util.List[?] =>
      val idx = parseIndex(part)
      if idx < 0 || idx >= l.size then null else l.get(idx)
    case a: Array[AnyRef] =>
      val idx = parseIndex(part)
      if idx < 0 || idx >= a.length then null else a(idx)
    case _ => null

  /** Parses a non-negative array index without throwing; `-1` for anything else. */
  private def parseIndex(part: String): Int =
    if part.isEmpty then -1
    else
      var value = 0
      var i = 0
      var ok = true
      while ok && i < part.length do
        val c = part.charAt(i)
        if c < '0' || c > '9' then ok = false
        else
          val digit = c - '0'
          if value > (Int.MaxValue - digit) / 10 then ok = false else value = value * 10 + digit
        i += 1
      if ok then value else -1
