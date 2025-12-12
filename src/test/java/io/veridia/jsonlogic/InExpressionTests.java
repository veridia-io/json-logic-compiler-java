package io.veridia.jsonlogic;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class InExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testStringIn() {
    assertEquals(true, jsonLogic.apply("{\"in\": [\"race\", \"racecar\"]}", null));
  }

  @Test
  public void testStringNotIn() {
    assertEquals(false, jsonLogic.apply("{\"in\": [\"race\", \"clouds\"]}", null));
    assertEquals(false, jsonLogic.apply("{\"in\": [null, \"clouds\"]}", null));
  }

  @Test
  public void testArrayIn() {
    assertEquals(true, jsonLogic.apply("{\"in\": [1, [1, 2, 3]]}", null));
    assertEquals(true, jsonLogic.apply("{\"in\": [4.56, [1, 2, 3, 4.56]]}", null));
    assertEquals(true, jsonLogic.apply("{\"in\": [null, [1, 2, 3, null]]}", null));
  }

  @Test
  public void testArrayNotIn() {
    assertEquals(false, jsonLogic.apply("{\"in\": [5, [1, 2, 3]]}", null));
    assertEquals(false, jsonLogic.apply("{\"in\": [null, [1, 2, 3]]}", null));
  }

  @Test
  public void testInVariableInt() {
    Map data = Collections.singletonMap("list", Arrays.asList(1, 2, 3));
    assertEquals(true, jsonLogic.apply("{\"in\": [2, {\"var\": \"list\"}]}", data));
  }

  @Test
  public void testNotInVariableInt() {
    Map data = Collections.singletonMap("list", Arrays.asList(1, 2, 3));
    assertEquals(false, jsonLogic.apply("{\"in\": [4, {\"var\": \"list\"}]}", data));
    assertEquals(false, jsonLogic.apply("{\"in\": [4, {\"var\": \"list\"}]}", null));
  }

  @Test
  public void testAllVariables() {
    Map data = Stream.of(new Object[][] {
      new Object[] {"list", Arrays.asList(1, 2, 3)},
      new Object[] {"value", 3}
    }).collect(Collectors.toMap(o -> o[0], o -> o[1]));

    assertEquals(true, jsonLogic.apply("{\"in\": [{\"var\": \"value\"}, {\"var\": \"list\"}]}", data));
    assertEquals(false, jsonLogic.apply("{\"in\": [{\"var\": \"value\"}, {\"var\": \"list\"}]}", null));
  }

  @Test
  public void testSingleArgument() {
    assertFalse((boolean) jsonLogic.apply("{\"in\": [\"Spring\"]}", null));
  }

  @Test
  public void testBadSecondArgument() {
    assertFalse((boolean) jsonLogic.apply("{\"in\": [\"Spring\", 3]}", null));
  }
}
