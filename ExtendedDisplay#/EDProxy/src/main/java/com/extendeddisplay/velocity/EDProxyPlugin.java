package com.extendeddisplay.velocity;

import com.extendeddisplay.protocol.EdProtocolConstants;
import com.extendeddisplay.protocol.ProtocolCodec;
import com.extendeddisplay.protocol.ServerStatusDto;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Plugin(
        id = "edproxy",
        name = "EDProxy",
        version = "1.0",
        description = "ExtendedDisplay proxy status provider")
public final class EDProxyPlugin {

    private static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from(EdProtocolConstants.CHANNEL_ID);

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private ServerStateService stateService;
    private EDProxyConfig config;

    @Inject
    public EDProxyPlugin(
            ProxyServer proxyServer,
            Logger logger,
            @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            config = EDProxyConfig.load(dataDirectory);
        } catch (IOException exception) {
            logger.warning("Failed to load EDProxy config.yml, using defaults.");
            config = new EDProxyConfig(1, false, java.util.Collections.emptySet());
        }
        this.stateService =
                new ServerStateService(proxyServer, config.blacklistedServers(), config.cacheTtlSeconds());
        proxyServer.getChannelRegistrar().register(CHANNEL);
        logger.info("EDProxy initialized on channel " + EdProtocolConstants.CHANNEL_ID);
        proxyServer.getEventManager().register(this, this);

        proxyServer.getCommandManager().register("edproxydebug", new SimpleCommand() {
            @Override
            public void execute(Invocation invocation) {
                String[] args = invocation.arguments();
                if (args.length != 1 || (!"true".equalsIgnoreCase(args[0]) && !"false".equalsIgnoreCase(args[0]))) {
                    invocation.source().sendMessage(Component.text("Usage: /edproxydebug <true|false>"));
                    return;
                }
                boolean enabled = Boolean.parseBoolean(args[0]);
                config = new EDProxyConfig(config.cacheTtlSeconds(), enabled, config.blacklistedServers());
                try {
                    config.save(dataDirectory);
                } catch (IOException e) {
                    invocation.source().sendMessage(Component.text("Failed to persist EDProxy debug config: " + e.getMessage()));
                    return;
                }
                invocation.source().sendMessage(Component.text("EDProxy debug mode: " + enabled));
            }
        });
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(CHANNEL)) {
            return;
        }
        String request = ProtocolCodec.decodeString(event.getData()).trim();
        debug("Received plugin message: " + request
                + " | source=" + event.getSource().getClass().getSimpleName()
                + " | target=" + event.getTarget().getClass().getSimpleName());
        String response = handleRequest(request);
        if (response == null || response.isBlank()) {
            debug("Ignored unsupported request payload: " + request);
            return;
        }

        byte[] encoded = ProtocolCodec.encodeString(response);
        boolean sent = false;
        if (event.getSource() instanceof ChannelMessageSink sourceSink) {
            sent = trySend(sourceSink, encoded, "source", response);
        }
        if (!sent && event.getTarget() instanceof ChannelMessageSink targetSink) {
            sent = trySend(targetSink, encoded, "target", response);
        }
        if (!sent) {
            debugWarn("Could not send response (no ChannelMessageSink on source/target)");
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    private String handleRequest(String request) {
        if (request.equalsIgnoreCase(EdProtocolConstants.REQUEST_LIST_ALL)) {
            debug("Processing list_all request");
            Map<String, ServerStatusDto> all = stateService.getAllStatuses();
            return ProtocolCodec.encodeAllResponse(all);
        }
        if (request.startsWith(EdProtocolConstants.REQUEST_GET_PREFIX)) {
            String server = request.substring(EdProtocolConstants.REQUEST_GET_PREFIX.length()).trim();
            debug("Processing get request for server: " + server);
            Optional<ServerStatusDto> status = stateService.getStatus(server);
            return ProtocolCodec.encodeSingleResponse(server, status.orElse(ServerStatusDto.fallbackOffline()));
        }
        return null;
    }

    private void debug(String msg) {
        if (config != null && config.debug()) {
            logger.info("[DEBUG][EDProxy] " + msg);
        }
    }

    private void debugWarn(String msg) {
        if (config != null && config.debug()) {
            logger.warning("[DEBUG][EDProxy] " + msg);
        }
    }

    private boolean trySend(ChannelMessageSink sink, byte[] encoded, String direction, String response) {
        try {
            sink.sendPluginMessage(CHANNEL, encoded);
            debug("Sent response via " + direction + ": " + response);
            return true;
        } catch (IllegalStateException ex) {
            debugWarn("Failed sending via " + direction + ": " + ex.getMessage());
            return false;
        }
    }
}
