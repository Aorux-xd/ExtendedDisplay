package dev.ed.expansion.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SlListPing {

    private static final Pattern RE_ONLINE = Pattern.compile("\"online\"\\s*:\\s*(\\d+)");
    private static final Pattern RE_MAX = Pattern.compile("\"max\"\\s*:\\s*(\\d+)");
    private static final Pattern RE_VER = Pattern.compile("\"version\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern RE_MOTD_SIMPLE = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]*)\"");

    private SlListPing() {
    }

    public record Result(boolean online, int onlinePlayers, int maxPlayers, String motd, String version,
                         long pingMs) {
    }

    public static Optional<Result> ping(String host, int port, int timeoutMs) {
        long t0 = System.nanoTime();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            ByteArrayOutputStream handshake = new ByteArrayOutputStream();
            DataOutputStream h = new DataOutputStream(handshake);
            writeVarInt(h, 0x00);
            writeVarInt(h, 767);
            writeString(h, host.length() > 250 ? host.substring(0, 250) : host);
            h.writeShort(port & 0xFFFF);
            writeVarInt(h, 1);
            h.flush();
            writePacket(out, handshake.toByteArray());

            ByteArrayOutputStream statusReq = new ByteArrayOutputStream();
            DataOutputStream s = new DataOutputStream(statusReq);
            writeVarInt(s, 0x00);
            s.flush();
            writePacket(out, statusReq.toByteArray());

            int len = readVarInt(in);
            byte[] packet = new byte[len];
            in.readFully(packet);
            DataInputStream p = new DataInputStream(new ByteArrayInputStream(packet));
            int pid = readVarInt(p);
            if (pid != 0x00) {
                return Optional.empty();
            }
            String json = readString(p);
            long ping = (System.nanoTime() - t0) / 1_000_000L;
            return Optional.of(parseJson(json, ping));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Result parseJson(String json, long pingMs) {
        int on = firstInt(RE_ONLINE, json, 0);
        int max = firstInt(RE_MAX, json, 0);
        String ver = firstGroup(RE_VER, json, "");
        String motd = firstGroup(RE_MOTD_SIMPLE, json, "");
        if (motd.isEmpty() && json.contains("\"description\"")) {
            motd = "…";
        }
        return new Result(true, on, max, motd, ver, pingMs);
    }

    private static int firstInt(Pattern p, String s, int def) {
        Matcher m = p.matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : def;
    }

    private static String firstGroup(Pattern p, String s, String def) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : def;
    }

    private static void writePacket(DataOutputStream out, byte[] packetBody) throws IOException {
        writeVarInt(out, packetBody.length);
        out.write(packetBody);
    }

    private static void writeVarInt(DataOutputStream out, int value) throws IOException {
        int v = value;
        while ((v & ~0x7F) != 0) {
            out.writeByte((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.writeByte(v);
    }

    private static void writeString(DataOutputStream out, String str) throws IOException {
        byte[] b = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, b.length);
        out.write(b);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            read = in.readByte();
            int value = (read & 0x7F);
            result |= (value << (7 * numRead));
            numRead++;
            if (numRead > 5) {
                throw new IOException("VarInt too big");
            }
        } while ((read & 0x80) != 0);
        return result;
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = readVarInt(in);
        if (len < 0 || len > 2_000_000) {
            throw new IOException("bad string");
        }
        byte[] b = new byte[len];
        in.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}
