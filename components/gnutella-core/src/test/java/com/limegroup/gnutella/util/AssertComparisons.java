package com.limegroup.gnutella.util;

import junit.framework.*;
    
/**
 * A set of assert comparisons to check greater than / less than
 * situations.
 */
public class AssertComparisons extends TestCase {
        
    private static final int LESS_THAN = 0;
    private static final int GREATER_THAN = 1;
    private static final int LESS_THAN_OR_EQUALS = 2;
    private static final int GREATER_THAN_OR_EQUALS = 3;
    
    /**
     * Named constructor.
     */
    public AssertComparisons(String name) {
        super(name);
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */
    static public void assertGreaterThan(Object expected, Object actual) {
        assertGreaterThan(null, expected, actual);
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */
    static public void assertGreaterThan(String msg, Object expected, Object actual) {
        assertComparison(GREATER_THAN, msg, expected, actual);
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(long expected, long actual) {
        assertGreaterThan( new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, long expected, long actual) {
        assertGreaterThan( msg, new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(short expected, short actual) {
        assertGreaterThan( new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, short expected, short actual) {
        assertGreaterThan( msg, new Short(expected), new Short(actual) );
    }

    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(int expected, int actual) {
        assertGreaterThan( new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, int expected, int actual) {
        assertGreaterThan( msg, new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(double expected, double actual) {
        assertGreaterThan( new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, double expected, double actual) {
        assertGreaterThan( msg, new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(float expected, float actual) {
        assertGreaterThan( new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, float expected, float actual) {
        assertGreaterThan( msg, new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThan(byte expected, byte actual) {
        assertGreaterThan( new Byte(expected), new Byte(actual) );
    }
    
    /**
     * Assertes that actual is greater than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThan(String msg, byte expected, byte actual) {
        assertGreaterThan( msg, new Byte(expected), new Byte(actual) );
    }

    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(Object expected, Object actual) {
        assertLessThan(null, expected, actual);
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, Object expected, Object actual) {
        assertComparison(LESS_THAN, msg, expected, actual);
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(long expected, long actual) {
        assertLessThan( new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, long expected, long actual) {
        assertLessThan( msg, new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(short expected, short actual) {
        assertLessThan( new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, short expected, short actual) {
        assertLessThan( msg, new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(int expected, int actual) {
        assertLessThan( new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, int expected, int actual) {
        assertLessThan( msg, new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(double expected, double actual) {
        assertLessThan( new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, double expected, double actual) {
        assertLessThan( msg, new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(float expected, float actual) {
        assertLessThan( new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, float expected, float actual) {
        assertLessThan( msg, new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThan(byte expected, byte actual) {
        assertLessThan( new Byte(expected), new Byte(actual) );
    }
    
    /**
     * Assertes that actual is less than expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThan(String msg, byte expected, byte actual) {
        assertLessThan( msg, new Byte(expected), new Byte(actual) );
    }

    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */
    static public void assertGreaterThanOrEquals(Object expected, Object actual) {
        assertGreaterThanOrEquals(null, expected, actual);
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, Object expected, Object actual) {
        assertComparison(GREATER_THAN_OR_EQUALS, msg,  expected, actual);
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(long expected, long actual) {
        assertGreaterThanOrEquals( new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, long expected, long actual) {
        assertGreaterThanOrEquals( msg, new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(short expected, short actual) {
        assertGreaterThanOrEquals( new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, short expected, short actual) {
        assertGreaterThanOrEquals( msg, new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(int expected, int actual) {
        assertGreaterThanOrEquals( new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, int expected, int actual) {
        assertGreaterThanOrEquals( msg, new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(double expected, double actual) {
        assertGreaterThanOrEquals( new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, double expected, double actual) {
        assertGreaterThanOrEquals( msg, new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(float expected, float actual) {
        assertGreaterThanOrEquals( new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, float expected, float actual) {
        assertGreaterThanOrEquals( msg, new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertGreaterThanOrEquals(byte expected, byte actual) {
        assertGreaterThanOrEquals( new Byte(expected), new Byte(actual) );
    }
    
    /**
     * Assertes that actual is greater than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertGreaterThanOrEquals(String msg, byte expected, byte actual) {
        assertGreaterThanOrEquals( msg, new Byte(expected), new Byte(actual) );
    }

    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(Object expected, Object actual) {
        assertLessThanOrEquals(null, expected, actual);
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, Object expected, Object actual) {
        assertComparison(LESS_THAN_OR_EQUALS, msg, expected, actual);
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(long expected, long actual) {
        assertLessThanOrEquals( new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, long expected, long actual) {
        assertLessThanOrEquals( msg, new Long(expected), new Long(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(short expected, short actual) {
        assertLessThanOrEquals( new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, short expected, short actual) {
        assertLessThanOrEquals( msg, new Short(expected), new Short(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(int expected, int actual) {
        assertLessThanOrEquals( new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, int expected, int actual) {
        assertLessThanOrEquals( msg, new Integer(expected), new Integer(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(double expected, double actual) {
        assertLessThanOrEquals( new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, double expected, double actual) {
        assertLessThanOrEquals( msg, new Double(expected), new Double(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(float expected, float actual) {
        assertLessThanOrEquals( new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, float expected, float actual) {
        assertLessThanOrEquals( msg, new Float(expected), new Float(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown.
     */    
    static public void assertLessThanOrEquals(byte expected, byte actual) {
        assertLessThanOrEquals( new Byte(expected), new Byte(actual) );
    }
    
    /**
     * Assertes that actual is less than or equal to expected.  If it isn't,
     * an AssertionFailedError is thrown with the given message.
     */    
    static public void assertLessThanOrEquals(String msg, byte expected, byte actual) {
        assertLessThanOrEquals( msg, new Byte(expected), new Byte(actual) );
    }    

    
    /**
     * Assert that an object actual is either less than, greater than,
     * less than or equal to, or greater than or equals to object expected.
     * Checks java.lang.Comparable & com.sun.java.util.collections.Comparable
     * If neither 'expected' or 'actual' implement either interface, an
     * AssertionFailedError is thrown.
     * If both 'expected' and 'actual' are null, an AssertionFailedError is thrown.
     */
    static private void assertComparison(int type, String msg, Object expected, Object actual) {
        int ret = 0;
        if ( expected == null && actual == null) {
            fail(formatComparison(type, msg, expected, actual));
        } else if (expected instanceof java.lang.Comparable) {
            ret = ((java.lang.Comparable)expected).compareTo(actual);
        } else if (actual instanceof java.lang.Comparable) {
            ret = -1 * ((java.lang.Comparable)actual).compareTo(expected);
        } else if (expected instanceof com.sun.java.util.collections.Comparable) {
            ret =
                ((com.sun.java.util.collections.Comparable)expected).compareTo(actual);
        } else if (actual instanceof com.sun.java.util.collections.Comparable) {
            ret = -1 *
                ((com.sun.java.util.collections.Comparable)actual).compareTo(expected);
        } else { //neither implement either interface.
            fail("Neither " + expected + " nor actual " + 
                 "implement expected Comparable interface.");
        }
        
        // break out of here if the comparison failed.
        switch(type) {
        case LESS_THAN:
            if ( ret <= 0 ) break;
            return;
        case GREATER_THAN:
            if ( ret >= 0 ) break;
            return;
        case LESS_THAN_OR_EQUALS:
            if ( ret < 0 ) break;
            return;
        case GREATER_THAN_OR_EQUALS:
            if ( ret > 0 ) break;
            return;
        }

        //if we didn't return, we failed.
        fail(formatComparison(type, msg, expected, actual));        
        
    }
    
    static private String formatComparison(int type,
                                         String message,
                                         Object expected, 
                                         Object actual) {
        String compare = "";
        switch(type) {
        case LESS_THAN:
            compare = "less than"; break;
        case GREATER_THAN:
            compare = "greater than"; break;
        case LESS_THAN_OR_EQUALS:
            compare = "less than or equal to"; break;
        case GREATER_THAN_OR_EQUALS:
            compare = "greater than or equal to"; break;
        }
        String formatted = "";
        if ( message != null )
            formatted = message + " ";
        return formatted + "expected " + compare + ":<" + expected+
            "> but was:<" + actual + ">";
    }
}