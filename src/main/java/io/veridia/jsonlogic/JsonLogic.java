package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.veridia.jsonlogic.helpers.ToBoolean;

/**
 * Entry point for evaluating JSON Logic expressions.
 * <p>
 * Instances manage parsing, compilation, and a small cache of compiled expressions.
 */
public class JsonLogic {
    private final transient ObjectMapper mapper = new ObjectMapper();
    private final transient LogicParser parser = new LogicParser();
    private final transient JsonLogicCompiler compiler = new JsonLogicCompiler();

    private final transient Cache<String, CompiledExpression> compiledExpressionsCache;

    /**
     * Creates a new {@code JsonLogic} instance with a default compiled expression cache.
     */
    public JsonLogic() {
        this.compiledExpressionsCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build();
    }

    /**
     * Applies a JSON Logic expression against the provided context.
     *
     * @param str JSON Logic expression as a string
     * @param ctx context object or JSON string to use when evaluating the expression
     * @return the raw evaluation result, or {@code null} when the expression is blank
     * @throws JsonProcessingException when the expression or context cannot be parsed
     */
    public Object apply(String str, Object ctx) throws JsonProcessingException {
        if (str == null || str.isBlank())
            return null;

        CompiledExpression expr = compile(str);

        Object context = ctx == null ? null : ctx instanceof String ? mapper.readValue((String) ctx, new TypeReference<>() {}) : ctx;

        return expr.eval(context);
    }

    /**
     * Parses and compiles a JSON Logic expression, returning a reusable {@link CompiledExpression}.
     * <p>
     * Compilation results are cached, so repeated calls with the same expression string are cheap.
     * Callers on hot paths should compile once and invoke {@link CompiledExpression#eval(Object)}
     * directly against pre-parsed contexts to avoid all per-evaluation parsing and cache lookups.
     *
     * @param str JSON Logic expression as a string
     * @return the compiled expression (a no-op returning {@code null} for a blank input)
     * @throws JsonProcessingException when the expression cannot be parsed
     */
    public CompiledExpression compile(String str) throws JsonProcessingException {
        if (str == null || str.isBlank())
            return ctx -> null;

        // The expression string is the cache key directly: String.hashCode is cached by the JVM,
        // so lookups no longer pay for hashing the full expression on every call.
        CompiledExpression expr = compiledExpressionsCache.getIfPresent(str);

        if (expr == null) {
            JsonNode json = mapper.readTree(str);
            expr = compiler.compile(parser.parse(json));
            compiledExpressionsCache.put(str, expr);
        }

        return expr;
    }

    /**
     * Applies a JSON Logic expression and coerces the result to a boolean.
     *
     * @param str JSON Logic expression as a string
     * @param ctx context object or JSON string to use when evaluating the expression
     * @return boolean interpretation of the evaluation result
     * @throws JsonProcessingException when the expression or context cannot be parsed
     */
    public boolean check(String str, Object ctx) throws JsonProcessingException {
        Object result = apply(str, ctx);

        return ToBoolean.eval(result);
    }

    /**
     * Registers a custom operator implementation for use during compilation.
     *
     * @param impl operator implementation to register
     * @return this instance for chaining
     */
    public JsonLogic registerOperator(Operator impl) {
        compiler.registerOperator(impl);

        return this;
    }
}
