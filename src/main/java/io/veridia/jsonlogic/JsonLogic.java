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

public class JsonLogic {
    private final transient ObjectMapper mapper = new ObjectMapper();
    private final transient LogicParser parser = new LogicParser();
    private final transient JsonLogicCompiler compiler = new JsonLogicCompiler();

    private final transient Cache<String, CompiledExpression> compiledExpressionsCache;

    public JsonLogic() {
        this.compiledExpressionsCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build();
    }

    public Object apply(String str, Object ctx) throws JsonProcessingException {
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

    public boolean check(String str, Object ctx) throws JsonProcessingException {
        Object result = apply(str, ctx);

        return ToBoolean.eval(result);
    }
}
