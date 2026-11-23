# WATTOM

Your Java service is slow or frozen in production. If you restart it to add profiling flags you lose the bug.

WATTOM is a hot-attach diagnostic agent for Java 21. You do not need to restart your service. You do not need pre-configured flags.

Point WATTOM at a running Java process ID:

```bash
java -jar wattom.jar --pid 12345
```

WATTOM attaches instantly. It opens a live terminal dashboard showing CPU usage, memory allocation rates, deadlocked threads, and pinned Java 21 virtual threads. You can also trace p99 latency on any method using `--trace com.example.OrderService#process`.

Press Ctrl+C to detach. All code instrumentation reverts and sockets clean up safely.

## Live Dashboard Preview

```text
== WATTOM ========================================================
CPU       JVM 42%    [██████████----------]  Machine 65%
G1 Eden   128MB / 512MB    [██████--------------]
G1 Old    1024MB / 2048MB  [██████████----------]
Pinned VT count=2    starvation=25.0%  carriers=8
Alloc     4.2MB/sec
Tracer    com.example.OrderService#process: count=1280 p99=4.8ms avg=1.2ms
```

## How It Works

WATTOM uses a dual-mode JAR design. The launcher uses the JDK Attach API (`VirtualMachine.attach`) to inject the agent into the target JVM.

Inside the target process, the agent runs lightweight collectors:
- **JFR Event Stream**: Subscribes to `jdk.VirtualThreadPinned`, `jdk.CPULoad`, and `jdk.ObjectAllocationInNewTLAB`.
- **Scheduled Poller**: Samples heap usage and checks thread deadlocks every 250ms.
- **ByteBuddy Tracer**: Inlines lightweight advice bytecode for method p99 latency tracking when requested.
- **Unix Domain Socket**: Pushes binary length-prefixed metric frames to the local terminal UI over `/tmp/wattom-{pid}.sock`.

## Test Suite

WATTOM includes 10 test classes to ensure reliability:

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

- Java 21 or higher on both launcher and target JVM
- Flight recorder enabled on target JVM
- Attach mechanism enabled on target JVM

## Running Tests

```bash
./gradlew clean test
```

See [RUNBOOK.md](RUNBOOK.md) for diagnostic guides and [ARCHITECTURE.md](ARCHITECTURE.md) for design details.
