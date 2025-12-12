package io.veridia.jsonlogic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MathExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testAdd() {
    String json = "{\"+\":[4,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(6.0, result);
  }

  @Test
  public void testMultiAdd() {
    String json = "{\"+\":[2,2,2,2,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(10.0, result);
  }

  @Test
  public void testStringAdd() {
    assertEquals(1.0, jsonLogic.apply("{\"+\" : [1, \"foo\"]}", null));
  }

  @Test
  public void testSubtract() {
    String json = "{\"-\":[4,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(2.0, result);
  }

  @Test
  public void testSingleSubtract() {
    String json = "{\"-\": 2 }";
    Object result = jsonLogic.apply(json, null);

    assertEquals(-2.0, result);
  }

  @Test
  public void testSingleSubtractString() {
    String json = "{\"-\": \"2\" }";
    Object result = jsonLogic.apply(json, null);

    assertEquals(-2.0, result);
  }

  @Test
  public void testMultiply() {
    String json = "{\"*\":[4,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(8.0, result);
  }

  @Test
  public void testMultiMultiply() {
    String json = "{\"*\":[2,2,2,2,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(32.0, result);
  }

  @Test
  public void testMultiplyWithArray() {
    String json = "{\"*\":[2,[[3, 4], 5]]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(0.0, result);  // This matches reference impl at jsonlogic.com
  }

  @Test
  public void testMultiplyWithEmptyArray() {
    String json = "{\"*\":[2,[]]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(0.0, result);  // This matches reference impl at jsonlogic.com
  }

  @Test
  public void testDivide() {
    String json = "{\"/\":[4,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(2.0, result);
  }

  @Test
  public void testDivideBy0() {
    String json = "{\"/\":[4,0]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(0.0, result);
  }

  @Test
  public void testModulo() {
    String json = "{\"%\": [101,2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(1.0, result);
  }

  @Test
  public void testMin() {
    String json = "{\"min\":[1,2,3]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(1.0, result);
  }

  @Test
  public void testMax() {
    String json = "{\"max\":[1,2,3]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(3.0, result);
  }

  @Test
  public void testDivideSingleNumber() {
    assertEquals(null, jsonLogic.apply("{\"/\": [0]}", null));
  }
}
