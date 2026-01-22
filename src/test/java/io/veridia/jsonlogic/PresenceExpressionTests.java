package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class PresenceExpressionTests {

    private static final JsonLogic jsonLogic = new JsonLogic();

    // ------------------------
    // exists
    // ------------------------

    @Test
    public void testExistsTrue() throws JsonProcessingException {
        String json = "{ \"exists\": { \"var\": \"a\" } }";
        Object result = jsonLogic.apply(json, Map.of("a", 1));

        assertEquals(true, result);
    }

    @Test
    public void testExistsFalseMissing() throws JsonProcessingException {
        String json = "{ \"exists\": { \"var\": \"a\" } }";
        Object result = jsonLogic.apply(json, Map.of());

        assertEquals(false, result);
    }

    @Test
    public void testExistsFalseNull() throws JsonProcessingException {
        String json = "{ \"exists\": { \"var\": \"a\" } }";

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("a", null);

        Object result = jsonLogic.apply(json, ctx);

        assertEquals(false, result);
    }

    @Test
    public void testExistsNestedVar() throws JsonProcessingException {
        String json = "{ \"exists\": { \"var\": \"request.referrer.host\" } }";

        Object ctx = Map.of(
                "request", Map.of(
                        "referrer", Map.of(
                                "host", "google.com"
                        )
                )
        );

        Object result = jsonLogic.apply(json, ctx);

        assertEquals(true, result);
    }

    @Test
    public void testExistsNestedVarMissingIntermediate() throws JsonProcessingException {
        String json = "{ \"exists\": { \"var\": \"request.referrer.host\" } }";

        Object ctx = Map.of(
                "request", Map.of()
        );

        Object result = jsonLogic.apply(json, ctx);

        assertEquals(false, result);
    }

    @Test
    public void testExistsNestedVarNullValue() throws JsonProcessingException {
        String json = "{ \"exists\": { \"var\": \"request.referrer.host\" } }";

        Map<String, Object> referrer = new HashMap<>();
        referrer.put("host", null);

        Map<String, Object> request = new HashMap<>();
        request.put("referrer", referrer);

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("request", request);

        Object result = jsonLogic.apply(json, ctx);

        assertEquals(false, result);
    }

    // ------------------------
    // not_exists
    // ------------------------

    @Test
    public void testNotExistsTrue() throws JsonProcessingException {
        String json = "{ \"not_exists\": { \"var\": \"a\" } }";
        Object result = jsonLogic.apply(json, Map.of());

        assertEquals(true, result);
    }

    @Test
    public void testNotExistsFalse() throws JsonProcessingException {
        String json = "{ \"not_exists\": { \"var\": \"a\" } }";
        Object result = jsonLogic.apply(json, Map.of("a", 123));

        assertEquals(false, result);
    }

    // ------------------------
    // edge cases (important)
    // ------------------------

    @Test
    public void testExistsEmptyString() throws JsonProcessingException {
        String json = "{ \"exists\": { \"var\": \"a\" } }";
        Object result = jsonLogic.apply(json, Map.of("a", ""));

        assertEquals(true, result);
    }

    @Test
    public void testExistsEmptyArray() throws JsonProcessingException {
        String json = "{ \"exists\": { \"var\": \"a\" } }";
        Object result = jsonLogic.apply(json, Map.of("a", new Object[0]));

        assertEquals(true, result);
    }
}
