package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import io.veridia.jsonlogic.helpers.ToBoolean;

import static java.nio.charset.StandardCharsets.UTF_8;

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

        String key = Hashing.murmur3_128().hashString(str, UTF_8).toString();
        CompiledExpression expr = compiledExpressionsCache.getIfPresent(key);

        if (expr == null) {
            JsonNode json = mapper.readTree(str);


            expr = compiler.compile(parser.parse(json));
            compiledExpressionsCache.put(key, expr);
        }

        Object context = ctx == null ? null : ctx instanceof String ? mapper.readValue((String) ctx, new TypeReference<>() {}) : ctx;

        return expr.eval(context);
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
