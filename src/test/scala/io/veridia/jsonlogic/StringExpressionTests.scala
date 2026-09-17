package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util as ju

class StringExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private val cases: Seq[(String, String, Boolean)] = Seq(
    ("contains: true", "{\"contains\": [\"hello world\", \"world\"]}", true),
    ("contains: false", "{\"contains\": [\"hello\", \"xyz\"]}", false),
    ("starts_with: true", "{\"starts_with\": [\"abcdef\", \"abc\"]}", true),
    ("starts_with: false", "{\"starts_with\": [\"abcdef\", \"bcd\"]}", false),
    ("ends_with: true", "{\"ends_with\": [\"abcdef\", \"def\"]}", true),
    ("ends_with: false", "{\"ends_with\": [\"abcdef\", \"abc\"]}", false),
    ("regex_match: simple", "{\"regex_match\": [\"google.com\", \"google\\\\.com\"]}", true),
    ("regex_match: find, not full match", "{\"regex_match\": [\"https://google.com/search\", \"google\\\\.com\"]}", true),
    ("regex_match: no match", "{\"regex_match\": [\"example.com\", \"google\"]}", false),
    ("regex_match: invalid pattern fails closed", "{\"regex_match\": [\"test\", \"(\"]}", false),
    ("contains: number coerces to its string form", "{\"contains\": [12345, \"234\"]}", true),
    ("contains: null left is false", "{\"contains\": [null, \"a\"]}", false),
    ("contains: null right is false", "{\"contains\": [\"abc\", null]}", false),
    ("contains: empty substring is true", "{\"contains\": [\"abc\", \"\"]}", true),
    ("starts_with: empty prefix is true", "{\"starts_with\": [\"abc\", \"\"]}", true),
    ("contains: on an empty string is false", "{\"contains\": [\"\", \"a\"]}", false),
    ("regex_match: empty input matches .*", "{\"regex_match\": [\"\", \".*\"]}", true),
    ("regex_match: anchors", "{\"regex_match\": [\"/pricing\", \"^/pricing$\"]}", true),
    ("contains: unicode", "{\"contains\": [\"привет мир\", \"мир\"]}", true)
  )

  for (name, json, expected) <- cases do
    test(name) {
      jsonLogic.apply(json, null) shouldEqual expected
    }

  test("regex_match: pattern over the 256-char safety limit fails closed") {
    val longPattern = "a".repeat(300)
    jsonLogic.apply(s"""{"regex_match": ["aaa", "$longPattern"]}""", null) shouldEqual false
  }

  test("regex_match: input over the 1024-char safety limit fails closed") {
    val longInput = "a".repeat(2000)
    jsonLogic.apply(s"""{"regex_match": ["$longInput", "a+"]}""", null) shouldEqual false
  }

  test("contains: resolves left operand via a context variable") {
    val ctx = ju.Map.of("request", ju.Map.of("path", "/pricing/plans"))
    jsonLogic.apply("{ \"contains\": [ {\"var\": \"request.path\"}, \"/pricing\" ] }", ctx) shouldEqual true
  }

  test("regex_match: resolves left operand via a context variable") {
    val ctx = ju.Map.of("referrer", ju.Map.of("host", "www.google.com"))
    jsonLogic.apply("{ \"regex_match\": [ {\"var\": \"referrer.host\"}, \"google\\\\.com$\" ] }", ctx) shouldEqual true
  }

  private object NullToString:
    override def toString: String = null

  test("contains: a non-null value whose own toString() returns null is false, not an NPE") {
    val ctx = ju.Collections.singletonMap("x", NullToString: Object)
    jsonLogic.apply("{\"contains\": [{\"var\": \"x\"}, \"abc\"]}", ctx) shouldEqual false
  }

  test("regex_match: a non-null value whose own toString() returns null is false, not an NPE") {
    val ctx = ju.Collections.singletonMap("x", NullToString: Object)
    jsonLogic.apply("{\"regex_match\": [{\"var\": \"x\"}, \"a+\"]}", ctx) shouldEqual false
  }
