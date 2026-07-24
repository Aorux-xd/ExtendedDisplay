package dev.ed.edhub.config;

public enum HubTimePreset {
    DAY(1000L),
    NOON(6000L),
    NIGHT(13000L),
    MIDNIGHT(18000L);

    private final long ticks;

    HubTimePreset(long ticks) {
        this.ticks = ticks;
    }

    public long ticks() {
        return ticks;
    }

    public static HubTimePreset parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DAY;
        }
        try {
            return HubTimePreset.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return DAY;
        }
    }
}
