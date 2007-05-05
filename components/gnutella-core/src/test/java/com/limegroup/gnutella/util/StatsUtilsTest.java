package com.limegroup.gnutella.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.math.BigInteger;
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
    
    public void testHistogram() throws Exception {
        List<Double> l = new ArrayList<Double>();
        for (int x = 0; x < 20; x++)
            l.add((double)x);
        
        List<Integer> hist = StatsUtils.getHistogram(l, 2);
        assertEquals(Integer.valueOf(10), hist.get(0));
        assertEquals(Integer.valueOf(10), hist.get(1));
        
        hist = StatsUtils.getHistogram(l, 10);
        assertEquals(10,hist.size());
        for (int i : hist)
            assertEquals(2, i);
        
        hist = StatsUtils.getHistogram(l, 20);
        assertEquals(20,hist.size());
        for (int i : hist)
            assertEquals(1, i);
        
        // a histogram with more breaks than data points will have
        // some of them 0
        hist = StatsUtils.getHistogram(l, 40); //1,0,1,0,1,0...
        for (int i = 0; i < 40; i++) {
            if (i % 2 == 0)
                assertEquals(Integer.valueOf(1), hist.get(i));
            else
                assertEquals(Integer.valueOf(0), hist.get(i));
        }
        
        // now test with BigIntegers 
        // Long.MAX_VALUE, power 2, power 3... power 10
        List<BigInteger> big = new ArrayList<BigInteger>();
        for (int i = 0; i < 10; i++) {
            BigInteger v = BigInteger.valueOf(Long.MAX_VALUE);
            int power = i;
            while (power-- > 0)
                v = v.multiply(v);
            big.add(v);
        }
        
        // we have 1000 breaks, but the values are powers, so most will
        // be in the first step and one in the last
        hist = StatsUtils.getHistogramBigInt(big, 1000);
        assertEquals(Integer.valueOf(9), hist.get(0));
        assertEquals(Integer.valueOf(1), hist.get(999));
        for (int i = 1; i< 999; i++)
            assertEquals(Integer.valueOf(0),hist.get(i));
        
        // try less spread out big ints
        // 0, Long.MAX_VALUE, *4, *9, *16...
        big.clear();
        for (int i = 0; i < 10; i++) {
            BigInteger v = BigInteger.valueOf(Long.MAX_VALUE);
            v = v.multiply(BigInteger.valueOf(i * i));
            big.add(v);
        }

        // with 9^2 breaks there should be an entry in each square slot
        // except the last one which will fall into 80 because of rounding
        hist = StatsUtils.getHistogramBigInt(big, 81);
        assertEquals(Integer.valueOf(1), hist.get(80));
        // all others should be 0
        for (int i = 0; i < 80; i++) {
            
            double root = Math.sqrt(i);
            if (root - (int)(root) != 0)
                assertEquals(Integer.valueOf(0), hist.get(i));
            else
                assertEquals(Integer.valueOf(1), hist.get(i));
        }
        
    }
    
    private void assertMatches(double expected, String key, Map<String, Object> stats) 
    throws Exception {
        assertEquals(expected, get(key,stats));
    }
    
    private double get(String key, Map<String,Object>stats) throws Exception {
        byte [] b = (byte[])stats.get(key);
        DataInputStream dais = new DataInputStream(new ByteArrayInputStream(b));
        return Double.longBitsToDouble(dais.readLong());
    }
}
