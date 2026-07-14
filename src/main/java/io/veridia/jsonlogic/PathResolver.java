package io.veridia.jsonlogic;

import java.util.List;
import java.util.Map;

public class PathResolver {

    /**
     * Splits a dotted variable path into its segments once, at compile time.
     * Kept out of the eval hot path so callers never pay for regex parsing per evaluation.
     */
    public static String[] split(String path) {
        if (path == null || path.isEmpty()) return new String[0];
        return path.split("\\.");
    }

    /**
     * Resolves a dotted path against a root object.
     * <p>
     * Prefer the {@link #resolve(Object, String[])} overload on hot paths: it accepts an
     * already-split path so no regex splitting happens per evaluation.
     */
    public static Object resolve(Object root, String path) {
        if (root == null) return null;
        if (path == null || path.isEmpty()) return root;
        return resolve(root, split(path));
    }

    /**
     * Resolves a pre-split path against a root object. This is the allocation-free path used
     * by compiled {@code var} nodes.
     */
    @SuppressWarnings("unchecked")
    public static Object resolve(Object root, String[] parts) {
        if (root == null) return null;
        if (parts == null || parts.length == 0) return root;

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
                int index = parseIndex(part);
                if (index < 0) return null;
                List<?> list = (List<?>) current;
                if (index >= list.size()) return null;
                current = list.get(index);
                continue;
            }

            if (current.getClass().isArray()) {
                int index = parseIndex(part);
                if (index < 0) return null;
                Object[] list = (Object[]) current;
                if (index >= list.length) return null;
                current = list[index];
                continue;
            }

            // Nothing else supports deep navigation
            return null;
        }

        return current;
    }

    /**
     * Resolves a single, dot-free key against a root object. Fast path for flat variable access,
     * which is by far the most common {@code var} shape.
     */
    @SuppressWarnings("unchecked")
    public static Object resolveSingle(Object root, String key) {
        if (root == null) return null;

        if (root instanceof Map) {
            return ((Map<String, Object>) root).get(key);
        }

        if (root instanceof List) {
            int index = parseIndex(key);
            if (index < 0) return null;
            List<?> list = (List<?>) root;
            return index < list.size() ? list.get(index) : null;
        }

        if (root.getClass().isArray()) {
            int index = parseIndex(key);
            if (index < 0) return null;
            Object[] list = (Object[]) root;
            return index < list.length ? list[index] : null;
        }

        return null;
    }

    /**
     * Parses a path segment as a non-negative array index without throwing.
     * Returns {@code -1} for anything that is not a valid non-negative integer.
     */
    private static int parseIndex(String part) {
        if (part.isEmpty()) return -1;
        int value = 0;
        for (int i = 0; i < part.length(); i++) {
            char c = part.charAt(i);
            if (c < '0' || c > '9') return -1;
            value = value * 10 + (c - '0');
            if (value < 0) return -1; // overflow
        }
        return value;
    }
}
