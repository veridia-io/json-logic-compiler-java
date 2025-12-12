package io.veridia.jsonlogic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EqualityExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testSameValueSameType() {
    assertEquals(true, jsonLogic.apply("{\"==\": [1, 1]}", null));
  }

  @Test
  public void testSameValueDifferentType() {
    assertEquals(true, jsonLogic.apply("{\"==\": [1, 1]}", null));
  }

  @Test
  public void testDifferentValueDifferentType() {
    assertEquals(true, jsonLogic.apply("{\"==\": [[], false]}", null));
  }

  @Test
  public void testEmptyStringAndZeroComparison() {
    assertEquals(true, jsonLogic.apply("{\"==\": [\" \", 0]}", null));
  }
}
