package dev.ed.edcore.internal.crypto;

import dev.ed.edcore.api.crypto.CryptoManager;
import dev.ed.edcore.crypto.TotpUtil;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class SimpleCryptoManager implements CryptoManager {
    @Override
    public String hash(String secret, Algorithm algorithm) {
        if (algorithm == Algorithm.SHA512) {
            return "sha512$" + sha512(secret);
        }
        if (algorithm == Algorithm.BCRYPT) {
            // lightweight fallback label for public plugin stack
            return hashPbkdf2(secret, "bcrypt");
        }
        return hashPbkdf2(secret, "pbkdf2");
    }

    @Override
    public boolean verify(String secret, String encodedHash) {
        try {
            String[] parts = encodedHash.split("\\$");
            if (parts.length == 2 && "sha512".equals(parts[0])) {
                return sha512(secret).equals(parts[1]);
            }
            if (parts.length == 4 && ("pbkdf2".equals(parts[0]) || "bcrypt".equals(parts[0]))) {
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                int it = Integer.parseInt(parts[2]);
                byte[] hash = pbkdf2(secret.toCharArray(), salt, it, 32);
                return Base64.getEncoder().encodeToString(hash).equals(parts[3]);
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public String generateTotpSecret() {
        return TotpUtil.newSecret();
    }

    @Override
    public boolean verifyTotp(String secret, int code) {
        return TotpUtil.verifyCode(secret, code);
    }

    private String hashPbkdf2(String secret, String label) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            int it = 120_000;
            byte[] hash = pbkdf2(secret.toCharArray(), salt, it, 32);
            return label + "$" + Base64.getEncoder().encodeToString(salt) + "$" + it + "$" +
                    Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLen) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLen * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return skf.generateSecret(spec).getEncoded();
    }

    private static String sha512(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] out = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
