package io.wattom.tui;

import io.wattom.metrics.HeapPool;
import io.wattom.metrics.MetricSnapshot;
import java.util.Locale;

public final class AnsiRenderer {
    private AnsiRenderer() {}

    public static String render(MetricSnapshot s, int cols) { return build(s, cols, true); }

    public static String renderPlain(MetricSnapshot s, int cols) { return build(s, cols, false); }

    private static String build(MetricSnapshot s, int cols, boolean color) {
        var sb   = new StringBuilder(512);
        String c  = color ? AnsiCodes.CYAN      : "";
        String r  = color ? AnsiCodes.RESET     : "";
        String b  = color ? AnsiCodes.BOLD      : "";
        String y  = color ? AnsiCodes.YELLOW    : "";
        String re = color ? AnsiCodes.BRIGHT_RED : "";

        sb.append(b).append("== WATTOM ").append("=".repeat(Math.max(0, cols - 10)))
          .append(r).append('\n');

        if (s.cpuReady()) {
            int jvm = (int) (s.cpuJvmUser() * 100);
            int mac = (int) (s.cpuMachineTotal() * 100);
            sb.append(c).append("CPU       ").append(r)
              .append(String.format("JVM %-3d%%  %s  Machine %-3d%%",
                  jvm, BarChart.render(jvm, 100, 25, color), mac))
              .append('\n');
        } else {
            sb.append(c).append("CPU       ").append(r).append("--\n");
        }

        for (HeapPool p : s.heapPools()) {
            long usedMb = p.used() / (1024 * 1024);
            long maxMb  = Math.max(1, p.max() > 0 ? p.max() : p.committed()) / (1024 * 1024);
            sb.append(c).append(String.format("%-10s", shorten(p.name()))).append(r)
              .append(String.format("%s  %4dMB / %-4dMB",
                  BarChart.render(p.used(), Math.max(1, p.max()), 25, color), usedMb, maxMb))
              .append('\n');
        }

        String pinColor = s.starvationRatio() >= 0.75 ? re
                        : s.starvationRatio() >= 0.50 ? y : c;
        sb.append(pinColor).append("Pinned VT ").append(r)
          .append(String.format("count=%-3d  starvation=%.1f%%  carriers=%d",
              s.pinnedVtCount(), s.starvationRatio() * 100, s.carrierCount()))
          .append('\n');

        sb.append(c).append("Alloc     ").append(r)
          .append(fmtBytes(s.tlabBytesPerSec())).append("/sec\n");

        if (!s.deadlockChains().isEmpty()) {
            sb.append(re).append(b).append("DEADLOCK DETECTED").append(r).append('\n');
            for (String chain : s.deadlockChains())
                sb.append("  ").append(chain).append('\n');
        }

        if (!s.tracerLines().isEmpty()) {
            sb.append(c).append("Tracer    ").append(r).append('\n');
            for (String line : s.tracerLines())
                sb.append("  ").append(line).append('\n');
        }

        return sb.toString();
    }

    private static String shorten(String name) {
        int sp = name.indexOf(' ');
        return sp > 0 ? name.substring(0, sp) : name;
    }

    static String fmtBytes(long b) {
        if (b < 1024) return b + "B";
        if (b < 1024 * 1024) return String.format(Locale.ROOT, "%.1fKB", b / 1024.0);
        return String.format(Locale.ROOT, "%.1fMB", b / (1024.0 * 1024));
    }
}
