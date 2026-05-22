package io.wattom.launcher;

public record CliArgs(String pid, String traceSpec, int refreshMs) {

    public static CliArgs parse(String[] args) {
        String pid = null, trace = null;
        int refresh = 500;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--pid"     -> pid   = args[++i];
                case "--trace"   -> trace = args[++i];
                case "--refresh" -> refresh = Integer.parseInt(args[++i]);
                default          -> {} // ignore unrecognised flags
            }
        }
        if (pid == null)
            throw new IllegalArgumentException(
                "Usage: wattom --pid <PID> [--trace Class#method] [--refresh <ms>]");
        return new CliArgs(pid, trace, refresh);
    }
}
