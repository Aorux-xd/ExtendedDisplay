package dev.ed.edcore.api.auth.event;

public record AuthFailedLoginEvent(String username, String ip, String reason) {
}
