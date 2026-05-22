package io.wattom.tui;

final class AnsiCodes {
    private AnsiCodes() {}
    static final String RESET        = "[0m";
    static final String BOLD         = "[1m";
    static final String RED          = "[31m";
    static final String GREEN        = "[32m";
    static final String YELLOW       = "[33m";
    static final String CYAN         = "[36m";
    static final String BRIGHT_RED   = "[91m";
    static final String CLEAR_SCREEN = "[2J";
    static final String HOME         = "[H";
    static final String HIDE_CURSOR  = "[?25l";
    static final String SHOW_CURSOR  = "[?25h";
}
