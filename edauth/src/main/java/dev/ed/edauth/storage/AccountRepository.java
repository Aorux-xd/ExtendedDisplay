package dev.ed.edauth.storage;

import java.sql.SQLException;
import java.util.Optional;

public interface AccountRepository {
    void init() throws SQLException;

    Optional<AccountRecord> find(String username) throws SQLException;

    boolean create(String username, String passwordHash, String ip, long now) throws SQLException;

    void updatePassword(String username, String passwordHash) throws SQLException;

    void delete(String username) throws SQLException;

    void setPremium(String username, boolean premium) throws SQLException;

    void updateLogin(String username, String ip, long now) throws SQLException;
}
