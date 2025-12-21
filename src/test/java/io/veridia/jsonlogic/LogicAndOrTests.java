package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LogicAndOrTests {

    private static final JsonLogic jsonLogic = new JsonLogic();

    //
    // AND TESTS
    //

    @Test
    public void testAnd_AllTrue() throws JsonProcessingException {
        assertEquals(true,
                jsonLogic.apply("{\"and\": [true, 1, \"nonempty\"]}", null));
    }

    @Test
    public void testAnd_FirstFalse() throws JsonProcessingException {
        assertEquals(false,
                jsonLogic.apply("{\"and\": [false, true, true]}", null));
    }

    @Test
    public void testAnd_MiddleFalse() throws JsonProcessingException {
        assertEquals(false,
                jsonLogic.apply("{\"and\": [true, false, true]}", null));
    }

    @Test
    public void testAnd_LastFalse() throws JsonProcessingException {
        // This is the test your compiler FAILS right now.
        assertEquals(false,
                jsonLogic.apply("{\"and\": [true, true, false]}", null));
    }

    //
    // OR TESTS
    //

    @Test
    public void testOr_FirstTruthy() throws JsonProcessingException {
        assertEquals(true,
                jsonLogic.apply("{\"or\": [true, false, false]}", null));
    }

    @Test
    public void testOr_AllFalsy() throws JsonProcessingException {
        assertEquals(false,
                jsonLogic.apply("{\"or\": [0, null, \"\"]}", null));
    }

    //
    // MIXED
    //

    @Test
    public void testAndOr_Mixed1() throws JsonProcessingException {
        assertEquals(true,
                jsonLogic.apply("{\"and\": [ true, {\"or\": [false, true]} ]}", null));
    }

    @Test
    public void testAndOr_Mixed2() throws JsonProcessingException {
        assertEquals(false,
                jsonLogic.apply("{\"and\": [ true, {\"or\": [false, false]} ]}", null));
    }

    @Test
    public void testAndOr_Mixed3() throws JsonProcessingException {
        assertEquals(true,
                jsonLogic.apply("{\"or\": [ false, {\"and\": [true, true]} ]}", null));
    }

    //
    // REAL BUG: test that exposed your AND issue
    //

    @Test
    public void testRealScenario2_ANDLastCondition() throws JsonProcessingException {
        // Your compiled engine incorrectly returns TRUE here.
        String json = "{\"and\":[true, true, false]}";
        assertEquals(false, jsonLogic.apply(json, null));
    }
}
