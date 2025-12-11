package io.veridia.jsonlogic;

import java.util.HashMap;
import java.util.Map;

public class OperatorRegistry {
    private final Map<String, Operator> ops = new HashMap<>();

    public void register(Operator impl) {
        ops.put(impl.key(), impl);
    }

    public Operator get(String name) {
        Operator op = ops.get(name);
        if (op == null) {
            throw new UnsupportedOperationException("Operator not implemented: " + name);
        }
        return op;
    }
}
