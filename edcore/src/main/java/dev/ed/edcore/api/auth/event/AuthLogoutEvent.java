package dev.ed.edcore.api.auth.event;

public record AuthLogoutEvent(String username, String ip) {
}
