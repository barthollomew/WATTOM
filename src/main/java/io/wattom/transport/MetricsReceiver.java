package io.wattom.transport;

import io.wattom.metrics.MetricSnapshot;
import java.io.DataInputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class MetricsReceiver implements AutoCloseable {
    private final Path socketPath;
    private final Consumer<MetricSnapshot> onSnapshot;
    private volatile ServerSocketChannel server;

    public MetricsReceiver(Path socketPath, Consumer<MetricSnapshot> onSnapshot) {
        this.socketPath = socketPath;
        this.onSnapshot = onSnapshot;
    }

    public void start() throws Exception {
        Files.deleteIfExists(socketPath);
        server = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        server.bind(UnixDomainSocketAddress.of(socketPath));
        Thread.ofPlatform().daemon(true).name("wattom-receiver").start(() -> {
            try {
                SocketChannel client = server.accept();
                DataInputStream in = new DataInputStream(Channels.newInputStream(client));
                while (!Thread.currentThread().isInterrupted()) {
                    onSnapshot.accept(MetricCodec.decode(in));
                }
            } catch (Throwable t) {
                // agent disconnected or server closed - normal shutdown path
            }
        });
    }

    @Override
    public void close() {
        try { if (server != null) server.close(); } catch (Throwable ignored) {}
        try { Files.deleteIfExists(socketPath); } catch (Throwable ignored) {}
    }
}
