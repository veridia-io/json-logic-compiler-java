package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.veridia.jsonlogic.helpers.ToBoolean;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class GeneralTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testNullExpression() throws JsonProcessingException {
      assertNull(jsonLogic.apply(null, null));
  }

  @Test
  public void testEmptyExpression() throws JsonProcessingException {
    var m = Map.of("one", 1);
    assertEquals(m, jsonLogic.apply("{\"var\": \"\"}", m));

    assertNull(jsonLogic.apply("{\"var\": \"\"}", null));

    assertNull(jsonLogic.apply("", null));
  }
}
