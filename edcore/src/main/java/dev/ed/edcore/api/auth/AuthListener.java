package dev.ed.edcore.api.auth;

public interface AuthListener {
    default void onRegister(String username) {}

    default void onLogin(String username) {}

    default void onLogout(String username) {}

    default void onPremiumChange(String username, boolean premium) {}

    default void onUnregister(String username) {}
}
