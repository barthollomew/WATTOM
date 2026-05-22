package io.wattom.agent;

import java.lang.instrument.Instrumentation;

public final class WattomAgent {
    private WattomAgent() {}

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        // lifecycle wired in Task 8
    }
}
