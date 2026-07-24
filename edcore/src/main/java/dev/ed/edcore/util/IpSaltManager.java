package dev.ed.edcore.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

public final class IpSaltManager {
    private final Path file;

    public IpSaltManager(Path dataDir) {
        this(dataDir, "ip-salt.key");
    }

    public IpSaltManager(Path dataDir, String fileName) {
        this.file = dataDir.resolve(fileName);
    }

    public byte[] loadOrCreate() {
        try {
            if (Files.exists(file)) {
                String s = Files.readString(file, StandardCharsets.UTF_8).trim();
                return Base64.getDecoder().decode(s);
            }
            Files.createDirectories(file.getParent());
            byte[] salt = new byte[32];
            new SecureRandom().nextBytes(salt);
            Files.writeString(file, Base64.getEncoder().encodeToString(salt), StandardCharsets.UTF_8);
            return salt;
        } catch (Exception e) {
            byte[] salt = new byte[32];
            new SecureRandom().nextBytes(salt);
            return salt;
        }
    }
}
