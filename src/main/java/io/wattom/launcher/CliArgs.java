package io.wattom.launcher;

public record CliArgs(String pid, String traceSpec, int refreshMs, boolean help) {
    public static final String USAGE = """
        Usage: java -jar wattom.jar --pid <PID> [--trace Class#method] [--refresh <ms>]

        Options:
          --pid <PID>              Target JVM process ID (required)
          --trace <Class#method>   Track p99 latency for a method
          --refresh <ms>           Dashboard refresh interval in ms (default: 500)
          -h, --help               Show this help message""";

    public static CliArgs parse(String[] args) {
        String pid = null, trace = null;
        int refresh = 500;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-h", "--help" -> {
                    return new CliArgs(null, null, refresh, true);
                }
                case "--pid" -> pid = requireValue(args, ++i, "--pid");
                case "--trace" -> trace = requireValue(args, ++i, "--trace");
                case "--refresh" -> {
                    refresh = Integer.parseInt(requireValue(args, ++i, "--refresh"));
                    if (refresh <= 0)
                        throw new IllegalArgumentException("--refresh must be positive");
                }
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }
        if (pid == null || pid.isBlank())
            throw new IllegalArgumentException("Missing required --pid option");
        return new CliArgs(pid, trace, refresh, false);
    }

    private static String requireValue(String[] args, int index, String flag) {
        if (index >= args.length)
            throw new IllegalArgumentException("Missing value for " + flag);
        return args[index];
    }
}
