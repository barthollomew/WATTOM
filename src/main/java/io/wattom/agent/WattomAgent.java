package io.wattom.agent;

import io.wattom.diagnostics.DeadlockDetector;
import io.wattom.diagnostics.HeapSampler;
import io.wattom.jfr.JfrController;
import io.wattom.metrics.MethodLatencyStats;
import io.wattom.metrics.MetricsStore;
import io.wattom.tracer.LatencyRegistry;
import io.wattom.tracer.MethodTracer;
import io.wattom.transport.MetricsPusher;

import java.lang.instrument.Instrumentation;
import java.util.Locale;
import java.util.concurrent.*;

public final class WattomAgent {
    private WattomAgent() {}

    private static volatile JfrController jfr;
    private static volatile MetricsPusher pusher;
    private static volatile MethodTracer  tracer;
    private static volatile ScheduledExecutorService poller;

    public static void premain(String args, Instrumentation inst) { agentmain(args, inst); }

    public static void agentmain(String args, Instrumentation inst) {
        try {
            AgentArgs cfg = AgentArgs.parse(args);
            MetricsStore store = new MetricsStore();

            jfr = new JfrController(store);
            jfr.start();

            poller = Executors.newSingleThreadScheduledExecutor(r ->
                Thread.ofPlatform().daemon(true).name("wattom-poller").unstarted(r));
            var heap = new HeapSampler();
            var dl   = new DeadlockDetector();
            poller.scheduleAtFixedRate(() -> {
                try {
                    store.updateHeapPools(heap.sample());
                    store.updateDeadlocks(dl.detect());
                    if (tracer != null) {
                        var lines = LatencyRegistry.all().entrySet().stream()
                            .map(e -> formatTracerLine(e.getKey(), e.getValue()))
                            .toList();
                        store.updateTracerLines(lines);
                    }
                } catch (Throwable t) {
                    System.err.println("[WATTOM] Poller error: " + t.getMessage());
                }
            }, 0, 250, TimeUnit.MILLISECONDS);

            if (cfg.traceSpec() != null) {
                java.net.URL loc = WattomAgent.class.getProtectionDomain()
                                               .getCodeSource().getLocation();
                inst.appendToSystemClassLoaderSearch(
                    new java.util.jar.JarFile(new java.io.File(loc.toURI())));
                tracer = new MethodTracer(inst, cfg.traceSpec());
                tracer.install();
            }

            pusher = new MetricsPusher(store, cfg.socketPath());
            pusher.start();

            Runtime.getRuntime().addShutdownHook(
                Thread.ofPlatform().unstarted(WattomAgent::detach));

        } catch (Throwable t) {
            System.err.println("[WATTOM] Agent startup error: " + t);
        }
    }

    static void detach() {
        try { if (jfr    != null) jfr.close();         } catch (Throwable ignored) {}
        try { if (poller != null) poller.shutdownNow(); } catch (Throwable ignored) {}
        try { if (tracer != null) tracer.reset();       } catch (Throwable ignored) {}
        try { if (pusher != null) pusher.close();       } catch (Throwable ignored) {}
    }

    private static String formatTracerLine(String key, MethodLatencyStats stats) {
        long p99 = stats.p99Nanos();
        String p99Str = p99 < 0 ? "--"
            : p99 < 1_000 ? p99 + "ns"
            : p99 < 1_000_000 ? String.format(Locale.ROOT, "%.1fus", p99 / 1_000.0)
            : String.format(Locale.ROOT, "%.1fms", p99 / 1_000_000.0);
        return String.format(Locale.ROOT, "%s: count=%d p99=%s", key, stats.count(), p99Str);
    }
}
