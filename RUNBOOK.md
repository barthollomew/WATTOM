# WATTOM Runbook

## Quick Start

```bash
./gradlew shadowJar
java -jar build/libs/wattom.jar --pid <TARGET_PID>
```

Exit with Ctrl+C. All instrumentation is safely reverted.

## Command Reference

| Flag | Default | Description |
|------|---------|-------------|
| `--pid <PID>` | required | Target JVM process ID |
| `--trace <Class#method>` | off | Enable per-method p99 latency tracing |
| `--refresh <ms>` | 500 | Dashboard refresh interval |

## Scenario 1: High CPU

Symptoms: CPU usage near 100%, TLAB allocation rate is high.

1. Attach: `java -jar wattom.jar --pid <PID>`
2. If JVM CPU stays high, identify the hot call path via `jstack <PID>`.
3. Activate method tracer: `--trace com.example.HotService#process`
4. WATTOM shows p99 latency and call count in real time.
5. If p99 exceeds SLA, optimize the hot path or add caching.

## Scenario 2: Virtual Thread Starvation

Symptoms: Pinned virtual thread count is above zero and starvation ratio is high.

Cause: Synchronized blocks executed inside virtual threads hold the carrier thread and block other virtual threads.

1. Observe the dashboard for pinned virtual threads and carrier count.
2. High ratio means many carriers are occupied by pinned virtual threads.
3. Search for synchronized blocks in virtual thread code paths.
4. Replace synchronized blocks with reentrant locks.
5. Increase carrier pool parallelism if needed via system property.

## Scenario 3: Memory Leak

Symptoms: Old Gen memory grows steadily and never drops after GC.

1. Watch the heap section for Old Gen growth over time.
2. Trigger heap dump:
   `jcmd <PID> GC.heap_dump /tmp/heap.hprof`
3. Analyze the dump with Eclipse MAT or VisualVM.
4. Cross-reference with allocation rate in WATTOM to find leaking objects.

## Requirements and Limits

| Item | Requirement |
|------|-------------|
| Java version | 21+ on launcher and target JVM |
| JFR | Must not be disabled |
| Attach | Target must allow attach mechanism |
| Pinning threshold | Events below 20ms are not counted |
| Socket | `/tmp/wattom-{pid}.sock` cleaned up on exit |
| ByteBuddy trace | `Can-Retransform-Classes: true` required in agent manifest |
