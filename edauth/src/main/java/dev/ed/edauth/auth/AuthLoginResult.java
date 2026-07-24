package dev.ed.edauth.auth;

public enum AuthLoginResult {
    SUCCESS,
    INVALID_CREDENTIALS,
    RATE_LIMITED,
    TWO_FACTOR_REQUIRED
}
