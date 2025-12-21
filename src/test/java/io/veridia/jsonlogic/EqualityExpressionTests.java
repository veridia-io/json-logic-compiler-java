package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EqualityExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testSameValueSameType() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"==\": [1, 1]}", null));
  }

  @Test
  public void testSameValueDifferentType() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"==\": [1, 1]}", null));
  }

  @Test
  public void testDifferentValueDifferentType() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"==\": [[], false]}", null));
  }

  @Test
  public void testEmptyStringAndZeroComparison() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"==\": [\" \", 0]}", null));
  }

  //
  // 1. NUMERIC STRING vs NUMBER
  //

  @Test
  public void testNumericStringEqualsNumber() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [\"0.0\", 0]}", null));
  }

  @Test
  public void testNumericStringEqualsNumber2() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [\"5\", 5]}", null));
  }

  @Test
  public void testNumericStringEqualsNumber3() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [\"7.43\", 7.43]}", null));
  }

  //
  // 2. BOOLEAN vs NUMBER
  //

  @Test
  public void testFalseEqualsZero() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [false, 0]}", null));
  }

  @Test
  public void testTrueEqualsOne() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [true, 1]}", null));
  }

  @Test
  public void testBooleanNotEqualsWrongNumber() throws JsonProcessingException {
    assertEquals(false,
            jsonLogic.apply("{\"==\": [true, 2]}", null));
  }

  //
  // 3. STRING vs STRING
  //

  @Test
  public void testStringEqualsString() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [\"foo\", \"foo\"]}", null));
  }

  @Test
  public void testStringNotEqualsString() throws JsonProcessingException {
    assertEquals(false,
            jsonLogic.apply("{\"==\": [\"foo\", \"bar\"]}", null));
  }

  //
  // 4. NULL COERCION
  //

  @Test
  public void testNullEqualsZero() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [null, 0]}", null));
  }

  @Test
  public void testNullEqualsFalse() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [null, false]}", null));
  }

  @Test
  public void testNullEqualsNumericString() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [null, \"0.0\"]}", null));
  }

  @Test
  public void testNullNotEqualsNonNumericString() throws JsonProcessingException {
    assertEquals(false,
            jsonLogic.apply("{\"==\": [null, \"foo\"]}", null));
  }

  //
  // 5. MIXED NEGATIVE CASES
  //

  @Test
  public void testNumberNotEqualsNonNumericString() throws JsonProcessingException {
    assertEquals(false,
            jsonLogic.apply("{\"==\": [5, \"foo\"]}", null));
  }

  @Test
  public void testFalseNotEqualsNonZeroNumber() throws JsonProcessingException {
    assertEquals(false,
            jsonLogic.apply("{\"==\": [false, 2]}", null));
  }

  @Test
  public void testNumericStringNotEqualsNullString() throws JsonProcessingException {
    assertEquals(false,
            jsonLogic.apply("{\"==\": [\"0\", \"foo\"]}", null));
  }

  //
  // 6. BOTH NULL
  //

  @Test
  public void testNullEqualsNull() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [null, null]}", null));
  }

  //
  // 7. EDGE CASES
  //

  @Test
  public void testWhitespaceStringNumericCoercion() throws JsonProcessingException {
    assertEquals(true,
            jsonLogic.apply("{\"==\": [\" 7.5 \", 7.5]}", null));
  }

  @Test
  public void testWhitespaceNonNumericString() throws JsonProcessingException {
    assertEquals(false,
            jsonLogic.apply("{\"==\": [\" foo \", \"foo\"]}", null));
  }

  @Test
  public void testNumericStringEqualsBooleanTrue() throws JsonProcessingException {
    // "1" → 1, true → 1 → numeric equality
    assertEquals(true,
            jsonLogic.apply("{\"==\": [\"1\", true]}", null));
  }
}
