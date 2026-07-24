package dev.ed.edcore.internal.migration;

import dev.ed.edcore.api.migration.MigrationManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SimpleMigrationManager implements MigrationManager {
    private final Map<String, MigrationProvider> providers = new ConcurrentHashMap<>();

    @Override
    public void register(MigrationProvider provider) {
        providers.put(provider.id().toLowerCase(), provider);
    }

    @Override
    public int migrate(String providerId, String targetId) throws Exception {
        MigrationProvider p = providers.get(providerId.toLowerCase());
        if (p == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerId);
        }
        return p.migrateTo(targetId);
    }
}
