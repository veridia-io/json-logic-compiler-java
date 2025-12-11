package io.veridia.jsonlogic.helpers;

public class ToDouble {
    public static double eval(Object o) {
        if (o == null) return 0;

        if (o instanceof Boolean) return ((Boolean) o) ? 1 : 0;

        if (o instanceof Number) return ((Number) o).doubleValue();

        if (o instanceof String) {
            String s = (String) o;
            try {
                if (s.trim().isEmpty()) {
                    s = "0";
                }

                return Double.parseDouble(s);
            }
            catch (NumberFormatException e) {
                return 0;
            }
        }

        return 0;
    }
}
