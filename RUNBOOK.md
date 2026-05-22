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

Symptoms: CPU bar near 100%, TLAB allocation rate is high.

1. Attach: `java -jar wattom.jar --pid <PID>`
2. If JVM CPU > 80% sustained, identify the hot call path via `jstack <PID>`.
3. Activate method tracer: `--trace com.example.HotService#process`
4. WATTOM shows p99 latency and call count for that method in real time.
5. If p99 > expected SLA, add caching or optimize the hot path.

## Scenario 2: Virtual Thread Starvation

Symptoms: `Pinned VT count > 0`, starvation ratio approaches 100%.

Cause: `synchronized` blocks executed inside `Thread.ofVirtual()` hold the carrier thread,
blocking other virtual threads from scheduling.

1. Observe: `Pinned VT  count=4  starvation=50%  carriers=8`
2. Ratio >= 50% means more than half the carrier pool is occupied by pinned VTs.
3. Search for `synchronized` inside virtual-thread code paths.
4. Fix: Replace `synchronized` with `java.util.concurrent.locks.ReentrantLock` (VT-friendly).
5. Temporary workaround to increase carrier pool:
   `-Djdk.virtualThreadScheduler.parallelism=32`

## Scenario 3: Memory Leak

Symptoms: Old Gen pool grows steadily and never GCs.

1. Watch the heap section for Old Gen growing > 10% over 5 minutes.
2. Trigger heap dump:
   `jcmd <PID> GC.heap_dump /tmp/heap-$(date +%s).hprof`
3. Analyze with Eclipse MAT (Dominator Tree view) or VisualVM.
4. Cross-reference with WATTOM's TLAB allocation rate: if bytes/sec is high but
   Old Gen grows, objects are surviving GC - investigate retention paths.

## Requirements and Limits

| Item | Requirement |
|------|-------------|
| Java version | 21+ (launcher and target JVM) |
| JFR | Must not be disabled (`-XX:-FlightRecorder` breaks pinning/CPU events) |
| Attach | Target must not set `-XX:+DisableAttachMechanism` |
| Pinning threshold | Events < 20ms are not counted (JFR default) |
| Socket | `/tmp/wattom-{pid}.sock` - cleaned up on exit |
| ByteBuddy trace | `Can-Retransform-Classes: true` required (set in agent manifest) |
