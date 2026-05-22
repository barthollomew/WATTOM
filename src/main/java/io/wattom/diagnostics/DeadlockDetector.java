package io.wattom.diagnostics;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;

public final class DeadlockDetector {
    private final ThreadMXBean threads = ManagementFactory.getThreadMXBean();

    public List<String> detect() {
        long[] ids = threads.findDeadlockedThreads();
        if (ids == null) return List.of();
        ThreadInfo[] infos = threads.getThreadInfo(ids, true, true);
        var result = new ArrayList<String>(infos.length);
        for (ThreadInfo info : infos) {
            if (info == null) continue;
            var sb = new StringBuilder();
            sb.append(info.getThreadName())
              .append(" (id=").append(info.getThreadId()).append(")");
            if (info.getLockName() != null)
                sb.append(" waiting on ").append(info.getLockName());
            if (info.getLockOwnerName() != null)
                sb.append(" held by ").append(info.getLockOwnerName());
            result.add(sb.toString());
        }
        return List.copyOf(result);
    }
}
