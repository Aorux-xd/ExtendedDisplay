package dev.ed.edcore.api.auth;

public interface EDAuthAPI {
    boolean isRegistered(String playerName);

    boolean isAuthenticated(String playerName);

    boolean isPremium(String playerName);

    void forceLogin(String playerName, String ipAddress);

    void forceRegister(String playerName, String password, String ipAddress);

    void changePassword(String playerName, String newPassword);

    void unregister(String playerName);

    void addAuthListener(AuthListener listener);

    PlayerProfile getProfile(String playerName);

    boolean is2FAEnabled(String playerName);

    String generateTOTPSecret(String playerName) throws Exception;

    boolean validateTOTP(String playerName, int code);

    void resetTOTP(String playerName) throws Exception;

    String createSessionToken(String playerName) throws Exception;
}
