package dev.ed.edauth.auth;

import dev.ed.edcore.api.auth.AuthListener;
import dev.ed.edcore.api.auth.EDAuthAPI;
import dev.ed.edcore.api.auth.PlayerProfile;
import dev.ed.edauth.storage.AccountRecord;
import dev.ed.edauth.storage.AccountRepository;
import dev.ed.edauth.storage.SecurityRepository;
import dev.ed.edauth.util.AuthConfig;
import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.edcore.security.BcryptThrottle;
import dev.ed.edcore.util.AuditLogger;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AuthService implements EDAuthAPI {
    private final AccountRepository repo;
    private final SecurityRepository security;
    private final AuthConfig cfg;
    private final AuditLogger audit;
    private final BcryptThrottle bcryptThrottle;
    private final List<AuthListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Long> lockUntil = new ConcurrentHashMap<>();
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, Boolean> authed = new ConcurrentHashMap<>();
    private final Map<String, String> sessionIp = new ConcurrentHashMap<>();
    private final Map<String, Long> pendingTotp = new ConcurrentHashMap<>();

    public AuthService(AccountRepository repo, SecurityRepository security, AuthConfig cfg, AuditLogger audit) {
        this.repo = repo;
        this.security = security;
        this.cfg = cfg;
        this.audit = audit;
        this.bcryptThrottle = new BcryptThrottle(cfg.bcryptMaxPerSecond);
    }

    public boolean register(String username, String password, String ip) throws SQLException {
        if (!cfg.passwordPolicy.validate(username, password)) {
            audit.log("register_failed", username, ip, "password_policy");
            return false;
        }
        if (!bcryptThrottle.allow()) {
            audit.log("register_limited", username, ip, "bcrypt_throttle");
            return false;
        }
        String hash = EDCoreProvider.get().getCryptoManager().hash(password, cfg.algorithm);
        boolean ok = repo.create(username, hash, ip, Instant.now().getEpochSecond());
        if (ok) {
            listeners.forEach(l -> l.onRegister(username));
            audit.log("register_success", username, ip, "ok");
        } else {
            audit.log("register_failed", username, ip, "exists");
        }
        return ok;
    }

    public boolean login(String username, String password, String ip) throws SQLException {
        if (password != null && password.startsWith("token:")) {
            String token = password.substring("token:".length());
            if (loginWithToken(token, ip)) {
                return true;
            }
        }
        long now = Instant.now().getEpochSecond();
        Long blocked = lockUntil.get(username.toLowerCase());
        if (blocked != null && blocked > now) return false;

        String nickLower = username.toLowerCase();
        if (!cfg.rateLimiter.allow(ip, nickLower, false)) {
            audit.log("login_limited", username, ip, "rate_limit");
            return false;
        }

        Optional<AccountRecord> account = repo.find(username);
        if (account.isEmpty()) {
            cfg.rateLimiter.allow(ip, nickLower, true);
            audit.log("login_failed", username, ip, "invalid_credentials");
            return false;
        }
        if (!bcryptThrottle.allow()) {
            audit.log("login_limited", username, ip, "bcrypt_throttle");
            return false;
        }
        boolean ok = EDCoreProvider.get().getCryptoManager().verify(password, account.get().passwordHash());
        if (!ok) {
            cfg.rateLimiter.allow(ip, nickLower, true);
            int n = attempts.merge(username.toLowerCase(), 1, Integer::sum);
            if (n >= cfg.bruteMaxAttempts) {
                lockUntil.put(username.toLowerCase(), now + cfg.bruteDelaySec);
                attempts.remove(username.toLowerCase());
            }
            audit.log("login_failed", username, ip, "invalid_credentials");
            return false;
        }
        attempts.remove(username.toLowerCase());
        lockUntil.remove(username.toLowerCase());

        try {
            if (security.getTotpSecret(username).isPresent() && !security.isTrustedIp(username, ip)) {
                pendingTotp.put(username.toLowerCase() + "|" + ip, now + 180);
                audit.log("login_pending_2fa", username, ip, "totp_required");
                return false;
            }
        } catch (Exception ignored) {
        }

        repo.updateLogin(username, ip, now);
        forceLogin(username, ip);
        listeners.forEach(l -> l.onLogin(username));
        audit.log("login_success", username, ip, "ok");
        return true;
    }

    public void logout(String username) {
        String key = username.toLowerCase();
        String ip = sessionIp.get(key);
        authed.remove(key);
        sessionIp.remove(key);
        EDCoreProvider.get().getSessionManager().remove(key);
        listeners.forEach(l -> l.onLogout(username));
        audit.log("logout", username, ip, "manual");
    }

    public boolean canBySession(String username, String ip) {
        if (!cfg.sessionEnabled) return false;
        String key = username.toLowerCase();
        boolean valid = EDCoreProvider.get().getSessionManager().isValid(key, Instant.now().getEpochSecond());
        if (!valid) return false;
        if (!cfg.sessionStrictIpBinding) return true;
        String savedIp = sessionIp.get(key);
        if (savedIp == null || ip == null) return false;
        return ipMatches(savedIp, ip, cfg.sessionIpMask);
    }

    @Override
    public boolean isRegistered(String playerName) {
        try {
            return repo.find(playerName).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isAuthenticated(String playerName) {
        return authed.getOrDefault(playerName.toLowerCase(), false);
    }

    @Override
    public boolean isPremium(String playerName) {
        try {
            return repo.find(playerName).map(AccountRecord::premium).orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void forceLogin(String playerName, String ipAddress) {
        String key = playerName.toLowerCase();
        authed.put(key, true);
        if (ipAddress != null) {
            sessionIp.put(key, ipAddress);
        }
        long exp = Instant.now().getEpochSecond() + cfg.sessionTimeoutSec;
        EDCoreProvider.get().getSessionManager().put(key, exp);
    }

    @Override
    public void forceRegister(String playerName, String password, String ipAddress) {
        try {
            if (register(playerName, password, ipAddress)) {
                forceLogin(playerName, ipAddress);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void changePassword(String playerName, String newPassword) {
        try {
            String hash = EDCoreProvider.get().getCryptoManager().hash(newPassword, cfg.algorithm);
            repo.updatePassword(playerName, hash);
            audit.log("password_change", playerName, null, "ok");
        } catch (Exception ignored) {
        }
    }

    @Override
    public void unregister(String playerName) {
        try {
            repo.delete(playerName);
            logout(playerName);
            listeners.forEach(l -> l.onUnregister(playerName));
            audit.log("unregister", playerName, null, "ok");
        } catch (Exception ignored) {
        }
    }

    public void setPremium(String playerName, boolean premium) {
        try {
            repo.setPremium(playerName, premium);
            listeners.forEach(l -> l.onPremiumChange(playerName, premium));
            audit.log("premium_toggle", playerName, null, premium ? "premium_on" : "premium_off");
        } catch (Exception ignored) {
        }
    }

    public boolean verifyPassword(String username, String password) {
        try {
            Optional<AccountRecord> account = repo.find(username);
            if (account.isEmpty()) return false;
            if (!bcryptThrottle.allow()) return false;
            return EDCoreProvider.get().getCryptoManager().verify(password, account.get().passwordHash());
        } catch (Exception e) {
            return false;
        }
    }

    public String enable2fa(String username) throws Exception {
        String secret = EDCoreProvider.get().getCryptoManager().generateTotpSecret();
        security.setTotp(username, secret, true);
        return secret;
    }

    public boolean disable2fa(String username, int code) throws Exception {
        Optional<String> secret = security.getTotpSecret(username);
        if (secret.isEmpty() || !EDCoreProvider.get().getCryptoManager().verifyTotp(secret.get(), code)) {
            return false;
        }
        security.deleteTotp(username);
        return true;
    }

    public void forceReset2fa(String username) throws Exception {
        security.deleteTotp(username);
    }

    public java.util.List<String> regenerateRecoveryCodes(String username) throws Exception {
        return security.replaceRecoveryCodes(username);
    }

    public boolean recover2fa(String username, String code) throws Exception {
        boolean ok = security.consumeRecoveryCode(username, code);
        if (ok) {
            security.deleteTotp(username);
        }
        return ok;
    }

    public String createLoginToken(String username) throws Exception {
        return security.createLoginToken(username, 3600);
    }

    public void revokeLoginTokens(String username) throws Exception {
        security.revokeTokens(username);
    }

    public boolean loginWithToken(String token, String ip) {
        try {
            Optional<String> username = security.consumeLoginToken(token);
            if (username.isEmpty()) return false;
            forceLogin(username.get(), ip);
            audit.log("login_success", username.get(), ip, "token");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean is2faEnabled(String username) {
        try {
            return security.getTotpSecret(username).isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean is2FAEnabled(String playerName) {
        return is2faEnabled(playerName);
    }

    @Override
    public String generateTOTPSecret(String playerName) throws Exception {
        return enable2fa(playerName);
    }

    @Override
    public boolean validateTOTP(String playerName, int code) {
        try {
            Optional<String> secret = security.getTotpSecret(playerName);
            return secret.isPresent() && EDCoreProvider.get().getCryptoManager().verifyTotp(secret.get(), code);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void resetTOTP(String playerName) throws Exception {
        forceReset2fa(playerName);
    }

    @Override
    public String createSessionToken(String playerName) throws Exception {
        return createLoginToken(playerName);
    }

    public boolean isTotpPending(String username, String ip) {
        Long exp = pendingTotp.get(username.toLowerCase() + "|" + ip);
        return exp != null && exp >= Instant.now().getEpochSecond();
    }

    public boolean verifyTotpLogin(String username, String ip, int code, boolean trustIp) {
        String key = username.toLowerCase() + "|" + ip;
        Long exp = pendingTotp.get(key);
        long now = Instant.now().getEpochSecond();
        if (exp == null || exp < now) {
            return false;
        }
        try {
            Optional<String> secret = security.getTotpSecret(username);
            if (secret.isEmpty() || !EDCoreProvider.get().getCryptoManager().verifyTotp(secret.get(), code)) {
                return false;
            }
            pendingTotp.remove(key);
            forceLogin(username, ip);
            if (trustIp) {
                security.trustIp(username, ip, 30L * 24L * 3600L);
            }
            audit.log("login_success", username, ip, "totp");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean ipMatches(String a, String b, int mask) {
        if (mask >= 32) return a.equals(b);
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        if (pa.length != 4 || pb.length != 4) return a.equals(b);
        int octets = Math.max(0, Math.min(4, mask / 8));
        for (int i = 0; i < octets; i++) {
            if (!pa[i].equals(pb[i])) return false;
        }
        return true;
    }

    @Override
    public void addAuthListener(AuthListener listener) {
        listeners.add(listener);
    }

    @Override
    public PlayerProfile getProfile(String playerName) {
        try {
            Optional<AccountRecord> o = repo.find(playerName);
            if (o.isEmpty()) return null;
            AccountRecord a = o.get();
            return new PlayerProfile(a.username(), a.premium(), a.firstIp(), a.lastIp(), a.lastLogin(), a.registered());
        } catch (Exception e) {
            return null;
        }
    }
}
