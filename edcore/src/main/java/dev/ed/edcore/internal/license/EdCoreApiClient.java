package dev.ed.edcore.internal.license;

import dev.ed.edcore.api.logging.EDLogger;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP-клиент API edcore.vercel.app.
 */
public final class EdCoreApiClient {

    private final String baseUrl;
    private final EDLogger log;
    private final HttpClient http;

    public EdCoreApiClient(String baseUrl, EDLogger log) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.log = log;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public boolean registerServer(String serverId, String hardwareId) {
        String body = json(Map.of(
                "server_id", serverId,
                "hardware_id", hardwareId
        ));
        Optional<String> response = post("/api/v1/server/register", body);
        return response.map(r -> r.contains("\"registered\":true") || r.contains("\"registered\": true")).orElse(false);
    }

    public VerifyResponse verifyLicense(String licenseKey, String serverId, String hardwareId) {
        String body = json(Map.of(
                "license_key", licenseKey,
                "server_id", serverId,
                "hardware_id", hardwareId
        ));
        Optional<String> response = post("/api/v1/license/verify", body);
        if (response.isEmpty()) {
            return VerifyResponse.fail("no_response");
        }
        String r = response.get();
        boolean valid = r.contains("\"valid\":true") || r.contains("\"valid\": true");
        if (!valid) {
            return VerifyResponse.fail("invalid");
        }
        String owner = extractString(r, "owner");
        String status = extractString(r, "status");
        return VerifyResponse.ok(owner, status);
    }

    public Optional<String> fetchUpdates(String licenseKey, String serverId, List<String> plugins) {
        try {
            String pluginsJson = URLEncoder.encode(toJsonArray(plugins), StandardCharsets.UTF_8);
            String query = "license_key=" + enc(licenseKey)
                    + "&server_id=" + enc(serverId)
                    + "&plugins=" + pluginsJson;
            URI uri = URI.create(baseUrl + "/api/v1/plugins/updates?" + query);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .header("Accept", "application/json")
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return Optional.of(resp.body());
            }
            log.warn("EDCore API updates: HTTP " + resp.statusCode());
        } catch (Exception e) {
            log.warn("EDCore API updates: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<byte[]> downloadPlugin(String licenseKey, String serverId, String pluginName, String version) {
        try {
            String query = "license_key=" + enc(licenseKey)
                    + "&server_id=" + enc(serverId)
                    + "&plugin_name=" + enc(pluginName)
                    + "&version=" + enc(version);
            URI uri = URI.create(baseUrl + "/api/v1/plugins/download?" + query);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(120))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return Optional.of(resp.body());
            }
            log.warn("EDCore API download " + pluginName + ": HTTP " + resp.statusCode());
        } catch (Exception e) {
            log.warn("EDCore API download: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<String> post(String path, String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                return Optional.of(resp.body());
            }
            log.warn("EDCore API " + path + ": HTTP " + resp.statusCode() + " — " + resp.body());
        } catch (Exception e) {
            log.warn("EDCore API " + path + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "https://edcore.vercel.app";
        }
        String u = url.trim();
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    private static String json(Map<String, String> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : fields.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(e.getKey())).append("\":\"").append(escape(e.getValue())).append('"');
        }
        sb.append('}');
        return sb.toString();
    }

    private static String toJsonArray(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(escape(items.get(i))).append('"');
        }
        sb.append(']');
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractString(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int i = json.indexOf(needle);
        if (i < 0) {
            return "";
        }
        int start = i + needle.length();
        int end = json.indexOf('"', start);
        return end > start ? json.substring(start, end) : "";
    }

    public record VerifyResponse(boolean valid, String owner, String status, String error) {
        static VerifyResponse ok(String owner, String status) {
            return new VerifyResponse(true, owner, status, null);
        }

        static VerifyResponse fail(String error) {
            return new VerifyResponse(false, "", "", error);
        }
    }
}
