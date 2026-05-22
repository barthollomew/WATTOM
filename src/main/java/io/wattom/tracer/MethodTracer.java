package io.wattom.tracer;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.agent.builder.ResettableClassFileTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;
import java.lang.instrument.Instrumentation;

public final class MethodTracer {
    private final Instrumentation inst;
    private final TraceSpec spec;
    private volatile ResettableClassFileTransformer transformer;

    public MethodTracer(Instrumentation inst, TraceSpec spec) {
        this.inst = inst;
        this.spec = spec;
    }

    public void install() {
        LatencyRegistry.getOrCreate(spec.key());
        transformer = new AgentBuilder.Default()
            .disableClassFormatChanges()
            .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
            .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
            .ignore(ElementMatchers.nameStartsWith("net.bytebuddy."))
            .type(ElementMatchers.named(spec.className()))
            .transform((builder, type, loader, module, domain) ->
                builder.visit(Advice.to(LatencyAdvice.class)
                                    .on(ElementMatchers.named(spec.methodName()))))
            .installOn(inst);
    }

    public void reset() {
        try {
            if (transformer != null)
                transformer.reset(inst, AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);
            LatencyRegistry.remove(spec.key());
        } catch (Throwable t) {
            System.err.println("[WATTOM] Tracer reset error: " + t.getMessage());
        }
    }
}
