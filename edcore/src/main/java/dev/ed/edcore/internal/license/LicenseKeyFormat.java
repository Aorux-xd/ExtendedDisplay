package dev.ed.edcore.internal.license;

import java.util.regex.Pattern;

/** Ключ: ровно 10 символов, латиница и цифры. */
public final class LicenseKeyFormat {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9]{10}$");

    private LicenseKeyFormat() {
    }

    public static boolean isValid(String key) {
        return key != null && PATTERN.matcher(key.trim()).matches();
    }
}
