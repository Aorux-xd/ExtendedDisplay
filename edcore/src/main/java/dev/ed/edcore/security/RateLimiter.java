package dev.ed.edcore.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {
    public boolean enabled = true;
    public int ipPerMinute = 5;
    public int nickPerMinute = 10;
    public int globalIpThreshold = 15;
    public int globalIpBlockMinutes = 10;
    public int aggregateSubnet = 0;

    private record Counter(long windowStartSec, int count) {}

    private final Map<String, Counter> ipMinute = new ConcurrentHashMap<>();
    private final Map<String, Counter> nickMinute = new ConcurrentHashMap<>();
    private final Map<String, Counter> ipGlobal = new ConcurrentHashMap<>();
    private final Map<String, Long> ipBlockedUntil = new ConcurrentHashMap<>();

    public boolean allow(String ip, String nickLower, boolean failedAttempt) {
        if (!enabled) {
            return true;
        }
        long now = Instant.now().getEpochSecond();

        String ipKey = IpAggregator.aggregate(ip, aggregateSubnet);
        Long block = ipBlockedUntil.get(ipKey);
        if (block != null && block > now) {
            return false;
        }

        if (!checkWindow(ipMinute, "ip:" + ipKey, now, ipPerMinute, 60)) {
            return false;
        }
        if (!checkWindow(nickMinute, "nick:" + nickLower, now, nickPerMinute, 60)) {
            return false;
        }

        if (failedAttempt) {
            boolean ok = checkWindow(ipGlobal, "gip:" + ipKey, now, globalIpThreshold, 600);
            if (!ok) {
                ipBlockedUntil.put(ipKey, now + (long) globalIpBlockMinutes * 60L);
                ipGlobal.remove("gip:" + ipKey);
                return false;
            }
        }

        return true;
    }

    private boolean checkWindow(Map<String, Counter> map, String key, long now, int limit, int windowSec) {
        if (limit <= 0) {
            return true;
        }
        Counter c = map.get(key);
        if (c == null || now - c.windowStartSec >= windowSec) {
            map.put(key, new Counter(now, 1));
            return true;
        }
        if (c.count >= limit) {
            return false;
        }
        map.put(key, new Counter(c.windowStartSec, c.count + 1));
        return true;
    }
}
