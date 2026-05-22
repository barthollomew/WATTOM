package io.wattom.tracer;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MethodTracerTest {

    static class Target {
        static void work() {
            try { Thread.sleep(5); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    @Test
    void tracerRecordsLatencyAndReverts() throws Exception {
        var inst = ByteBuddyAgent.install();
        var spec = TraceSpec.parse(
            "io.wattom.tracer.MethodTracerTest$Target#work");

        var tracer = new MethodTracer(inst, spec);
        tracer.install();

        for (int i = 0; i < 10; i++) Target.work();

        var stats = LatencyRegistry.get(spec.key());
        assertNotNull(stats, "LatencyRegistry should have stats after instrumentation");
        assertTrue(stats.count() > 0, "Expected recorded calls");
        assertTrue(stats.p99Nanos() > 0, "Expected positive p99");

        tracer.reset();
        assertDoesNotThrow(Target::work);
        assertNull(LatencyRegistry.get(spec.key()));
    }
}
