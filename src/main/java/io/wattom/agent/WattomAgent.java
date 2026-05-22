package io.wattom.agent;

import io.wattom.diagnostics.DeadlockDetector;
import io.wattom.diagnostics.HeapSampler;
import io.wattom.jfr.JfrController;
import io.wattom.metrics.MetricsStore;
import io.wattom.tracer.MethodTracer;
import io.wattom.transport.MetricsPusher;

import java.lang.instrument.Instrumentation;
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
}
