package dev.ed.edcore.api.auth.event;

public record AuthPremiumChangeEvent(String username, boolean premium) {
}
