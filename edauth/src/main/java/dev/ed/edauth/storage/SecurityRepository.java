package dev.ed.edauth.storage;

import dev.ed.edcore.api.database.DatabaseManager;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public final class SecurityRepository {
    private final DatabaseManager db;

    public SecurityRepository(DatabaseManager db) {
        this.db = db;
    }

    public void init() throws Exception {
        try (Connection c = db.getConnection(); var st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS edauth_totp (
                      username VARCHAR(16) PRIMARY KEY,
                      secret VARCHAR(255) NOT NULL,
                      enabled BOOLEAN NOT NULL DEFAULT FALSE
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS edauth_totp_recovery (
                      username VARCHAR(16) NOT NULL,
                      code_hash VARCHAR(255) NOT NULL
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS edauth_login_tokens (
                      token VARCHAR(255) PRIMARY KEY,
                      username VARCHAR(16) NOT NULL,
                      expires_at BIGINT NOT NULL,
                      used BOOLEAN NOT NULL DEFAULT FALSE
                    )
                    """);
            st.execute("""
                    CREATE TABLE IF NOT EXISTS edauth_totp_trusted_ip (
                      username VARCHAR(16) NOT NULL,
                      ip VARCHAR(64) NOT NULL,
                      expires_at BIGINT NOT NULL
                    )
                    """);
        }
    }

    public void setTotp(String username, String secret, boolean enabled) throws Exception {
        deleteTotp(username);
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO edauth_totp(username,secret,enabled) VALUES(?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, secret);
            ps.setBoolean(3, enabled);
            ps.executeUpdate();
        }
    }

    public Optional<String> getTotpSecret(String username) throws Exception {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT secret FROM edauth_totp WHERE lower(username)=lower(?) AND enabled=true")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.ofNullable(rs.getString(1));
            }
        }
    }

    public void deleteTotp(String username) throws Exception {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM edauth_totp WHERE lower(username)=lower(?)")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM edauth_totp_recovery WHERE lower(username)=lower(?)")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    public List<String> replaceRecoveryCodes(String username) throws Exception {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM edauth_totp_recovery WHERE lower(username)=lower(?)")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
        List<String> raw = new ArrayList<>();
        SecureRandom rnd = new SecureRandom();
        for (int i = 0; i < 8; i++) {
            String code = String.format("%08d", rnd.nextInt(100_000_000));
            raw.add(code);
            String hash = dev.ed.edcore.api.EDCoreProvider.get().getCryptoManager()
                    .hash(code, dev.ed.edcore.api.crypto.CryptoManager.Algorithm.BCRYPT);
            try (Connection c = db.getConnection();
                 PreparedStatement ps = c.prepareStatement("INSERT INTO edauth_totp_recovery(username,code_hash) VALUES(?,?)")) {
                ps.setString(1, username);
                ps.setString(2, hash);
                ps.executeUpdate();
            }
        }
        return raw;
    }

    public boolean consumeRecoveryCode(String username, String code) throws Exception {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT code_hash FROM edauth_totp_recovery WHERE lower(username)=lower(?)")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String hash = rs.getString(1);
                    boolean ok = dev.ed.edcore.api.EDCoreProvider.get().getCryptoManager().verify(code, hash);
                    if (ok) {
                        try (Connection c2 = db.getConnection();
                             PreparedStatement ps2 = c2.prepareStatement("DELETE FROM edauth_totp_recovery WHERE lower(username)=lower(?) AND code_hash=?")) {
                            ps2.setString(1, username);
                            ps2.setString(2, hash);
                            ps2.executeUpdate();
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String createLoginToken(String username, long ttlSec) throws Exception {
        byte[] b = new byte[24];
        new SecureRandom().nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        long exp = Instant.now().getEpochSecond() + ttlSec;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO edauth_login_tokens(token,username,expires_at,used) VALUES(?,?,?,false)")) {
            ps.setString(1, token);
            ps.setString(2, username);
            ps.setLong(3, exp);
            ps.executeUpdate();
        }
        return token;
    }

    public Optional<String> consumeLoginToken(String token) throws Exception {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT username,expires_at,used FROM edauth_login_tokens WHERE token=?")) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                String u = rs.getString(1);
                long exp = rs.getLong(2);
                boolean used = rs.getBoolean(3);
                if (used || exp < Instant.now().getEpochSecond()) return Optional.empty();
                try (Connection c2 = db.getConnection();
                     PreparedStatement ps2 = c2.prepareStatement("UPDATE edauth_login_tokens SET used=true WHERE token=?")) {
                    ps2.setString(1, token);
                    ps2.executeUpdate();
                }
                return Optional.of(u);
            }
        }
    }

    public void revokeTokens(String username) throws Exception {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM edauth_login_tokens WHERE lower(username)=lower(?)")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    public void trustIp(String username, String ip, long ttlSec) throws Exception {
        long exp = Instant.now().getEpochSecond() + ttlSec;
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO edauth_totp_trusted_ip(username,ip,expires_at) VALUES(?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, ip);
            ps.setLong(3, exp);
            ps.executeUpdate();
        }
    }

    public boolean isTrustedIp(String username, String ip) throws Exception {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM edauth_totp_trusted_ip WHERE lower(username)=lower(?) AND ip=? AND expires_at>=?")) {
            ps.setString(1, username);
            ps.setString(2, ip);
            ps.setLong(3, Instant.now().getEpochSecond());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
