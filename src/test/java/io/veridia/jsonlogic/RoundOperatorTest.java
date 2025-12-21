package io.veridia.jsonlogic;

import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.assertEquals;

public class RoundOperatorTest {

    private final JsonLogic json = new JsonLogic();

    @Test
    public void testRoundToInteger() throws Exception {
        assertEquals(6.0,
                json.apply("{\"round\": [5.6, 0]}", null));
    }

    @Test
    public void testRoundToTwoDecimals() throws Exception {
        assertEquals(5.68,
                json.apply("{\"round\": [5.6789, 2]}", null));
    }

    @Test
    public void testRoundToThreeDecimals() throws Exception {
        assertEquals(3.142,
                json.apply("{\"round\": [3.14159, 3]}", null));
    }

    @Test
    public void testRoundNegativePrecision() throws Exception {
        assertEquals(10.0,
                json.apply("{\"round\": [12.34, -1]}", null));
        assertEquals(100.0,
                json.apply("{\"round\": [123.45, -2]}", null));
    }

    @Test
    public void testRoundVar() throws Exception {
        Map<String,Object> ctx = Map.of("amount", 7.3339);
        assertEquals(7.33,
                json.apply("{\"round\": [ {\"var\":\"amount\"}, 2 ]}", ctx));
    }

    @Test
    public void testRoundNull() throws Exception {
        assertEquals(0.0,
                json.apply("{\"round\": [null, 2]}", null));
    }

    @Test
    public void testRoundInvalidPrecision() throws Exception {
        assertEquals(5.0, json.apply("{\"round\": [5.123, \"bad\"]}", null));
    }
}
