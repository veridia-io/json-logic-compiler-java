package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArrayHasExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testSomeWithNull() throws JsonProcessingException {
    assertEquals(false, jsonLogic.apply("{\"and\":[{\"some\":[{\"var\":\"fruits\"},{\"in\":[{\"var\":\"item\"},[\"apple\"]]}]}]}", "{\"fruits\":null}"));
  }

  @Test
  public void testSomeEmptyArray() throws JsonProcessingException {
    assertEquals(false, jsonLogic.apply("{\"some\": [[], {\">\": [{\"var\": \"item\"}, 0]}]}", null));
  }

  @Test
  public void testSomeAll() throws JsonProcessingException {
    assertEquals(false, jsonLogic.apply("{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 3]}]}", null));
    assertEquals(true, jsonLogic.apply("{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 1]}]}", null));
  }

  @Test
  public void testNoneWithNull() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"and\":[{\"none\":[{\"var\":\"fruits\"},{\"in\":[{\"var\":\"item\"},[\"apple\"]]}]}]}", "{\"fruits\":null}"));
  }

  @Test
  public void testNoneEmptyArray() throws JsonProcessingException {
    assertEquals(false, jsonLogic.apply("{\"some\": [[], {\">\": [{\"var\": \"item\"}, 0]}]}", null));
  }

  @Test
  public void testNoneAll() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"none\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 3]}]}", null));
    assertEquals(false, jsonLogic.apply("{\"none\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 2]}]}", null));
  }
}