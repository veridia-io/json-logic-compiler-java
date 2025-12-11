package io.veridia.jsonlogic;

import io.veridia.jsonlogic.helpers.ToBoolean;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TruthyTests {
  @Test
  public void testTruthyValues() {
    // Zero
    assertFalse(ToBoolean.eval(0));

    // Any non-zero number
    assertTrue(ToBoolean.eval(1.04));
    assertTrue(ToBoolean.eval(-1));

    // Empty array or collection
    assertFalse(ToBoolean.eval(Collections.EMPTY_LIST));
    assertFalse(ToBoolean.eval(new int[0]));

    // Any non-empty array or collection
    assertTrue(ToBoolean.eval(Collections.singleton(1)));
    assertTrue(ToBoolean.eval(new boolean[] {false}));

    // Empty string
    assertFalse(ToBoolean.eval(""));

    // Any non-empty string
    assertTrue(ToBoolean.eval("hello world"));
    assertTrue(ToBoolean.eval("0"));

    // Null
    assertFalse(ToBoolean.eval(null));

    // NaN and Infinity
    assertFalse(ToBoolean.eval(Double.NaN));
    assertFalse(ToBoolean.eval(Float.NaN));
    assertTrue(ToBoolean.eval(Double.POSITIVE_INFINITY));
    assertTrue(ToBoolean.eval(Double.NEGATIVE_INFINITY));
    assertTrue(ToBoolean.eval(Float.POSITIVE_INFINITY));
    assertTrue(ToBoolean.eval(Float.NEGATIVE_INFINITY));
  }
}
