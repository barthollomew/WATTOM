package io.wattom.tui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BarChartTest {
    @Test void emptyOnZero()    { assertEquals("[          ]", BarChart.render(0, 100, 10, false)); }
    @Test void fullOnMax()      { assertEquals("[##########]", BarChart.render(100, 100, 10, false)); }
    @Test void halfFill()       { assertEquals("[#####     ]", BarChart.render(50, 100, 10, false)); }
    @Test void clampedOverMax() { assertEquals("[##########]", BarChart.render(999, 100, 10, false)); }
}
