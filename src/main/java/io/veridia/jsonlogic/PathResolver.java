package io.veridia.jsonlogic;

import java.util.List;
import java.util.Map;

public class PathResolver {

    @SuppressWarnings("unchecked")
    public static Object resolve(Object root, String path) {
        if (root == null) return null;
        if (path == null || path.isEmpty()) return root;

        String[] parts = path.split("\\.");

        Object current = root;

        for (String part : parts) {
            if (current == null) return null;

            // Map access
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
                continue;
            }

            // List access (array index)
            if (current instanceof List) {
                try {
                    int index = Integer.parseInt(part);
                    List<?> list = (List<?>) current;
                    if (index < 0 || index >= list.size()) return null;
                    current = list.get(index);
                } catch (NumberFormatException e) {
                    return null; // invalid index
                }
                continue;
            }

            if (current.getClass().isArray()) {
                try {
                    int index = Integer.parseInt(part);
                    Object[] list = (Object[]) current;
                    if (index < 0 || index >= list.length) return null;
                    current = list[index];
                } catch (NumberFormatException e) {
                    return null; // invalid index
                }
                continue;
            }

            // Nothing else supports deep navigation
            return null;
        }

        return current;
    }
}

