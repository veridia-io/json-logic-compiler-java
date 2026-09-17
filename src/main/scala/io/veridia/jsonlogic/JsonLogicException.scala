package io.veridia.jsonlogic

/** Unchecked: replaces the checked `JsonProcessingException` the Jackson-based engine used to
  * throw. No caller in `segmentation-engine-service` catches that checked type, so this is a
  * strict compatibility improvement, not a behavior change.
  */
class JsonLogicException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)
