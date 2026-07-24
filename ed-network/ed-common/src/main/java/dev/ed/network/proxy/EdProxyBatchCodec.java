package dev.ed.network.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EdProxyBatchCodec {

    private EdProxyBatchCodec() {
    }

    public static byte[] encode(long sequence, List<ServerSnapshot> snapshots) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.writeInt(EdProxyChannel.MAGIC);
        out.writeLong(sequence);
        out.writeInt(snapshots.size());
        for (ServerSnapshot s : snapshots) {
            writeUtf(out, s.name());
            out.writeInt(s.online());
            out.writeInt(s.max());
            writeUtf(out, s.motd());
            out.writeLong(s.pingMs());
            writeUtf(out, s.version());
            out.writeBoolean(s.backendReachable());
        }
        out.flush();
        return bos.toByteArray();
    }

    public static Batch decode(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        int magic = in.readInt();
        if (magic != EdProxyChannel.MAGIC) {
            throw new IOException("bad magic");
        }
        long seq = in.readLong();
        int n = in.readInt();
        if (n < 0 || n > 4096) {
            throw new IOException("bad count");
        }
        List<ServerSnapshot> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String name = readUtf(in);
            int online = in.readInt();
            int max = in.readInt();
            String motd = readUtf(in);
            long ping = in.readLong();
            String ver = readUtf(in);
            boolean ok = in.readBoolean();
            list.add(new ServerSnapshot(name, online, max, motd, ping, ver, ok));
        }
        return new Batch(seq, Collections.unmodifiableList(list));
    }

    private static void writeUtf(DataOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);
        out.write(b);
    }

    private static String readUtf(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > 65535) {
            throw new IOException("bad utf len");
        }
        byte[] b = new byte[len];
        in.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    public record Batch(long sequence, List<ServerSnapshot> snapshots) {
    }
}
