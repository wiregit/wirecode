package com.limegroup.gnutella.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.limegroup.gnutella.util.StatsUtils.DoubleStats;

import junit.framework.Test;

public class StatsUtilsTest extends LimeTestCase {

    public StatsUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(StatsUtilsTest.class);
    }
    
    public void testToMap() throws Exception {
        List<Double> l = new ArrayList<Double>();
        for (int i = 0; i < 100; i++)
            l.add((double)i);
        Map<String, Object> stats = StatsUtils.quickStatsDouble(l).getMap();
        assertEquals(Integer.valueOf(100), Integer.valueOf(stats.get("num").toString()));
        assertMatches(0.0,"min", stats);
        assertMatches(99.0,"max",stats);
        assertMatches(49.5,"med", stats);
        assertMatches(49.5,"avg", stats);
        assertMatches(24.25,"Q1", stats);
        assertMatches(74.75,"Q3", stats);
        assertEquals(841.66666, get("M2",stats), 0.00001);
    }
    
    public void testQuickStats() throws Exception {
        List<Double> l = new ArrayList<Double>();
        for (int i = 0; i < 100; i++)
            l.add((double)i);
        DoubleStats s = StatsUtils.quickStatsDouble(l);
        assertEquals(100, s.getNumber());
        assertEquals(0.0,s.min);
        assertEquals(99.0, s.max);
        assertEquals(49.5, s.med);
        assertEquals(49.5, s.avg);
        assertEquals(24.25, s.q1);
        assertEquals(74.75, s.q3);
        assertEquals(841.6666666, s.m2, 0.00001);
    }
    
    public void testSkewedness() throws Exception {
        List<Double> l = new ArrayList<Double>();
        for (int i = 0; i < 100; i++)
            l.add((double)i);
        DoubleStats s = StatsUtils.quickStatsDouble(l);
        assertEquals(0.0, s.m3); // symmetric
        
        // add a bunch of points on one side
        for (int i = 0; i < 10; i++)
            l.add((double)i);
        s = StatsUtils.quickStatsDouble(l);
        assertNotEquals(0.0, s.m3); // no longer symmetric
        
        // add same bunch on the other side
        for (int i = 90; i < 100; i++)
            l.add((double)i);
        s = StatsUtils.quickStatsDouble(l);
        assertEquals(0.0, s.m3); // symmetric again
    }
    
    public void testKurtosis() throws Exception {
        List<Double> l = new ArrayList<Double>();
        for (int i = 0; i < 100; i++)
            l.add((double)i);
        
        // flat distribution - high kurtosis
        DoubleStats s = StatsUtils.quickStatsDouble(l);
        assertEquals(1262205.416666, s.m4, 0.0001);
        double kurtosis = s.m4;
        
        // add a hill in the middle, kurtosis decreases
        for (int i = 25; i < 75; i++)
            l.add((double)i);
        s = StatsUtils.quickStatsDouble(l);
        assertLessThan(kurtosis, s.m4);
        kurtosis = s.m4;
        
        // make that hill taller
        for (int i = 35; i < 65; i++)
            l.add((double)i);
        s = StatsUtils.quickStatsDouble(l);
        assertLessThan(kurtosis, s.m4);
        kurtosis = s.m4;
        
        // and taller, kurtosis keeps going down
        for (int i = 45; i < 55; i++)
            l.add((double)i);
        s = StatsUtils.quickStatsDouble(l);
        assertLessThan(kurtosis, s.m4);
        kurtosis = s.m4;
    }
    
    private void assertMatches(double expected, String key, Map<String, Object> stats) {
        assertEquals(expected, get(key,stats));
    }
    
    private double get(String key, Map<String,Object>stats) {
        long value = Long.valueOf(stats.get(key).toString());
        return Double.longBitsToDouble(value);
    }
}
