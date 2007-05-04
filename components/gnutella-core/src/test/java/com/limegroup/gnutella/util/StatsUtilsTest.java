package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

public class StatsUtilsTest extends LimeTestCase {

    public StatsUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(StatsUtilsTest.class);
    }
    
    public void testQuickStats() throws Exception {
        List<Double> l = new ArrayList<Double>();
        for (int i = 0; i < 100; i++)
            l.add((double)i);
        Map<String, Object> stats = StatsUtils.quickStatsDouble(l);
        assertEquals(Integer.valueOf(100), Integer.valueOf(stats.get("num").toString()));
        assertMatches(0.0,"min", stats);
        assertMatches(99.0,"max",stats);
        assertMatches(49.5,"med", stats);
        assertMatches(49.5,"avg", stats);
        assertMatches(24.25,"Q1", stats);
        assertMatches(74.75,"Q3", stats);
        double variance = get("var", stats);
        assertGreaterThan(841.6666666, variance); // its a nasty irrational fraction
        assertLessThan(841.6666667, variance);
    }
    
    private void assertMatches(double expected, String key, Map<String, Object> stats) {
        assertEquals(expected, get(key,stats));
    }
    
    private double get(String key, Map<String,Object>stats) {
        long value = Long.valueOf(stats.get(key).toString());
        return Double.longBitsToDouble(value);
    }
}
