package dev.ed.edcore.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

public final class TotpUtil {
    private TotpUtil() {}

    public static String newSecret() {
        byte[] buf = new byte[20];
        new SecureRandom().nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    public static boolean verifyCode(String secret, int code) {
        long step = Instant.now().getEpochSecond() / 30L;
        for (long i = -1; i <= 1; i++) {
            if (generate(secret, step + i) == code) {
                return true;
            }
        }
        return false;
    }

    public static int generate(String secret, long timeStep) {
        try {
            byte[] key = Base64.getUrlDecoder().decode(secret);
            byte[] data = new byte[8];
            long v = timeStep;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (v & 0xFF);
                v >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int off = hash[hash.length - 1] & 0x0F;
            int bin = ((hash[off] & 0x7F) << 24)
                    | ((hash[off + 1] & 0xFF) << 16)
                    | ((hash[off + 2] & 0xFF) << 8)
                    | (hash[off + 3] & 0xFF);
            return bin % 1_000_000;
        } catch (Exception e) {
            return -1;
        }
    }
}
