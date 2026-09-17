package io.veridia.jsonlogic

import scala.collection.concurrent.TrieMap

/** Lock-free, thread-safe operator dispatch table — every `apply`/`check` call may run
  * concurrently across tenants sharing one `JsonLogic` instance.
  */
final class OperatorRegistry:
  private val ops = TrieMap.empty[String, Operator]

  def register(impl: Operator): Unit = ops.put(impl.key(), impl)

  def get(name: String): Operator =
    ops.getOrElse(name, throw new UnsupportedOperationException(s"Operator not implemented: $name"))
