package io.veridia.jsonlogic;

@FunctionalInterface
public interface CompiledExpression {
    Object eval(Object ctx);
}
