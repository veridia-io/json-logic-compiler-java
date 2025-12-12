package io.veridia.jsonlogic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NotExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testSingleBoolean() {
    assertEquals(true, jsonLogic.apply("{\"!\": false}", null));
  }

  @Test
  public void testSingleNumber() {
    assertEquals(true, jsonLogic.apply("{\"!\": 0}", null));
  }

  @Test
  public void testSingleString() {
    assertEquals(true, jsonLogic.apply("{\"!\": \"\"}", null));
  }

  @Test
  public void testSingleArray() {
    assertEquals(true, jsonLogic.apply("{\"!\": []}", null));
  }

  @Test
  public void testDoubleBoolean() {
    assertEquals(false, jsonLogic.apply("{\"!!\": false}", null));
  }

  @Test
  public void testDoubleNumber() {
    assertEquals(false, jsonLogic.apply("{\"!!\": 0}", null));
  }

  @Test
  public void testDoubleString() {
    assertEquals(false, jsonLogic.apply("{\"!!\": \"\"}", null));
  }

  @Test
  public void testDoubleArray() {
    assertEquals(false, jsonLogic.apply("{\"!!\": [[]]}", null));
  }
}
