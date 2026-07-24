package dev.ed.edcore.internal.database;

import dev.ed.edcore.api.database.DatabaseManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class JdbcDatabaseManager implements DatabaseManager {
    private Type type = Type.SQLITE;
    private String jdbcUrl = "jdbc:sqlite::memory:";
    private String user = "";
    private String password = "";

    @Override
    public void configure(Type type, String jdbcUrl, String user, String password) throws SQLException {
        this.type = type;
        this.jdbcUrl = jdbcUrl;
        this.user = user != null ? user : "";
        this.password = password != null ? password : "";
        try (Connection ignored = getConnection()) {
            // smoke check
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (user.isEmpty()) {
            return DriverManager.getConnection(jdbcUrl);
        }
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    @Override
    public Type getType() {
        return type;
    }
}
