package dev.ed.network.proxy;

import java.util.Objects;

public final class ServerSnapshot {

    private final String name;
    private final int online;
    private final int max;
    private final String motd;
    private final long pingMs;
    private final String version;
    /** true если ping с backend прошёл успешно */
    private final boolean backendReachable;

    public ServerSnapshot(String name, int online, int max, String motd, long pingMs, String version,
                          boolean backendReachable) {
        this.name = Objects.requireNonNull(name, "name");
        this.online = online;
        this.max = max;
        this.motd = motd != null ? motd : "";
        this.pingMs = pingMs;
        this.version = version != null ? version : "";
        this.backendReachable = backendReachable;
    }

    public String name() {
        return name;
    }

    public int online() {
        return online;
    }

    public int max() {
        return max;
    }

    public String motd() {
        return motd;
    }

    public long pingMs() {
        return pingMs;
    }

    public String version() {
        return version;
    }

    public boolean backendReachable() {
        return backendReachable;
    }

    public String statusToken() {
        return backendReachable ? "online" : "offline";
    }
}
