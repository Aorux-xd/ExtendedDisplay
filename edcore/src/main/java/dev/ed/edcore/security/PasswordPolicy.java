package dev.ed.edcore.security;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class PasswordPolicy {
    public int minLength = 6;
    public int maxLength = 100;
    public boolean requireUppercase = false;
    public boolean requireDigit = false;
    public boolean requireSpecial = false;
    public boolean blockCommonPasswords = true;

    private final Set<String> common = new HashSet<>();

    public void loadBuiltinCommonPasswords(InputStream in) {
        common.clear();
        if (in == null) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                common.add(line);
            }
        } catch (Exception ignored) {
        }
    }

    public void loadCommonPasswords(Set<String> words) {
        common.clear();
        if (words != null) {
            common.addAll(words);
        }
    }

    public boolean validate(String username, String password) {
        if (password == null) {
            return false;
        }
        if (password.length() < minLength || password.length() > maxLength) {
            return false;
        }
        if (username != null && !username.isBlank() && password.equalsIgnoreCase(username)) {
            return false;
        }
        if (requireUppercase && password.chars().noneMatch(Character::isUpperCase)) {
            return false;
        }
        if (requireDigit && password.chars().noneMatch(Character::isDigit)) {
            return false;
        }
        if (requireSpecial) {
            String specials = "!@#$%^&*()-_=+";
            if (password.chars().noneMatch(ch -> specials.indexOf(ch) >= 0)) {
                return false;
            }
        }
        return !blockCommonPasswords || !common.contains(password);
    }
}
