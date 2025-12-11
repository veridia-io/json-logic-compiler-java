package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NumericComparisonExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testLessThan() throws JsonProcessingException {
    String json = "{\"<\" : [1, 2]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testLessThanOrEqual() throws JsonProcessingException {
    String json = "{\"<=\" : [1, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testGreaterThan() throws JsonProcessingException {
    String json = "{\">\" : [2, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testGreaterThanOrEqual() throws JsonProcessingException {
    String json = "{\">=\" : [1, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testBetweenExclusive() throws JsonProcessingException {
    String json = "{\"<\" : [1, 2, 3]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testBetweenInclusive() throws JsonProcessingException {
    String json = "{\"<=\" : [1, 1, 3]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testGtBetweenExclusive() throws JsonProcessingException {
    String json = "{\">\" : [3, 2, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testGtBetweenInclusive() throws JsonProcessingException {
    String json = "{\">=\" : [3, 1, 1]}";
    Object result = jsonLogic.apply(json, null);

    assertEquals(true, result);
  }

  @Test
  public void testEdgeCases() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\">=\" : [3, 1, 1, 1]}", null));
    assertEquals(false, jsonLogic.apply("{\">=\" : [3, 1, 3, 1]}", null));
  }
}
