package io.veridia.jsonlogic;

import java.util.List;

public interface Operator {
    String key();
    CompiledExpression compile(List<CompiledExpression> args);
}
