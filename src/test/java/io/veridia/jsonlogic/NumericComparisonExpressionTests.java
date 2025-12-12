package io.veridia.jsonlogic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NumericComparisonExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testLessThan() {
    String json = "{\"<\" : [1, 2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testLessThanOrEqual() {
    String json = "{\"<=\" : [1, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testGreaterThan() {
    String json = "{\">\" : [2, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testGreaterThanOrEqual() {
    String json = "{\">=\" : [1, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testBetweenExclusive() {
    String json = "{\"<\" : [1, 2, 3]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testBetweenInclusive() {
    String json = "{\"<=\" : [1, 1, 3]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testGtBetweenExclusive() {
    String json = "{\">\" : [3, 2, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testGtBetweenInclusive() {
    String json = "{\">=\" : [3, 1, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testEdgeCases() {
    assertEquals(true, jsonLogic.apply("{\">=\" : [3, 1, 1, 1]}", null));
    assertEquals(false, jsonLogic.apply("{\">=\" : [3, 1, 3, 1]}", null));
  }
}
