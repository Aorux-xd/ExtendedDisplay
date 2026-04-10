package com.extendeddisplay.purpur;

import com.extendeddisplay.protocol.ServerStatusDto;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public final class DirectSlpPinger {
    public ServerStatusDto ping(String address, int timeoutMillis) {
        String[] parts = address.split(":", 2);
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            byte[] handshake = buildHandshake(host, port);
            writeVarInt(out, handshake.length);
            out.write(handshake);

            out.writeByte(1);
            out.writeByte(0);
            out.flush();

            readVarInt(in); // packet length
            int packetId = readVarInt(in);
            if (packetId != 0x00) {
                return ServerStatusDto.fallbackOffline();
            }
            int jsonLength = readVarInt(in);
            byte[] jsonBytes = new byte[jsonLength];
            in.readFully(jsonBytes);
            String json = new String(jsonBytes, java.nio.charset.StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject players = root.has("players") && root.get("players").isJsonObject()
                    ? root.getAsJsonObject("players")
                    : null;
            int online = players != null && players.has("online") ? players.get("online").getAsInt() : 0;
            int max = players != null && players.has("max") ? players.get("max").getAsInt() : 0;
            return new ServerStatusDto(true, Math.max(0, online), Math.max(0, max), address);
        } catch (Exception ignored) {
            return new ServerStatusDto(false, 0, 0, address);
        }
    }

    private static byte[] buildHandshake(String host, int port) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        writeVarInt(out, 0x00);
        writeVarInt(out, 47);
        writeString(out, host);
        out.writeShort(port & 0xFFFF);
        writeVarInt(out, 1);
        return bytes.toByteArray();
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0L) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value & 0x7F);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;
        do {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;
            position += 7;
            if (position > 35) {
                throw new IOException("VarInt too big");
            }
        } while ((currentByte & 0x80) == 0x80);
        return value;
    }
}
