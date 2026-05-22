package io.wattom.tui;

public final class Terminal {
    private Terminal() {}

    public static int columns() {
        String v = System.getenv("COLUMNS");
        if (v != null) {
            try { return Math.max(40, Integer.parseInt(v.trim())); }
            catch (NumberFormatException ignored) {}
        }
        return 100;
    }

    public static void hideCursor()  { System.out.print(AnsiCodes.HIDE_CURSOR); System.out.flush(); }
    public static void showCursor()  { System.out.print(AnsiCodes.SHOW_CURSOR); System.out.flush(); }
    public static void clearScreen() { System.out.print(AnsiCodes.CLEAR_SCREEN + AnsiCodes.HOME); }
}
