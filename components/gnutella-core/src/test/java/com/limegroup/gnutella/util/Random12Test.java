package com.limegroup.gnutella.util;

import junit.framework.Test;

/**
 * Unit tests for Random12
 */
public class Random12Test extends com.limegroup.gnutella.util.BaseTestCase {
    public Random12Test(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(Random12Test.class);
    }

    Random12 rand;
    public void setUp() {
        rand=new Random12(3145);
    }

    /////////////////////////////////////////////////////////////////


    public void testNextInt1() {
        doNextInt(1);
    }

    public void testNextInt2() {
        doNextInt(2);
    }

    public void testNextInt10() {
        doNextInt(10);
    }

    public void testNextInt64() {
        doNextInt(64);  //power of 2
    }

    public void testNextIntIllegal() {
        try {
            rand.nextInt(0);
            fail("No IAE");
        } catch (IllegalArgumentException pass) { }
    }


    public void doNextInt(int n) {
        int[] values=new int[n];

        for (int trials=0; trials<n*10; trials++) {
            int i=rand.nextInt(n);
            assertGreaterThanOrEquals(0,i);
            assertLessThan(n, i);
            values[i]++;
        }

        for (int i=0; i<n; i++) {
            assertGreaterThan(0, values[i]);
        }
    }
}
