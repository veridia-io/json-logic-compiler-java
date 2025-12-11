package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AllExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testEmptyArray() throws JsonProcessingException {
    assertEquals(false, jsonLogic.apply("{\"all\": [[], {\">\": [{\"var\": \"item\"}, 0]}]}", null));
  }

  @Test
  public void testAll() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"all\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 0]}]}", null));
    assertEquals(false, jsonLogic.apply("{\"all\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 1]}]}", null));
  }
}
