package dev.ed.edcore.api.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseManager {
    enum Type {
        SQLITE,
        H2,
        MYSQL,
        POSTGRESQL
    }

    void configure(Type type, String jdbcUrl, String user, String password) throws SQLException;

    Connection getConnection() throws SQLException;

    Type getType();
}
