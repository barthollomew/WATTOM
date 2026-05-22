package io.wattom.diagnostics;

import io.wattom.metrics.HeapPool;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class HeapSampler {

    public List<HeapPool> sample() {
        var pools = new ArrayList<HeapPool>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() != MemoryType.HEAP) continue;
            var u = pool.getUsage();
            if (u == null) continue;
            pools.add(new HeapPool(pool.getName(), u.getUsed(), u.getCommitted(), u.getMax()));
        }
        pools.sort(Comparator.comparingInt(p -> order(p.name())));
        return List.copyOf(pools);
    }

    static int order(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("eden"))                         return 0;
        if (n.contains("survivor"))                     return 1;
        if (n.contains("old") || n.contains("tenured")) return 2;
        return 3;
    }
}
