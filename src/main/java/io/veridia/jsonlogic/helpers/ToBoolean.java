package io.veridia.jsonlogic.helpers;

import java.lang.reflect.Array;
import java.util.Collection;

public class ToBoolean {
    public static boolean eval(Object value) {
        if (value == null) return false;

        if (value instanceof Boolean) return (Boolean) value;

        if (value instanceof Number) {
            double doubleValue = ((Number) value).doubleValue();

            return !Double.isNaN(doubleValue) && doubleValue != 0;
        }

        if (value instanceof String) return !((String) value).isEmpty();

        if (value instanceof Collection) return !((Collection<?>) value).isEmpty();

        if (value.getClass().isArray())return Array.getLength(value) > 0;

        return true;
    }
}
