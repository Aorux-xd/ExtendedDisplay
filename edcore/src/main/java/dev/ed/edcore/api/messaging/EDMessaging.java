package dev.ed.edcore.api.messaging;

import dev.ed.edcore.api.PlatformType;

/**
 * Обмен сообщениями между backend и прокси. Базовый контракт; расширения — в следующих версиях.
 */
public interface EDMessaging {

    PlatformType getPlatform();

    /**
     * Рекомендуемый идентификатор канала PluginMessage (Paper → Velocity).
     */
    String getDefaultPluginChannel();
}
