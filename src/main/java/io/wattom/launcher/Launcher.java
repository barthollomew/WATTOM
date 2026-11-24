package io.wattom.launcher;

import io.wattom.metrics.MetricSnapshot;
import io.wattom.transport.MetricsReceiver;
import io.wattom.tui.AnsiRenderer;
import io.wattom.tui.Terminal;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) throws Exception {
        CliArgs cli;
        try {
            cli = CliArgs.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(CliArgs.USAGE);
            System.exit(1);
            return;
        }
        if (cli.help()) {
            System.out.println(CliArgs.USAGE);
            return;
        }

        String selfJar = new File(
            Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        ).getAbsolutePath();

        Path socketPath = Path.of("/tmp/wattom-" + cli.pid() + ".sock");
        AtomicReference<MetricSnapshot> latest = new AtomicReference<>(MetricSnapshot.empty());

        MetricsReceiver receiver = new MetricsReceiver(socketPath, latest::set);
        receiver.start();

        var agentArgs = new StringBuilder("socketPath=").append(socketPath);
        if (cli.traceSpec() != null) agentArgs.append(",trace=").append(cli.traceSpec());

        AttachHelper.attach(cli.pid(), selfJar, agentArgs.toString());

        Terminal.hideCursor();
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> {
            Terminal.showCursor();
            receiver.close();
        }));

        int cols = Terminal.columns();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Terminal.clearScreen();
                System.out.print(AnsiRenderer.render(latest.get(), cols));
                System.out.flush();
                Thread.sleep(cli.refreshMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Throwable t) {
                System.err.println("[WATTOM] Render error: " + t.getMessage());
            }
        }
    }
}
