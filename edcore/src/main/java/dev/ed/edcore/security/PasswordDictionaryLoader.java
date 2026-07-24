package dev.ed.edcore.security;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PasswordDictionaryLoader {
    public enum Source { BUILTIN, FILE, URL }

    public Source source = Source.FILE;
    public String url = "";
    public List<String> urlWhitelist = List.of();
    public int urlMaxSizeMb = 5;

    public Set<String> load(Path dataDir, InputStream builtin) {
        Path file = dataDir.resolve("common-passwords.txt");
        try {
            Files.createDirectories(dataDir);
            if (!Files.exists(file) && builtin != null) {
                Files.copy(builtin, file);
            }
        } catch (Exception ignored) {
        }

        return switch (source) {
            case BUILTIN -> readFromStream(builtin);
            case FILE -> readFromFile(file);
            case URL -> readFromUrlThenCache(file);
        };
    }

    private Set<String> readFromFile(Path file) {
        try {
            if (!Files.exists(file)) {
                return Set.of();
            }
            return readLines(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Set.of();
        }
    }

    private Set<String> readFromStream(InputStream in) {
        try {
            if (in == null) {
                return Set.of();
            }
            return readLines(new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
        } catch (Exception e) {
            return Set.of();
        }
    }

    private Set<String> readFromUrlThenCache(Path cacheFile) {
        try {
            URI u = URI.create(url);
            if (!"https".equalsIgnoreCase(u.getScheme())) {
                return readFromFile(cacheFile);
            }
            String host = u.getHost() != null ? u.getHost().toLowerCase() : "";
            if (urlWhitelist == null || urlWhitelist.isEmpty()) {
                return readFromFile(cacheFile);
            }
            boolean allowed = urlWhitelist.stream()
                    .anyMatch(w -> host.equalsIgnoreCase(w) || host.endsWith("." + w.toLowerCase()));
            if (!allowed) {
                return readFromFile(cacheFile);
            }

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest req = HttpRequest.newBuilder(u).timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                return readFromFile(cacheFile);
            }
            String ct = resp.headers().firstValue("content-type").orElse("");
            if (!ct.toLowerCase().contains("text/plain")) {
                return readFromFile(cacheFile);
            }
            byte[] body = resp.body();
            if (body == null || body.length > urlMaxSizeMb * 1024L * 1024L) {
                return readFromFile(cacheFile);
            }
            Files.write(cacheFile, body);
            return readLines(new String(body, StandardCharsets.UTF_8).lines().toList());
        } catch (Exception e) {
            return readFromFile(cacheFile);
        }
    }

    private static Set<String> readLines(List<String> lines) {
        Set<String> out = new HashSet<>();
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            out.add(line);
        }
        return out;
    }
}
