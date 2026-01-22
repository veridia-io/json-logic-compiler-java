package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringExpressionTests {

    private static final JsonLogic jsonLogic = new JsonLogic();

    // -------------------------------------------------
    // contains
    // -------------------------------------------------

    @Test
    public void testContainsTrue() throws JsonProcessingException {
        String json = "{\"contains\": [\"hello world\", \"world\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(true, result);
    }

    @Test
    public void testContainsFalse() throws JsonProcessingException {
        String json = "{\"contains\": [\"hello\", \"xyz\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(false, result);
    }

    // -------------------------------------------------
    // starts_with
    // -------------------------------------------------

    @Test
    public void testStartsWithTrue() throws JsonProcessingException {
        String json = "{\"starts_with\": [\"abcdef\", \"abc\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(true, result);
    }

    @Test
    public void testStartsWithFalse() throws JsonProcessingException {
        String json = "{\"starts_with\": [\"abcdef\", \"bcd\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(false, result);
    }

    // -------------------------------------------------
    // ends_with
    // -------------------------------------------------

    @Test
    public void testEndsWithTrue() throws JsonProcessingException {
        String json = "{\"ends_with\": [\"abcdef\", \"def\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(true, result);
    }

    @Test
    public void testEndsWithFalse() throws JsonProcessingException {
        String json = "{\"ends_with\": [\"abcdef\", \"abc\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(false, result);
    }

    // -------------------------------------------------
    // regex_match (RE2)
    // -------------------------------------------------

    @Test
    public void testRegexMatchSimple() throws JsonProcessingException {
        String json = "{\"regex_match\": [\"google.com\", \"google\\\\.com\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(true, result);
    }

    @Test
    public void testRegexMatchFindNotFullMatch() throws JsonProcessingException {
        String json = "{\"regex_match\": [\"https://google.com/search\", \"google\\\\.com\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(true, result);
    }

    @Test
    public void testRegexMatchNoMatch() throws JsonProcessingException {
        String json = "{\"regex_match\": [\"example.com\", \"google\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(false, result);
    }

    @Test
    public void testRegexInvalidPattern() throws JsonProcessingException {
        String json = "{\"regex_match\": [\"test\", \"(\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(false, result);
    }

    // -------------------------------------------------
    // safety limits
    // -------------------------------------------------

    @Test
    public void testRegexPatternTooLong() throws JsonProcessingException {
        String longPattern = "a".repeat(300);
        String json = "{\"regex_match\": [\"aaa\", \"" + longPattern + "\"]}";

        Object result = jsonLogic.apply(json, null);
        assertEquals(false, result);
    }

    @Test
    public void testRegexInputTooLong() throws JsonProcessingException {
        String longInput = "a".repeat(2000);
        String json = "{\"regex_match\": [\"" + longInput + "\", \"a+\"]}";

        Object result = jsonLogic.apply(json, null);
        assertEquals(false, result);
    }

    // -------------------------------------------------
    // type coercion & null handling
    // -------------------------------------------------

    @Test
    public void testContainsWithNumber() throws JsonProcessingException {
        String json = "{\"contains\": [12345, \"234\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(true, result);
    }

    @Test
    public void testContainsNullLeft() throws JsonProcessingException {
        String json = "{\"contains\": [null, \"a\"]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(false, result);
    }

    @Test
    public void testContainsNullRight() throws JsonProcessingException {
        String json = "{\"contains\": [\"abc\", null]}";
        Object result = jsonLogic.apply(json, null);

        assertEquals(false, result);
    }
}
