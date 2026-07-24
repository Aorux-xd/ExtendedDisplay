package dev.ed.edchat.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BlockedWordsFilter {
    private BlockedWordsFilter() {}

    public static String filter(String message, List<String> blockedWords, String replacement) {
        if (message == null || blockedWords == null || blockedWords.isEmpty()) {
            return message;
        }
        String out = message;
        String lowerMessage = out.toLowerCase(Locale.ROOT);
        for (String word : blockedWords) {
            if (word == null || word.isBlank()) {
                continue;
            }
            String lowerWord = word.toLowerCase(Locale.ROOT);
            int index = lowerMessage.indexOf(lowerWord);
            while (index != -1) {
                out = out.substring(0, index) + replacement + out.substring(index + word.length());
                lowerMessage = out.toLowerCase(Locale.ROOT);
                index = lowerMessage.indexOf(lowerWord);
            }
        }
        return out;
    }

    public static List<String> findMatched(String message, List<String> blockedWords) {
        List<String> matched = new ArrayList<>();
        if (message == null || blockedWords == null) {
            return matched;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        for (String word : blockedWords) {
            if (word != null && !word.isBlank() && lower.contains(word.toLowerCase(Locale.ROOT))) {
                matched.add(word);
            }
        }
        return matched;
    }

    public static boolean containsBlocked(String message, List<String> blockedWords) {
        if (message == null || blockedWords == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        for (String word : blockedWords) {
            if (word != null && !word.isBlank() && lower.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
