package io.veridia.jsonlogic;

import tools.jackson.databind.JsonNode;

import java.util.*;

/**
 * Fully JSON-Logic-compliant parser.
 * Distinguishes operator objects from literal objects and supports literal arrays.
 */
public class LogicParser {

    public static class Expr {
        public final String op;         // operator name, or "var", or null (literal)
        public final Object constant;   // literal value (number, string, boolean, list, map)
        public final String variable;   // var path
        public final Object variableDefault;   // var path
        public final List<Expr> args;   // operator args

        private Expr(String op, Object constant, String variable, List<Expr> args, Object variableDefault) {
            this.op = op;
            this.constant = constant;
            this.variable = variable;
            this.args = args;
            this.variableDefault = variableDefault;
        }

        public static Expr constant(Object v) {
            return new Expr(null, v, null, null, null);
        }

        public static Expr variable(String v, Object variableDefault) {
            return new Expr("var", null, v, null, variableDefault);
        }

        public static Expr op(String op, List<Expr> args) {
            return new Expr(op, null, null, args, null);
        }
    }


    /**
     * Parse JSON Logic recursively into Expr.
     */
    public Expr parse(JsonNode node) {
        // --- 1. VALUE NODE (number, string, bool, null) ---
        if (node.isValueNode()) {
            return Expr.constant(
                    node.isNull() ? null :
                            node.isNumber() ? node.numberValue() :
                                    node.isBoolean() ? node.booleanValue() :
                                            node.textValue()
            );
        }

        // --- 2. ARRAY NODE → literal list ---
        if (node.isArray()) {
            List<Object> elements = new ArrayList<>(node.size());
            for (JsonNode el : node) {
                elements.add(exprToLiteral(parse(el)));
            }
            return Expr.constant(elements);
        }

        // --- 3. OBJECT NODE: may be operator OR literal ---
        if (node.isObject()) {
            // Collect all fields (JSON Logic operator objects must have exactly 1 key)
            Iterator<Map.Entry<String, JsonNode>> it = node.properties().iterator();
            List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
            it.forEachRemaining(fields::add);

            // LITERAL OBJECT: if not exactly ONE field → treat as map constant
            if (fields.size() != 1) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (var f : fields) {
                    map.put(f.getKey(), exprToLiteral(parse(f.getValue())));
                }
                return Expr.constant(map);
            }

            // Now we have exactly 1 field → MAY be operator
            var field = fields.get(0);
            String op = field.getKey();
            JsonNode value = field.getValue();

            // Special case: var operator
            if ("var".equals(op)) {
                // {"var":"a.b"} or {"var":["a.b", default]}
                if (value.isArray() && !value.isEmpty()) {
                    return Expr.variable(value.get(0).asText(), value.size() > 1 ? parse(value.get(1)).constant : null);
                }
                return Expr.variable(value.asText(), null);
            }

            // If value is array → operator with multiple args
            if (value.isArray()) {
                List<Expr> args = new ArrayList<>(value.size());
                for (JsonNode child : value) {
                    args.add(parse(child));
                }
                return Expr.op(op, args);
            }

            // Otherwise → single argument operator
            return Expr.op(op, Collections.singletonList(parse(value)));
        }

        throw new IllegalArgumentException("Invalid JSON Logic node: " + node);
    }


    /**
     * Converts parsed Expr into literal Java value for literal object fields.
     * Only for literal constant maps.
     */
    private Object exprToLiteral(Expr expr) {
        if (expr.op == null) return expr.constant; // already literal

        // Should not happen for literal objects, but if something slips through
        if ("var".equals(expr.op)) return expr.variable;

        if (expr.args != null) {
            // Convert nested literal arrays/maps if needed
            List<Object> list = new ArrayList<>(expr.args.size());
            for (Expr e : expr.args) {
                list.add(exprToLiteral(e));
            }
            return list;
        }

        return expr.constant;
    }
}
