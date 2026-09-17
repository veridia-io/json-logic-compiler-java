package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util as ju

class PresenceExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  test("exists: true") {
    jsonLogic.apply("{ \"exists\": { \"var\": \"a\" } }", ju.Map.of("a", java.lang.Integer.valueOf(1))) shouldEqual true
  }

  test("exists: false when missing") {
    jsonLogic.apply("{ \"exists\": { \"var\": \"a\" } }", ju.Map.of()) shouldEqual false
  }

  test("exists: false when explicitly null") {
    val ctx = new ju.HashMap[String, Object]()
    ctx.put("a", null)
    jsonLogic.apply("{ \"exists\": { \"var\": \"a\" } }", ctx) shouldEqual false
  }

  test("exists: nested var") {
    val ctx = ju.Map.of("request", ju.Map.of("referrer", ju.Map.of("host", "google.com")))
    jsonLogic.apply("{ \"exists\": { \"var\": \"request.referrer.host\" } }", ctx) shouldEqual true
  }

  test("exists: false when an intermediate path segment is missing") {
    val ctx = ju.Map.of("request", ju.Map.of())
    jsonLogic.apply("{ \"exists\": { \"var\": \"request.referrer.host\" } }", ctx) shouldEqual false
  }

  test("exists: false when the leaf value is explicitly null") {
    val referrer = new ju.HashMap[String, Object]()
    referrer.put("host", null)
    val request = new ju.HashMap[String, Object]()
    request.put("referrer", referrer)
    val ctx = new ju.HashMap[String, Object]()
    ctx.put("request", request)
    jsonLogic.apply("{ \"exists\": { \"var\": \"request.referrer.host\" } }", ctx) shouldEqual false
  }

  test("exists: array index") {
    val ctx = ju.Map.of("items", ju.List.of(ju.Map.of("id", java.lang.Integer.valueOf(123)), ju.Map.of("id", java.lang.Integer.valueOf(456))))
    jsonLogic.apply("{ \"exists\": { \"var\": \"items.0.id\" } }", ctx) shouldEqual true
  }

  test("exists: array index out of bounds") {
    val ctx = ju.Map.of("items", ju.List.of(ju.Map.of("id", java.lang.Integer.valueOf(123))))
    jsonLogic.apply("{ \"exists\": { \"var\": \"items.2.id\" } }", ctx) shouldEqual false
  }

  test("not_exists: true when missing") {
    jsonLogic.apply("{ \"not_exists\": { \"var\": \"a\" } }", ju.Map.of()) shouldEqual true
  }

  test("not_exists: false when present") {
    jsonLogic.apply("{ \"not_exists\": { \"var\": \"a\" } }", ju.Map.of("a", java.lang.Integer.valueOf(123))) shouldEqual false
  }

  test("exists: an empty string still exists") {
    jsonLogic.apply("{ \"exists\": { \"var\": \"a\" } }", ju.Map.of("a", "")) shouldEqual true
  }

  test("exists: an empty array still exists") {
    jsonLogic.apply("{ \"exists\": { \"var\": \"a\" } }", ju.Map.of("a", new Array[Object](0))) shouldEqual true
  }
