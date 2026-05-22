package io.wattom.agent;

import io.wattom.tracer.TraceSpec;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record AgentArgs(Path socketPath, TraceSpec traceSpec) {

    public static AgentArgs parse(String args) {
        if (args == null || args.isBlank())
            throw new IllegalArgumentException("Agent args must specify socketPath");
        Map<String, String> map = new HashMap<>();
        for (String kv : args.split(",")) {
            int eq = kv.indexOf('=');
            if (eq > 0) map.put(kv.substring(0, eq).trim(), kv.substring(eq + 1).trim());
        }
        String sp = map.get("socketPath");
        if (sp == null) throw new IllegalArgumentException("Missing socketPath in agent args");
        TraceSpec spec = map.containsKey("trace") ? TraceSpec.parse(map.get("trace")) : null;
        return new AgentArgs(Path.of(sp), spec);
    }
}
