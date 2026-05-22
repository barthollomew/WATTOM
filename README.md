# WATTOM

Hot-attach JVM diagnostic agent. Streams JFR events, detects virtual thread pinning and
carrier starvation, traces lock contention, instruments methods via ByteBuddy, and renders
a live ANSI dashboard. No runtime dependencies beyond ByteBuddy.

## Build

```bash
./gradlew shadowJar
```

Output: `build/libs/wattom.jar`

## Usage

```bash
java -jar build/libs/wattom.jar --pid <TARGET_PID>
java -jar build/libs/wattom.jar --pid <TARGET_PID> --trace com.example.Foo#bar --refresh 250
```

Press Ctrl+C to exit cleanly. The agent detaches and reverts all instrumentation.

## Requirements

- Java 21+ on both launcher and target JVM
- Target JVM must allow attach (`-XX:+DisableAttachMechanism` must not be set)
- JFR must not be disabled

See RUNBOOK.md for diagnostic scenarios and ARCHITECTURE.md for design details.

## Tests

```bash
./gradlew clean test
```
