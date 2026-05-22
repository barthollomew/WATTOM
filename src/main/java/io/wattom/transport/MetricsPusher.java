package io.wattom.transport;

import io.wattom.metrics.MetricsStore;
import java.io.DataOutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.util.concurrent.*;

public final class MetricsPusher implements AutoCloseable {
    private final MetricsStore store;
    private final Path socketPath;
    private final ScheduledExecutorService scheduler;
    private volatile SocketChannel channel;
    private volatile DataOutputStream out;

    public MetricsPusher(MetricsStore store, Path socketPath) {
        this.store = store;
        this.socketPath = socketPath;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r ->
            Thread.ofPlatform().daemon(true).name("wattom-pusher").unstarted(r));
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::push, 250, 250, TimeUnit.MILLISECONDS);
    }

    private void push() {
        try {
            if (channel == null || !channel.isConnected()) {
                channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                channel.connect(UnixDomainSocketAddress.of(socketPath));
                out = new DataOutputStream(Channels.newOutputStream(channel));
            }
            MetricCodec.encode(store.snapshot(), out);
        } catch (Throwable t) {
            channel = null;
            out = null;
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        try { if (channel != null) channel.close(); } catch (Throwable ignored) {}
    }
}
