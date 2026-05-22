# WATTOM Architecture

## Overview

WATTOM is a dual-mode Java 21 agent (under 3.5 MB) for diagnosing virtual thread starvation,
lock contention, and allocation hotspots in production JVMs with minimal overhead.

## Component Diagram

```
  +------------------------------------------------------------------+
  |  USER TERMINAL                                                   |
  |  $ java -jar wattom.jar --pid 12345                              |
  |                                                                  |
  |  Launcher (main)                                                 |
  |   +- MetricsReceiver ---- UDS /tmp/wattom-12345.sock --------+  |
  |   +- AnsiRenderer (TUI loop every 500ms)                     |  |
  +------------------------------------------------------------------+
                                                               |
  +-------------------------------------------------------------v----+
  |  TARGET JVM (pid 12345)                                         |
  |                                                                  |
  |  WattomAgent.agentmain()                                         |
  |   +- JfrController                                               |
  |   |    +- PinningHandler  <- jdk.VirtualThreadPinned            |
  |   |    +- CpuLoadHandler  <- jdk.CPULoad                        |
  |   |    +- TlabHandler     <- jdk.ObjectAllocationInNewTLAB      |
  |   +- ScheduledPoller (250ms)                                     |
  |   |    +- HeapSampler   (MemoryPoolMXBean)                       |
  |   |    +- DeadlockDetector (ThreadMXBean)                        |
  |   |                   all |                                      |
  |   |              MetricsStore                                    |
  |   |                   |                                          |
  |   +- MetricsPusher ----+---- UDS -> Launcher                    |
  |   +- MethodTracer (ByteBuddy, optional via --trace)             |
  +------------------------------------------------------------------+
```

## Attach API Mechanism

The launcher uses `com.sun.tools.attach.VirtualMachine.attach(pid)` from the `jdk.attach`
module (bundled in the JDK) to load the agent JAR into a running JVM. Because the same JAR
is used for both the launcher and the agent, `loadAgent(selfJar, agentArgs)` is called with
the launcher's own path.

Safety guarantees:
1. All agent startup code is wrapped in `catch (Throwable)` - if WATTOM crashes during
   startup, the host JVM continues unaffected.
2. No strong references to application classes are retained - classloader leaks are prevented.
3. ByteBuddy transforms are reverted via `ResettableClassFileTransformer.reset()` on detach.
4. JFR `RecordingStream` is explicitly closed; heap/deadlock polling executor is shut down.
5. The UDS socket file is deleted on clean exit.

## IPC: Unix Domain Socket

A UDS at `/tmp/wattom-{pid}.sock` bridges the agent and the launcher. The launcher creates
the server socket before calling `loadAgent()`, eliminating any connection race. The wire
format is a binary length-prefix protocol (MetricCodec) - compact, zero-allocation parsing.

## ByteBuddy Instrumentation Safety

`disableClassFormatChanges()` constrains ByteBuddy to inline-only transformations (no new
fields or methods), required by the JVM spec for class retransformation. `@Advice` code is
inlined into the target method's bytecode at install time and fully reverted at reset time.
