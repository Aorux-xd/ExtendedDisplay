package dev.ed.edcore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Единое форматирование ED: legacy {@code &}, hex {@code &#RRGGBB}, градиент {@code &#A&#Bтекст}.
 * Одиночный {@code &#RRGGBB} не превращается в градиент; MiniMessage-теги {@code <#...>} сохраняются.
 */
public final class MessageFormatter {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    /** Градиент только при двух hex подряд: &#RRGGBB&#RRGGBBтекст */
    private static final Pattern GRADIENT = Pattern.compile(
            "&#([0-9A-Fa-f]{6})&#([0-9A-Fa-f]{6})([^&#]*)", Pattern.DOTALL);
    private static final Pattern HEX = Pattern.compile("&#([0-9A-Fa-f]{6})");
    private static final Pattern MINI_TAG = Pattern.compile("<[^>]+>");

    public Component format(String input) {
        return format(input, Map.of());
    }

    public Component format(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String s = applyMapPlaceholders(input, placeholders);
        List<String> preservedTags = new ArrayList<>();
        s = shieldMiniMessageTags(s, preservedTags);
        s = parseGradient(s);
        s = parseHex(s);
        s = unshieldMiniMessageTags(s, preservedTags);
        s = parseAmpersand(s);
        return MINI.deserialize(s);
    }

    public Component format(Object player, String input) {
        return format(player, input, Map.of());
    }

    public Component format(Object player, String input, Map<String, String> placeholders) {
        String s = applyMapPlaceholders(input == null ? "" : input, placeholders);
        s = PlaceholderHook.setPlaceholders(player, s);
        return format(s);
    }

    /** Один legacy-hex {@code &#RRGGBB} → MiniMessage {@code <#RRGGBB>} (без градиента). */
    public static String singleHexToTag(String legacyOrHex) {
        if (legacyOrHex == null || legacyOrHex.isBlank()) {
            return "";
        }
        Matcher m = HEX.matcher(legacyOrHex.trim());
        if (m.find()) {
            return "<#" + m.group(1) + ">";
        }
        if (legacyOrHex.startsWith("#") && legacyOrHex.length() >= 7) {
            return "<" + legacyOrHex.substring(0, 7) + ">";
        }
        return "";
    }

    private static String applyMapPlaceholders(String input, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return input;
        }
        String out = input;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            String val = e.getValue() == null ? "" : MINI.escapeTags(e.getValue());
            out = out.replace(e.getKey(), val);
        }
        return out;
    }

    private static String shieldMiniMessageTags(String input, List<String> preserved) {
        Matcher m = MINI_TAG.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            preserved.add(m.group());
            m.appendReplacement(sb, Matcher.quoteReplacement("\uE000" + (preserved.size() - 1) + "\uE001"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String unshieldMiniMessageTags(String input, List<String> preserved) {
        Matcher m = Pattern.compile("\uE000(\\d+)\uE001").matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            int idx = Integer.parseInt(m.group(1));
            String tag = idx >= 0 && idx < preserved.size() ? preserved.get(idx) : "";
            m.appendReplacement(sb, Matcher.quoteReplacement(tag));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String parseGradient(String input) {
        Matcher m = GRADIENT.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    "<gradient:#" + m.group(1) + ":#" + m.group(2) + ">" + m.group(3) + "</gradient>"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String parseHex(String input) {
        Matcher m = HEX.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement("<#" + m.group(1) + ">"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String parseAmpersand(String input) {
        StringBuilder out = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < input.length()) {
                String tag = legacyTag(input.charAt(i + 1));
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String legacyTag(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'l' -> "<bold>";
            case 'o' -> "<italic>";
            case 'n' -> "<underline>";
            case 'm' -> "<strikethrough>";
            case 'k' -> "<obfuscated>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }
}
