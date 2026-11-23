# WATTOM Architecture

## Overview

WATTOM is a dual-mode Java 21 agent under 3.5 MB. It diagnoses virtual thread starvation, lock contention, and allocation hotspots in production JVMs with low overhead.

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

The launcher uses `com.sun.tools.attach.VirtualMachine.attach(pid)` from the `jdk.attach` module to load the agent JAR into a running JVM. The same JAR is used for both the launcher and the agent.

Safety guarantees:
1. All agent startup code is wrapped in try catch blocks so host JVM runs safely if agent startup fails.
2. No strong references to application classes are retained. This prevents classloader leaks.
3. ByteBuddy transforms are reset on detach.
4. JFR recording stream is closed and polling executors shut down.
5. Socket files are deleted on exit.

## IPC: Unix Domain Socket

A Unix domain socket at `/tmp/wattom-{pid}.sock` bridges the agent and the launcher. The launcher creates the server socket before calling load agent. This removes any connection race. The wire format uses metric codec for compact zero allocation parsing.

## ByteBuddy Instrumentation Safety

Class format changes are disabled. This constrains ByteBuddy to inline transformations without adding new fields or methods. Advice code is inlined into target methods at install time and fully reverted at reset time.
