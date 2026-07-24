package dev.ed.edauth.util;

import java.util.HashMap;
import java.util.Map;

public final class MessageBundle {
    private final Map<String, String> map = new HashMap<>();

    public void put(String key, String value) {
        map.put(key, value);
    }

    public String get(String key, String def) {
        String v = map.getOrDefault(key, def);
        return v.replace('&', '§');
    }
}
