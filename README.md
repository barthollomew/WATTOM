# WATTOM

Hot-attach JVM diagnostic agent for production JVMs. No restart required.

Streams JFR events, detects virtual thread pinning and carrier starvation, tracks heap
and TLAB allocation rates, detects deadlocks, and instruments individual methods for
real-time p99 latency. Renders a live ANSI dashboard in your terminal.

Single JAR, no runtime dependencies beyond ByteBuddy.

## Install

```bash
./gradlew shadowJar
# output: build/libs/wattom.jar
```

## Usage

Attach to a running JVM by PID:

```bash
java -jar wattom.jar --pid <PID>
```

Trace a specific method and set a custom refresh rate:

```bash
java -jar wattom.jar --pid <PID> --trace com.example.OrderService#process --refresh 250
```

Press Ctrl+C to detach. All instrumentation is reverted and the socket is cleaned up.

## Flags

| Flag | Default | Description |
|------|---------|-------------|
| `--pid` | required | Target JVM process ID |
| `--trace` | off | Method to trace, format: `ClassName#methodName` |
| `--refresh` | 500 | Dashboard refresh interval in milliseconds |

## Requirements

- Java 21+ on both the launcher and the target JVM
- JFR must be enabled (do not set `-XX:-FlightRecorder`)
- Target JVM must allow attach (do not set `-XX:+DisableAttachMechanism`)

## Tests

```bash
./gradlew clean test
```

See [RUNBOOK.md](RUNBOOK.md) for diagnostic scenarios and [ARCHITECTURE.md](ARCHITECTURE.md) for design details.
