package dev.ed.edcore.api.auth.event;

public record AuthLoginEvent(String username, String ip, AuthLoginMethod method) {
}
