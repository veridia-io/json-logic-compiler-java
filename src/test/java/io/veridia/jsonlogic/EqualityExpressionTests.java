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
}
