package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MathExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testAdd() throws JsonProcessingException {
    String json = "{\"+\":[4,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(6.0, result);
  }

  @Test
  public void testMultiAdd() throws JsonProcessingException {
    String json = "{\"+\":[2,2,2,2,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(10.0, result);
  }

  @Test
  public void testStringAdd() throws JsonProcessingException {
    assertEquals(1.0, jsonLogic.apply("{\"+\" : [1, \"foo\"]}", null));
  }

  @Test
  public void testSubtract() throws JsonProcessingException {
    String json = "{\"-\":[4,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(2.0, result);
  }

  @Test
  public void testSingleSubtract() throws JsonProcessingException {
    String json = "{\"-\": 2 }";
    Object result = jsonLogic.apply(json, null);

    assertEquals(-2.0, result);
  }

  @Test
  public void testSingleSubtractString() throws JsonProcessingException {
    String json = "{\"-\": \"2\" }";
    Object result = jsonLogic.apply(json, null);

    assertEquals(-2.0, result);
  }

  @Test
  public void testMultiply() throws JsonProcessingException {
    String json = "{\"*\":[4,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(8.0, result);
  }

  @Test
  public void testMultiMultiply() throws JsonProcessingException {
    String json = "{\"*\":[2,2,2,2,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(32.0, result);
  }

  @Test
  public void testMultiplyWithArray() throws JsonProcessingException {
    String json = "{\"*\":[2,[[3, 4], 5]]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(0.0, result);  // This matches reference impl at jsonlogic.com
  }

  @Test
  public void testMultiplyWithEmptyArray() throws JsonProcessingException {
    String json = "{\"*\":[2,[]]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(0.0, result);  // This matches reference impl at jsonlogic.com
  }

  @Test
  public void testDivide() throws JsonProcessingException {
    String json = "{\"/\":[4,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(2.0, result);
  }

  @Test
  public void testDivideBy0() throws JsonProcessingException {
    String json = "{\"/\":[4,0]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(0.0, result);
  }

  @Test
  public void testModulo() throws JsonProcessingException {
    String json = "{\"%\": [101,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(1.0, result);
  }

  @Test
  public void testMin() throws JsonProcessingException {
    String json = "{\"min\":[1,2,3]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(1.0, result);
  }

  @Test
  public void testMax() throws JsonProcessingException {
    String json = "{\"max\":[1,2,3]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(3.0, result);
  }

  @Test
  public void testDivideSingleNumber() throws JsonProcessingException {
    assertEquals(null, jsonLogic.apply("{\"/\": [0]}", null));
  }
}
