package io.veridia.jsonlogic.operators;

import com.google.re2j.Pattern;
import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class StringOperator implements Operator {
    public static final StringOperator CONTAINS =
            new StringOperator("contains", String::contains);
    public static final StringOperator STARTS_WITH =
            new StringOperator("starts_with", String::startsWith);
    public static final StringOperator ENDS_WITH =
            new StringOperator("ends_with", String::endsWith);
    public static final StringOperator REGEX_MATCH =
            new StringOperator("regex_match", StringOperator::regexMatch);

    private static final int MAX_PATTERN_LENGTH = 256;
    private static final int MAX_INPUT_LENGTH = 1024;

    private final String op;
    private final BiFunction<String, String, Boolean> reducer;

    StringOperator(String op, BiFunction<String, String, Boolean> reducer) {
        this.op = op;
        this.reducer = reducer;
    }

    public String key() {
        return op;
    }

    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() != 2)
            return ctx -> false;

        return ctx -> {
            Object left = args.get(0).eval(ctx);
            Object right = args.get(1).eval(ctx);

            if (left == null || right == null) {
                return false;
            }

            String leftStr = Objects.toString(left, null);
            String rightStr = Objects.toString(right, null);

            if (leftStr == null || rightStr == null) {
                return false;
            }

            return this.reducer.apply(leftStr, rightStr);
        };
    }

    private static boolean regexMatch(String input, String pattern) {
        if (pattern.length() > MAX_PATTERN_LENGTH) {
            return false;
        }

        if (input.length() > MAX_INPUT_LENGTH) {
            return false;
        }

        try {
            Pattern compiled = Pattern.compile(pattern);
            return compiled.matcher(input).find();
        } catch (Exception e) {
            // Invalid regex → fail closed
            return false;
        }
    }
}
