package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util as ju

class VariableTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private def map(kv: (String, Any)*): ju.Map[String, Object] =
    val m = new ju.HashMap[String, Object]()
    kv.foreach { case (k, v) => m.put(k, v.asInstanceOf[Object]) }
    m

  test("empty string returns the whole context") {
    jsonLogic.apply("{\"var\": \"\"}", java.lang.Double.valueOf(3.14)) shouldEqual 3.14
  }

  test("map access") {
    val data = map("pi" -> 3.14)
    jsonLogic.apply("{\"var\": \"pi\"}", data) shouldEqual 3.14
  }

  test("default value") {
    jsonLogic.apply("{\"var\": [\"pi\", 3.14]}", null) shouldEqual 3.14
  }

  test("undefined var resolves to null") {
    jsonLogic.apply("{\"var\": [\"pi\"]}", null) shouldBe null
    jsonLogic.apply("{\"var\": \"\"}", null) shouldBe null
    jsonLogic.apply("{\"var\": 0}", null) shouldBe null
  }

  test("array access by numeric var") {
    val data: Array[String] = Array("hello", "world")
    jsonLogic.apply("{\"var\": 0}", data.asInstanceOf[Object]) shouldEqual "hello"
    jsonLogic.apply("{\"var\": 1}", data.asInstanceOf[Object]) shouldEqual "world"
    jsonLogic.apply("{\"var\": 2}", data.asInstanceOf[Object]) shouldBe null
    jsonLogic.apply("{\"var\": 3}", data.asInstanceOf[Object]) shouldBe null
  }

  test("array access with string-keyed numeric var") {
    val data: Array[String] = Array("hello", "world")
    jsonLogic.apply("{\"var\": \"0\"}", data.asInstanceOf[Object]) shouldEqual "hello"
    jsonLogic.apply("{\"var\": \"1\"}", data.asInstanceOf[Object]) shouldEqual "world"
    jsonLogic.apply("{\"var\": \"2\"}", data.asInstanceOf[Object]) shouldBe null
    jsonLogic.apply("{\"var\": \"3\"}", data.asInstanceOf[Object]) shouldBe null
  }

  test("list access") {
    val data = ju.Arrays.asList("hello", "world")
    jsonLogic.apply("{\"var\": 0}", data) shouldEqual "hello"
    jsonLogic.apply("{\"var\": 1}", data) shouldEqual "world"
    jsonLogic.apply("{\"var\": 2}", data) shouldBe null
    jsonLogic.apply("{\"var\": 3}", data) shouldBe null
  }

  test("list access with string-keyed numeric var") {
    val data = ju.Arrays.asList("hello", "world")
    jsonLogic.apply("{\"var\": \"0\"}", data) shouldEqual "hello"
    jsonLogic.apply("{\"var\": \"1\"}", data) shouldEqual "world"
    jsonLogic.apply("{\"var\": \"2\"}", data) shouldBe null
    jsonLogic.apply("{\"var\": \"3\"}", data) shouldBe null
  }

  test("complex nested access") {
    val data = map(
      "users" -> ju.Arrays.asList(
        map("name" -> "John", "followers" -> 1337),
        map("name" -> "Jane", "followers" -> 2048)
      )
    )
    jsonLogic.apply("{\"var\": \"users.0.name\"}", data) shouldEqual "John"
    jsonLogic.apply("{\"var\": \"users.0.followers\"}", data) shouldEqual 1337
    jsonLogic.apply("{\"var\": \"users.1.name\"}", data) shouldEqual "Jane"
    jsonLogic.apply("{\"var\": \"users.1.followers\"}", data) shouldEqual 2048
  }

  test("missing nested map key returns the default") {
    val data = map("a" -> map("b" -> new ju.HashMap[String, Object]()))
    jsonLogic.apply("{\"var\": [\"a.b.c\", \"fallback\"]}", data) shouldEqual "fallback"
  }

  test("array index within bounds returns the element") {
    val data = map("items" -> ju.Arrays.asList(10, 20))
    val result = jsonLogic.apply("{\"var\": [\"items.1\", 999]}", data)
    result shouldBe a[Number]
    result.asInstanceOf[Number].doubleValue() shouldEqual 20.0
  }

  test("array index out of bounds returns the default") {
    val data = map("items" -> ju.Arrays.asList(10, 20))
    jsonLogic.apply("{\"var\": [\"items.2\", \"missing\"]}", data) shouldEqual "missing"
  }

  test("top-level numeric index over a list") {
    val data = ju.Arrays.asList("apple", "banana", "carrot")
    jsonLogic.apply("{\"var\": [1, \"missing\"]}", data) shouldEqual "banana"
  }

  test("empty var key returns the same data instance") {
    val data = map("x" -> 1)
    jsonLogic.apply("{\"var\": \"\"}", data) should be theSameInstanceAs data
  }
