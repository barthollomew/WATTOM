package io.wattom.tui;

public final class BarChart {
    private BarChart() {}

    public static String render(long value, long max, int width, boolean color) {
        int fill = max <= 0 ? 0 : (int) Math.min(width, Math.round((double) value / max * width));
        var sb = new StringBuilder(width + 12);
        sb.append('[');
        if (color) sb.append(AnsiCodes.GREEN);
        sb.append("#".repeat(fill));
        if (color) sb.append(AnsiCodes.RESET);
        sb.append(" ".repeat(width - fill));
        sb.append(']');
        return sb.toString();
    }
}
