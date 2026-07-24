package dev.ed.edcore.internal.messaging;

import dev.ed.edcore.api.PlatformType;
import dev.ed.edcore.api.messaging.EDMessaging;

public final class EDMessagingImpl implements EDMessaging {

    private final PlatformType platform;

    public EDMessagingImpl(PlatformType platform) {
        this.platform = platform;
    }

    @Override
    public PlatformType getPlatform() {
        return platform;
    }

    @Override
    public String getDefaultPluginChannel() {
        return "edcore:channel";
    }
}
