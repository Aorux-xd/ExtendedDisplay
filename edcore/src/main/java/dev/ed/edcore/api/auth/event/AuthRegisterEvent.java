package dev.ed.edcore.api.auth.event;

public record AuthRegisterEvent(String username, String ip, boolean pre) {
}
