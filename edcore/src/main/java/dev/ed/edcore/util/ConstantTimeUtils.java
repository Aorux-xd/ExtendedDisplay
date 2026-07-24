package dev.ed.edcore.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class ConstantTimeUtils {
    private ConstantTimeUtils() {}

    public static boolean secureCompare(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    public static boolean secureCompare(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return secureCompare(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
