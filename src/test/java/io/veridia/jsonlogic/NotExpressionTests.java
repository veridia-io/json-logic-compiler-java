package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NotExpressionTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testSingleBoolean() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"!\": false}", null));
  }

  @Test
  public void testSingleNumber() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"!\": 0}", null));
  }

  @Test
  public void testSingleString() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"!\": \"\"}", null));
  }

  @Test
  public void testSingleArray() throws JsonProcessingException {
    assertEquals(true, jsonLogic.apply("{\"!\": []}", null));
  }

  @Test
  public void testDoubleBoolean() throws JsonProcessingException {
    assertEquals(false, jsonLogic.apply("{\"!!\": false}", null));
  }

  @Test
  public void testDoubleNumber() throws JsonProcessingException {
    assertEquals(false, jsonLogic.apply("{\"!!\": 0}", null));
  }

  @Test
  public void testDoubleString() throws JsonProcessingException {
    assertEquals(false, jsonLogic.apply("{\"!!\": \"\"}", null));
  }

  @Test
  public void testDoubleArray() throws JsonProcessingException {
    assertEquals(false, jsonLogic.apply("{\"!!\": [[]]}", null));
  }
}
