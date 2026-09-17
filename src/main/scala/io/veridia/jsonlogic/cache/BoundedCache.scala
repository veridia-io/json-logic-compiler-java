package io.veridia.jsonlogic.cache

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}

/** Thin wrapper over a Caffeine cache: fixed max size, no time-based eviction — the same contract
  * the Guava cache this replaces had, but safe under the concurrent, per-tenant-shared access
  * pattern `segmentation-engine-service`'s three static `JsonLogic` instances put it under.
  */
final class BoundedCache[K <: AnyRef, V <: AnyRef](maximumSize: Long):
  private val underlying: Cache[K, V] = Caffeine.newBuilder().maximumSize(maximumSize).build[K, V]()

  def getIfPresent(key: K): V = underlying.getIfPresent(key)

  def put(key: K, value: V): Unit = underlying.put(key, value)
