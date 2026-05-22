package io.wattom.tracer;

import io.wattom.metrics.MethodLatencyStats;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LatencyRegistry {
    private LatencyRegistry() {}

    private static final Map<String, MethodLatencyStats> MAP = new ConcurrentHashMap<>();

    public static MethodLatencyStats getOrCreate(String key) {
        return MAP.computeIfAbsent(key, k -> new MethodLatencyStats(1024));
    }

    public static MethodLatencyStats get(String key) { return MAP.get(key); }

    public static Map<String, MethodLatencyStats> all() { return Map.copyOf(MAP); }

    public static void remove(String key) { MAP.remove(key); }
}
