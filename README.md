# WATTOM

Hot-attach JVM diagnostic agent for production JVMs. No restart required.

WATTOM streams JFR events, detects virtual thread pinning and carrier starvation, tracks heap and TLAB allocation rates, detects deadlocks, and instruments individual methods for real-time p99 latency. It renders a live ANSI dashboard in your terminal.

Single JAR. No runtime dependencies beyond ByteBuddy.

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

## Architecture

WATTOM is a dual-mode Java 21 agent. The launcher uses `VirtualMachine.attach(pid)` to load the agent JAR into a target JVM. 

Inside the target JVM, the agent starts several background routines:
- Streams JFR events including `jdk.VirtualThreadPinned`, `jdk.CPULoad`, and `jdk.ObjectAllocationInNewTLAB`.
- Samples heap memory and checks for deadlocks every 250ms.
- Traces method p99 latency using ByteBuddy `@Advice` when enabled.
- Pushes length-prefixed binary frames over a Unix domain socket at `/tmp/wattom-{pid}.sock` to the ANSI TUI.

## Test Suite

WATTOM includes 10 test classes to verify correctness:

Unit tests:
- `DeadlockDetectorTest`
- `PinningHandlerTest`
- `MethodLatencyStatsTest`
- `MethodTracerTest`
- `MetricCodecTest`
- `AnsiRendererTest`
- `BarChartTest`

Integration tests:
- `ChaosAttachIT`
- `NestedLockChaosIT`
- `PinningDetectionIT`

## Requirements

- Java 21+ on both the launcher and the target JVM
- JFR must be enabled
- Target JVM must allow attach

## Tests

```bash
./gradlew clean test
```

See [RUNBOOK.md](RUNBOOK.md) for diagnostic scenarios and [ARCHITECTURE.md](ARCHITECTURE.md) for design details.
