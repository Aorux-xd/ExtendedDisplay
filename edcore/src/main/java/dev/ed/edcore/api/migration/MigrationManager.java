package dev.ed.edcore.api.migration;

public interface MigrationManager {
    interface MigrationProvider {
        String id();

        int migrateTo(String targetId) throws Exception;
    }

    void register(MigrationProvider provider);

    int migrate(String providerId, String targetId) throws Exception;
}
