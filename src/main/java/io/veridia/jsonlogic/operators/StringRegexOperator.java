package io.veridia.jsonlogic.operators;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;

import java.util.List;
import java.util.Objects;

public class StringRegexOperator implements Operator {
    public static final StringRegexOperator INSTANCE = new StringRegexOperator();

    private static final int MAX_PATTERN_LENGTH = 256;
    private static final int MAX_INPUT_LENGTH = 1024;

    private final transient Cache<String, Pattern> compiledPatternsCache;

    StringRegexOperator() {
        this.compiledPatternsCache = CacheBuilder.newBuilder()
                .maximumSize(1024)
                .build();
    }

    @Override
    public String key() {
        return "regex_match";
    }

    @Override
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

            return this.regexMatch(leftStr, rightStr);
        };
    }

    private boolean regexMatch(String input, String pattern) {
        if (pattern.length() > MAX_PATTERN_LENGTH) {
            return false;
        }

        if (input.length() > MAX_INPUT_LENGTH) {
            return false;
        }

        try {
            Pattern compiled = compiledPatternsCache.getIfPresent(pattern);
            if (compiled == null) {
                compiled = Pattern.compile(pattern);

                compiledPatternsCache.put(pattern, compiled);
            }

            return compiled.matcher(input).find();
        } catch (PatternSyntaxException e) {
            // Invalid regex → fail closed
            return false;
        }
    }
}
