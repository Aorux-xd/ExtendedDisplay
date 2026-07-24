package dev.ed.edcore.internal.license;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** SHA-256 JAR EDCore для EdCoreGate. */
public final class EdCoreJarChecksum {

    private EdCoreJarChecksum() {
    }

    public static String sha256Hex(Path jarPath) {
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(jarPath)) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    md.update(buf, 0, read);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            return "";
        }
    }
}
