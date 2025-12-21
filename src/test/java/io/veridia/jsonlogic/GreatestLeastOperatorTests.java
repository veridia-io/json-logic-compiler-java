package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GreatestLeastOperatorTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testSimpleGreatest() throws JsonProcessingException {
    assertEquals(10.0,
            jsonLogic.apply("{\"greatest\": [10, 3]}", null));
  }

  @Test
  public void testSimpleLeast() throws JsonProcessingException {
    assertEquals(3.0,
            jsonLogic.apply("{\"least\": [10, 3]}", null));
  }

  @Test
  public void testStringNumbers() throws JsonProcessingException {
    assertEquals(7.5,
            jsonLogic.apply("{\"greatest\": [\"7.5\", \"2\"]}", null));
  }

  @Test
  public void testNullHandling() throws JsonProcessingException {
    assertEquals(5.0,
            jsonLogic.apply("{\"greatest\": [null, 5]}", null));
    assertEquals(0.0,
            jsonLogic.apply("{\"least\": [null, 5]}", null));
  }

  @Test
  public void testBooleanValues() throws JsonProcessingException {
    assertEquals(3.0,
            jsonLogic.apply("{\"greatest\": [true, 3]}", null));
    assertEquals(0.0,
            jsonLogic.apply("{\"least\": [false, 5]}", null));
  }

  @Test
  public void testInvalidArgs() throws JsonProcessingException {
    assertEquals(0.0,
            jsonLogic.apply("{\"least\": [5]}", null)); // fallback
  }
}
