package com.limegroup.gnutella;

import junit.framework.*;

public class StatisticsTest extends TestCase { 
    //Most of this code assumes a window factor W of 7 days.

    /** Fudge factor */
    private float DELTA=0.0001f;
    private static final int MSECS_PER_HOUR=60*60*1000;
    private static final int MSECS_PER_DAY=24*MSECS_PER_HOUR;


    public StatisticsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(StatisticsTest.class);
    }

    public void setUp() {
        SettingsManager.instance().setLastShutdownTime(0);
        SettingsManager.instance().setFractionalUptime(0.0f);
        TestStatistics.now=0;
    }
    
    /** Tests the test!  You may laugh, but it took me a while to get that class
     *  working, because the initializer for Statistics depends on template
     *  methods in the subclass, which was previously using uninitialized state
     *  of the subclass. */
    public void testTestStatistics() {
        TestStatistics.now=1000;
        TestStatistics t=new TestStatistics();
        TestStatistics.now+=12;
        assertEquals(12, t.getUptime());
    }

    /**
     * Start up for first time.  Run for 1 day.  Shutdown.
     */
    public void testFirstDay() {
        TestStatistics.now=MSECS_PER_DAY*8;
        TestStatistics stats=new TestStatistics();
        TestStatistics.now+=MSECS_PER_DAY;
        assertEquals(1.0f/7.0f, stats.calculateFractionalUptime(), DELTA);
    }

    /**
     * Run half the time: make sure value doesn't change.
     */
    public void testHalfTimeEquilibrium() {
        //                 ------up------
        // days 0          1             2
        SettingsManager.instance().setFractionalUptime(0.5f);
        TestStatistics.now=MSECS_PER_DAY;
        TestStatistics stats=new TestStatistics();
        TestStatistics.now+=MSECS_PER_DAY;
        assertEquals(0.5f, stats.calculateFractionalUptime(), DELTA);
        assertEquals(12*60*60., (float)stats.calculateDailyUptime(), DELTA);
    }

    /** 
     * Initially up half time.  Shut down for 2 days, run for 1 day.
     * New uptime: 3/7*1/3+4/7*0.5=3/7
     */
    public void testFractionalUpdate() {
        SettingsManager settings=SettingsManager.instance();

        //Test calculate without property update
        settings.setFractionalUptime(0.5f);
        TestStatistics.now=2*MSECS_PER_DAY;
        TestStatistics stats=new TestStatistics();
        TestStatistics.now+=MSECS_PER_DAY;
        assertEquals(3.0f/7.0f, stats.calculateFractionalUptime(), DELTA);
        assertEquals(0, settings.getLastShutdownTime());
        assertEquals(0.5f, settings.getFractionalUptime(), DELTA);

        //Test shutdown method too.
        stats.shutdown();
        assertEquals(TestStatistics.now, settings.getLastShutdownTime());
        assertEquals(3.0f/7.0f, settings.getFractionalUptime(), DELTA);
    }

    /** Test one hour down, two hours up.  It takes a long time to
     *  converge on this value. */
    public void testConvergence() {
        for (int i=0; i<200; i++) {            
            TestStatistics.now+=MSECS_PER_HOUR;
            TestStatistics stats=new TestStatistics();    //start
            TestStatistics.now+=2*MSECS_PER_HOUR;
            stats.shutdown();                             //stop
        }

        assertEquals(2.0f/3.0f, 
                     SettingsManager.instance().getFractionalUptime(), 
                     0.05);
    }

    /**
     * Run for a huge period of time.
     */
    public void testUpFullTime() {
        TestStatistics.now=MSECS_PER_DAY;   //optional
        TestStatistics stats=new TestStatistics();
        TestStatistics.now+=MSECS_PER_DAY*8;
        assertEquals(1.0f, stats.calculateFractionalUptime(), DELTA);
        assertEquals(24*60*60., (float)stats.calculateDailyUptime(), DELTA);
    }

    /**
     * Tests clocks running backwards, e.g., from daylight savings.
     * (Session should be ignored)
     */
    public void testUptimeBackwards() {
        SettingsManager settings=SettingsManager.instance();
        settings.setFractionalUptime(0.5f);

        //Test calculate without property update
        TestStatistics.now=MSECS_PER_DAY;
        TestStatistics stats=new TestStatistics();
        TestStatistics.now-=MSECS_PER_HOUR;   //backwards!
        assertEquals(0.5f, settings.getFractionalUptime(), 0.0f);

        //Test shutdown method too.
        stats.shutdown();
        assertEquals(TestStatistics.now, settings.getLastShutdownTime());
        assertEquals(0.5f, settings.getFractionalUptime(), 0.0f);        
    }
    
   /**
     * Tests clocks running backwards, e.g., from daylight savings.
     * (Session should be ignored)
     */
    public void testShutdownBackwards() {
        SettingsManager settings=SettingsManager.instance();
        settings.setLastShutdownTime(MSECS_PER_DAY);
        settings.setFractionalUptime(0.5f);

        //Test calculate without property update
        TestStatistics.now=settings.getLastShutdownTime()-MSECS_PER_HOUR;  //!
        TestStatistics stats=new TestStatistics();
        TestStatistics.now+=MSECS_PER_DAY;
        assertEquals(0.5f, settings.getFractionalUptime(), 0.0f);

        //Test shutdown method too.
        stats.shutdown();
        assertEquals(TestStatistics.now, settings.getLastShutdownTime());
        assertEquals(0.5f, settings.getFractionalUptime(), 0.0f);        
    }
}


/** Lets you fake the system time used in fractional uptime calculations. */
class TestStatistics extends Statistics {
    static long now;   

    protected long now() {
        //Need static variable because Statistics.startTime() is
        //initialized before local fields of TestStatistics.
        return now;
    }
}
