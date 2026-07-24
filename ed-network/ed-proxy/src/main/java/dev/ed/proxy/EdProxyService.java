package dev.ed.proxy;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.ed.edcore.api.EDCoreProvider;
import dev.ed.network.proxy.EdProxyBatchCodec;
import dev.ed.network.proxy.ServerSnapshot;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;

final class EdProxyService {

    private static final MinecraftChannelIdentifier CH = MinecraftChannelIdentifier.create("ed", "proxy");

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private volatile int collectIntervalSeconds = 5;
    private volatile int pingTimeoutMillis = 2500;
    private volatile boolean debug = false;

    private final AtomicLong sequence = new AtomicLong();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    EdProxyService(ProxyServer server, Logger logger, Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    void reloadConfig() {
        Path cfg = dataDirectory.resolve("config.yml");
        try {
            if (!Files.isRegularFile(cfg)) {
                try (InputStream in = EdProxyPlugin.class.getClassLoader().getResourceAsStream("config.yml")) {
                    if (in != null) {
                        Files.createDirectories(dataDirectory);
                        Files.copy(in, cfg);
                    }
                }
            }
            if (!Files.isRegularFile(cfg)) {
                return;
            }
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(Files.readString(cfg));
            if (map != null) {
                Object i = map.get("collect-interval-seconds");
                if (i instanceof Number n) {
                    collectIntervalSeconds = Math.max(1, n.intValue());
                }
                Object t = map.get("ping-timeout-millis");
                if (t instanceof Number n) {
                    pingTimeoutMillis = Math.max(500, n.intValue());
                }
                Object d = map.get("debug");
                if (d instanceof Boolean b) {
                    debug = b;
                }
            }
        } catch (Exception e) {
            logger.warn("EDProxy: не удалось прочитать config.yml: {}", e.toString());
        }
    }

    int collectIntervalSeconds() {
        return collectIntervalSeconds;
    }

    void shutdown() {
        shutdown.set(true);
    }

    void collectAndBroadcast() {
        if (shutdown.get() || !EDCoreProvider.isReady()) {
            return;
        }
        List<RegisteredServer> servers = new ArrayList<>(server.getAllServers());
        if (servers.isEmpty()) {
            return;
        }
        PingOptions opts = PingOptions.builder()
                .timeout(Duration.ofMillis(pingTimeoutMillis))
                .build();

        List<CompletableFuture<ServerSnapshot>> futures = servers.stream()
                .map(rs -> snapshotFor(rs, opts))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((v, err) -> {
                    if (shutdown.get() || !EDCoreProvider.isReady()) {
                        return;
                    }
                    if (err != null) {
                        if (debug) {
                            logger.debug("EDProxy: ошибка сбора: {}", err.toString());
                        }
                        return;
                    }
                    List<ServerSnapshot> list = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());
                    long seq = sequence.incrementAndGet();
                    try {
                        byte[] payload = EdProxyBatchCodec.encode(seq, list);
                        for (RegisteredServer rs : servers) {
                            try {
                                rs.sendPluginMessage(CH, payload);
                            } catch (Exception ex) {
                                if (debug) {
                                    logger.debug("EDProxy: не удалось отправить на {}: {}", rs.getServerInfo().getName(), ex.toString());
                                }
                            }
                        }
                        var core = EDCoreProvider.get();
                        core.getStorage().put("edproxy.lastPayloadB64", Base64.getEncoder().encodeToString(payload));
                        core.getStorage().put("edproxy.lastSequence", Long.toString(seq));
                        core.getStorage().save();
                        if (debug) {
                            logger.info("EDProxy: рассылка seq={} серверов={}", seq, list.size());
                        }
                    } catch (Exception e) {
                        if (debug) {
                            logger.debug("EDProxy: кодирование/кэш: {}", e.toString());
                        }
                    }
                });
    }

    private CompletableFuture<ServerSnapshot> snapshotFor(RegisteredServer rs, PingOptions opts) {
        String name = rs.getServerInfo().getName();
        int onProxy = (int) server.getAllPlayers().stream()
                .filter(p -> p.getCurrentServer()
                        .map(sc -> sc.getServerInfo().getName().equalsIgnoreCase(name))
                        .orElse(false))
                .count();

        long start = System.nanoTime();
        CompletableFuture<ServerPing> pingFuture;
        try {
            pingFuture = rs.ping(opts);
        } catch (Exception e) {
            if (debug) {
                logger.debug("EDProxy: ping init error for {}: {}", name, e.toString());
            }
            return CompletableFuture.completedFuture(new ServerSnapshot(name, 0, 0, "", -1L, "", false));
        }
        return pingFuture.completeOnTimeout(null, Math.max(500, pingTimeoutMillis), TimeUnit.MILLISECONDS).handle((ping, ex) -> {
            long pingMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            if (ex != null || ping == null) {
                if (debug && ex != null) {
                    logger.debug("EDProxy: ping error for {}: {}", name, ex.toString());
                }
                return new ServerSnapshot(name, 0, 0, "", -1L, "", false);
            }
            int max = ping.getPlayers().map(ServerPing.Players::getMax).orElse(0);
            int onlinePing = ping.getPlayers().map(ServerPing.Players::getOnline).orElse(onProxy);
            int online = Math.max(onProxy, onlinePing);
            String motd = "";
            var component = ping.getDescriptionComponent();
            if (component != null) {
                try {
                    motd = plain.serialize(component);
                } catch (Exception ignored) {
                    motd = "";
                }
            }
            String ver = ping.getVersion() != null ? ping.getVersion().getName() : "";
            return new ServerSnapshot(name, online, max, motd, pingMs, ver, true);
        });
    }
}
