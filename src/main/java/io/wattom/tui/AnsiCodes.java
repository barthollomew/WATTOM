package io.wattom.tui;

final class AnsiCodes {
    private AnsiCodes() {}
    static final String RESET        = "\u001b[0m";
    static final String BOLD         = "\u001b[1m";
    static final String GREEN        = "\u001b[32m";
    static final String YELLOW       = "\u001b[33m";
    static final String CYAN         = "\u001b[36m";
    static final String BRIGHT_RED   = "\u001b[91m";
    static final String CLEAR_SCREEN = "\u001b[2J";
    static final String HOME         = "\u001b[H";
    static final String HIDE_CURSOR  = "\u001b[?25l";
    static final String SHOW_CURSOR  = "\u001b[?25h";
}
