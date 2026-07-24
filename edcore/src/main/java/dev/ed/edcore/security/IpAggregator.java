package dev.ed.edcore.security;

public final class IpAggregator {
    private IpAggregator() {}

    public static String aggregate(String ip, int cidr) {
        if (ip == null) {
            return "unknown";
        }
        if (cidr <= 0) {
            return ip;
        }
        if (cidr >= 32) {
            return ip;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return ip;
        }
        int octets = Math.max(0, Math.min(4, cidr / 8));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                sb.append('.');
            }
            if (i < octets) {
                sb.append(parts[i]);
            } else {
                sb.append('0');
            }
        }
        return sb + "/" + cidr;
    }
}
