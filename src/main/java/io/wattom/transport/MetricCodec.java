package io.wattom.transport;

import io.wattom.metrics.HeapPool;
import io.wattom.metrics.MetricSnapshot;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MetricCodec {
    static final byte VERSION = 1;
    private MetricCodec() {}

    public static void encode(MetricSnapshot s, DataOutputStream out) throws IOException {
        out.writeByte(VERSION);
        out.writeLong(s.timestampNanos());
        out.writeFloat(s.cpuJvmUser());
        out.writeFloat(s.cpuJvmSystem());
        out.writeFloat(s.cpuMachineTotal());
        out.writeInt(s.heapPools().size());
        for (HeapPool p : s.heapPools()) {
            writeStr(p.name(), out);
            out.writeLong(p.used());
            out.writeLong(p.committed());
            out.writeLong(p.max());
        }
        out.writeInt(s.pinnedVtCount());
        out.writeDouble(s.starvationRatio());
        out.writeInt(s.carrierCount());
        out.writeLong(s.tlabBytesPerSec());
        writeStrList(s.deadlockChains(), out);
        writeStrList(s.tracerLines(), out);
        out.writeBoolean(s.cpuReady());
        out.flush();
    }

    public static MetricSnapshot decode(DataInputStream in) throws IOException {
        byte ver = in.readByte();
        if (ver != VERSION) throw new IOException("Unknown WATTOM protocol version: " + ver);
        long ts     = in.readLong();
        float u     = in.readFloat();
        float sys   = in.readFloat();
        float tot   = in.readFloat();
        int poolCt  = in.readInt();
        var pools   = new ArrayList<HeapPool>(poolCt);
        for (int i = 0; i < poolCt; i++)
            pools.add(new HeapPool(readStr(in), in.readLong(), in.readLong(), in.readLong()));
        int pinned  = in.readInt();
        double starve = in.readDouble();
        int carriers  = in.readInt();
        long tlab     = in.readLong();
        List<String> dl = readStrList(in);
        List<String> tr = readStrList(in);
        boolean cpuOk  = in.readBoolean();
        return new MetricSnapshot(ts, u, sys, tot, List.copyOf(pools),
                                  pinned, starve, carriers, tlab, dl, tr, cpuOk);
    }

    private static void writeStr(String s, DataOutputStream out) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);
        out.write(b);
    }

    private static String readStr(DataInputStream in) throws IOException {
        return new String(in.readNBytes(in.readInt()), StandardCharsets.UTF_8);
    }

    private static void writeStrList(List<String> list, DataOutputStream out) throws IOException {
        out.writeInt(list.size());
        for (String s : list) writeStr(s, out);
    }

    private static List<String> readStrList(DataInputStream in) throws IOException {
        int n = in.readInt();
        var list = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) list.add(readStr(in));
        return List.copyOf(list);
    }
}
