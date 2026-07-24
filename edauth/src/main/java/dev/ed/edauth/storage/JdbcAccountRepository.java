package dev.ed.edauth.storage;

import dev.ed.edcore.api.database.DatabaseManager;

import java.sql.*;
import java.util.Optional;

public final class JdbcAccountRepository implements AccountRepository {
    private final DatabaseManager db;

    public JdbcAccountRepository(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public void init() throws SQLException {
        String ddlAccounts = """
                CREATE TABLE IF NOT EXISTS edauth_accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR(16) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    premium BOOLEAN DEFAULT FALSE,
                    first_ip VARCHAR(45),
                    last_ip VARCHAR(45),
                    last_login BIGINT,
                    registered BIGINT NOT NULL
                )
                """;
        String ddlSessions = """
                CREATE TABLE IF NOT EXISTS edauth_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username VARCHAR(16) NOT NULL,
                    ip VARCHAR(45),
                    login_time BIGINT NOT NULL
                )
                """;
        try (Connection c = db.getConnection(); Statement st = c.createStatement()) {
            st.execute(ddlAccounts);
            st.execute(ddlSessions);
        }
    }

    @Override
    public Optional<AccountRecord> find(String username) throws SQLException {
        String sql = "SELECT username,password,premium,first_ip,last_ip,last_login,registered FROM edauth_accounts WHERE lower(username)=lower(?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new AccountRecord(
                        rs.getString(1), rs.getString(2), rs.getBoolean(3), rs.getString(4),
                        rs.getString(5), rs.getLong(6), rs.getLong(7)
                ));
            }
        }
    }

    @Override
    public boolean create(String username, String passwordHash, String ip, long now) throws SQLException {
        if (find(username).isPresent()) return false;
        String sql = "INSERT INTO edauth_accounts(username,password,premium,first_ip,last_ip,last_login,registered) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setBoolean(3, false);
            ps.setString(4, ip);
            ps.setString(5, ip);
            ps.setLong(6, now);
            ps.setLong(7, now);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public void updatePassword(String username, String passwordHash) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE edauth_accounts SET password=? WHERE lower(username)=lower(?)")) {
            ps.setString(1, passwordHash);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String username) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM edauth_accounts WHERE lower(username)=lower(?)")) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    @Override
    public void setPremium(String username, boolean premium) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE edauth_accounts SET premium=? WHERE lower(username)=lower(?)")) {
            ps.setBoolean(1, premium);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    @Override
    public void updateLogin(String username, String ip, long now) throws SQLException {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE edauth_accounts SET last_ip=?,last_login=? WHERE lower(username)=lower(?)")) {
            ps.setString(1, ip);
            ps.setLong(2, now);
            ps.setString(3, username);
            ps.executeUpdate();
        }
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO edauth_sessions(username,ip,login_time) VALUES(?,?,?)")) {
            ps.setString(1, username);
            ps.setString(2, ip);
            ps.setLong(3, now);
            ps.executeUpdate();
        }
    }
}
