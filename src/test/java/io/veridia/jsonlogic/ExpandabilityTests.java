package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.veridia.jsonlogic.helpers.ToDouble;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;

public class ExpandabilityTests {
  public static class AssignGroupOperator implements Operator {
    public String key() {
      return "assignGroup";
    }

    @SuppressWarnings("unchecked")
    public CompiledExpression compile(List<CompiledExpression> args) {
      if (args.size() != 2) return ctx -> 1L;

      CompiledExpression groupsCountExpr = args.get(0);
      CompiledExpression experimentNameExpr = args.get(1);

      try {
        long groupsCount = (long) ToDouble.eval(groupsCountExpr.eval(Collections.emptyMap()));
        String experimentName = (String) experimentNameExpr.eval(Collections.emptyMap());

        if (groupsCount < 1) return ctx -> 1L;

        return ctx -> {
          try {
            Map<String, Object> ctxMap = (Map<String, Object>) ((Map<?, ?>) ctx).get("context");
            String canonicalId = ctxMap.get("canonicalId").toString();

            return crc32mod(canonicalId, experimentName, groupsCount) + 1L;
          } catch (Exception e) {
            return 1L;
          }
        };
      } catch (Exception e) {
        return ctx -> 1L;
      }
    }

    private long crc32mod(String input, String nonce, long mod) {
      CRC32 crc = new CRC32();
      crc.update(input.getBytes(StandardCharsets.UTF_8));
      crc.update(nonce.getBytes(StandardCharsets.UTF_8));
      return crc.getValue() % mod;
    }
  }

  private static final JsonLogic jsonLogic = new JsonLogic().registerOperator(new AssignGroupOperator());

  @Test
  public void testAssignGroupEmpty() throws JsonProcessingException {
      assertEquals(1L, jsonLogic.apply("{\"assignGroup\": []}", Map.of("context", Map.of("canonicalId", "1234"))));
  }

  @Test
  public void testAssignGroup1() throws JsonProcessingException {
    assertEquals(1L, jsonLogic.apply("{\"assignGroup\": [1]}", Map.of("context", Map.of("canonicalId", "1234"))));
  }

  @Test
  public void testAssignGroup1_2() throws JsonProcessingException {
    assertEquals(1L, jsonLogic.apply("{\"assignGroup\": [1, \"\"]}", Map.of("context", Map.of("canonicalId", "1234"))));
  }

  @Test
  public void testAssignGroup2() throws JsonProcessingException {
    assertEquals(1L, jsonLogic.apply("{\"assignGroup\": [2, \"new1\"]}", Map.of("context", Map.of("canonicalId", "1234"))));
    assertEquals(2L, jsonLogic.apply("{\"assignGroup\": [2, \"new1\"]}", Map.of("context", Map.of("canonicalId", "12345"))));
    assertEquals(1L, jsonLogic.apply("{\"assignGroup\": [2, \"new4\"]}", Map.of("context", Map.of("canonicalId", "12345"))));
    assertEquals(1L, jsonLogic.apply("{\"assignGroup\": [2, 3]}", Map.of("context", Map.of("canonicalId", "12345"))));
  }
}
