package io.wattom.tracer;

public record TraceSpec(String className, String methodName) {

    public static TraceSpec parse(String spec) {
        int sep = spec.indexOf('#');
        if (sep < 0) sep = spec.lastIndexOf('.');
        if (sep < 0 || sep == spec.length() - 1)
            throw new IllegalArgumentException("Use ClassName#method format: " + spec);
        return new TraceSpec(spec.substring(0, sep), spec.substring(sep + 1));
    }

    public String key() { return className + "#" + methodName; }
}
