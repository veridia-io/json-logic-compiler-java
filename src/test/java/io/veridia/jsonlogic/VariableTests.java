package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class VariableTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testEmptyString() throws JsonProcessingException {
    assertEquals(3.14, jsonLogic.apply("{\"var\": \"\"}", 3.14));
  }

  @Test
  public void testMapAccess() throws JsonProcessingException {
    Map<String, Double> data = new HashMap<String, Double>() {{
      put("pi", 3.14);
    }};

    assertEquals(3.14, jsonLogic.apply("{\"var\": \"pi\"}", data));
  }

  @Test
  public void testDefaultValue() throws JsonProcessingException {
    assertEquals(3.14, jsonLogic.apply("{\"var\": [\"pi\", 3.14]}", null));
  }

  @Test
  public void testUndefined() throws JsonProcessingException {
    assertNull(jsonLogic.apply("{\"var\": [\"pi\"]}", null));
    assertNull(jsonLogic.apply("{\"var\": \"\"}", null));
    assertNull(jsonLogic.apply("{\"var\": 0}", null));
  }

  @Test
  public void testArrayAccess() throws JsonProcessingException {
    String[] data = new String[] {"hello", "world"};

    assertEquals("hello", jsonLogic.apply("{\"var\": 0}", data));
    assertEquals("world", jsonLogic.apply("{\"var\": 1}", data));
    assertNull(jsonLogic.apply("{\"var\": 2}", data));
    assertNull(jsonLogic.apply("{\"var\": 3}", data));
  }

  @Test
  public void testArrayAccessWithStringKeys() throws JsonProcessingException {
    String[] data = new String[] {"hello", "world"};

    assertEquals("hello", jsonLogic.apply("{\"var\": \"0\"}", data));
    assertEquals("world", jsonLogic.apply("{\"var\": \"1\"}", data));
    assertNull(jsonLogic.apply("{\"var\": \"2\"}", data));
    assertNull(jsonLogic.apply("{\"var\": \"3\"}", data));
  }

  @Test
  public void testListAccess() throws JsonProcessingException {
    List<String> data = Arrays.asList("hello", "world");

    assertEquals("hello", jsonLogic.apply("{\"var\": 0}", data));
    assertEquals("world", jsonLogic.apply("{\"var\": 1}", data));
    assertNull(jsonLogic.apply("{\"var\": 2}", data));
    assertNull(jsonLogic.apply("{\"var\": 3}", data));
  }

  @Test
  public void testListAccessWithStringKeys() throws JsonProcessingException {
    List<String> data = Arrays.asList("hello", "world");

    assertEquals("hello", jsonLogic.apply("{\"var\": \"0\"}", data));
    assertEquals("world", jsonLogic.apply("{\"var\": \"1\"}", data));
    assertNull(jsonLogic.apply("{\"var\": \"2\"}", data));
    assertNull(jsonLogic.apply("{\"var\": \"3\"}", data));
  }

  @Test
  public void testComplexAccess() throws JsonProcessingException {
    Map<String, Object> data = new HashMap<String, Object>() {{
      put("users", Arrays.asList(
        new HashMap<String, Object>() {{
          put("name", "John");
          put("followers", 1337);
        }},
        new HashMap<String, Object>() {{
          put("name", "Jane");
          put("followers", 2048);
        }}
      ));
    }};

    assertEquals("John", jsonLogic.apply("{\"var\": \"users.0.name\"}", data));
    assertEquals(1337, jsonLogic.apply("{\"var\": \"users.0.followers\"}", data));
    assertEquals("Jane", jsonLogic.apply("{\"var\": \"users.1.name\"}", data));
    assertEquals(2048, jsonLogic.apply("{\"var\": \"users.1.followers\"}", data));
  }

  @Test
  public void missingNestedMapKey_returnsDefault() throws JsonProcessingException {
    // data.a.b is missing -> should use default
    String rule = "{\"var\": [\"a.b.c\", \"fallback\"]}";
    Map<String, Object> data = map("a", map("b", new HashMap<>()));

    Object result = jsonLogic.apply(rule, data);

    assertEquals("fallback", result);
  }

  @Test
  public void arrayIndexWithinBounds_returnsElement_asDoubleForNumbers() throws JsonProcessingException {
    // items.1 exists -> should return 20 (as a double per evaluator.transform)
    String rule = "{\"var\": [\"items.1\", 999]}";
    Map<String, Object> data = map("items", Arrays.asList(10, 20));

    Object result = jsonLogic.apply(rule, data);

    assertTrue(result instanceof Number);
    assertEquals(20.0, ((Number) result).doubleValue(), 0.0);
  }

  @Test
  public void arrayIndexOutOfBounds_returnsDefault() throws JsonProcessingException {
    // items.2 missing -> use default
    String rule = "{\"var\": [\"items.2\", \"missing\"]}";
    Map<String, Object> data = map("items", Arrays.asList(10, 20));

    Object result = jsonLogic.apply(rule, data);

    assertEquals("missing", result);
  }

  @Test
  public void topLevelNumericIndex_overList_works() throws JsonProcessingException {
    // {"var": [1, "missing"]} over a top-level list -> "banana"
    String rule = "{\"var\": [1, \"missing\"]}";
    List<String> data = Arrays.asList("apple", "banana", "carrot");

    Object result = jsonLogic.apply(rule, data);

    assertEquals("banana", result);
  }

  @Test
  public void emptyVarKey_returnsWholeDataObject() throws JsonProcessingException {
    // {"var": ""} should return the entire data object (same instance)
    String rule = "{\"var\": \"\"}";
    Map<String, Object> data = map("x", 1);

    Object result = jsonLogic.apply(rule, data);

    assertSame("Should return the same data instance", data, result);
  }

  /** Helper to make small maps concisely. */
  private static Map<String, Object> map(Object... kv) {
    Map<String, Object> m = new HashMap<>();
    for (int i = 0; i < kv.length; i += 2) {
      m.put((String) kv[i], kv[i + 1]);
    }
    return m;
  }
}
