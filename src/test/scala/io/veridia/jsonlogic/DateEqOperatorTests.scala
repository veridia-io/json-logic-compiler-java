package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.Instant

class DateEqOperatorTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  test("default components are full calendar-date equality") {
    val a = Instant.parse("2024-06-15T08:00:00Z").toEpochMilli
    val b = Instant.parse("2024-06-15T23:00:00Z").toEpochMilli // same day, different time
    jsonLogic.check(s"""{"date_eq": [$a, $b]}""", null) shouldEqual true

    val c = Instant.parse("2024-06-16T08:00:00Z").toEpochMilli // next day
    jsonLogic.check(s"""{"date_eq": [$a, $c]}""", null) shouldEqual false
  }

  test("month/day components ignore year -- the birthday case") {
    val birthdate = Instant.parse("1990-06-15T00:00:00Z").toEpochMilli
    val todayIsTheBirthday = Instant.parse("2024-06-15T00:00:00Z").toEpochMilli
    val todayIsNot = Instant.parse("2024-06-16T00:00:00Z").toEpochMilli

    jsonLogic.check(s"""{"date_eq": [$birthdate, $todayIsTheBirthday, ["month", "day"]]}""", null) shouldEqual true
    jsonLogic.check(s"""{"date_eq": [$birthdate, $todayIsNot, ["month", "day"]]}""", null) shouldEqual false
    // Same month/day but different year — full-date default equality must reject it, proving the
    // two component sets are genuinely different checks, not just an alias.
    jsonLogic.check(s"""{"date_eq": [$birthdate, $todayIsTheBirthday]}""", null) shouldEqual false
  }

  test("a single component matches any year") {
    val a = Instant.parse("1990-06-01T00:00:00Z").toEpochMilli
    val b = Instant.parse("2024-06-30T00:00:00Z").toEpochMilli
    jsonLogic.check(s"""{"date_eq": [$a, $b, ["month"]]}""", null) shouldEqual true
  }

  test("negation via the existing ! operator") {
    // No dedicated date_ne — {"!": [{"date_eq": [...]}]} is the negation.
    val a = Instant.parse("2024-06-15T00:00:00Z").toEpochMilli
    val b = Instant.parse("2024-06-16T00:00:00Z").toEpochMilli
    jsonLogic.check(s"""{"!": [{"date_eq": [$a, $b]}]}""", null) shouldEqual true
  }

  test("honors an explicit timezone at a day boundary") {
    // 2024-06-15T23:30Z is already 2024-06-16 in Asia/Tokyo (UTC+9).
    val a = Instant.parse("2024-06-15T23:30:00Z").toEpochMilli
    val b = Instant.parse("2024-06-16T01:00:00Z").toEpochMilli

    jsonLogic.check(s"""{"date_eq": [$a, $b]}""", null) shouldEqual false // UTC default
    jsonLogic.check(s"""{"date_eq": [$a, $b, ["year","month","day"], "Asia/Tokyo"]}""", null) shouldEqual true
  }
