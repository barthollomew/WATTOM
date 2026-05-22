package io.wattom.metrics;

public record HeapPool(String name, long used, long committed, long max) {}
