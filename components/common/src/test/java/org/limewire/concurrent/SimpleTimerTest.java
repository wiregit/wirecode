package org.limewire.concurrent;

import java.util.ArrayList;

import org.limewire.concurrent.SimpleTimer;
import org.limewire.service.ErrorCallback;
import org.limewire.service.ErrorCallbackStub;
import org.limewire.service.ErrorService;
import org.limewire.util.BaseTestCase;

import junit.framework.Test;


/**
 * Unit tests for SimpleTimer
 */
public class SimpleTimerTest extends BaseTestCase {
    
    private long T=100;
            
	public SimpleTimerTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(SimpleTimerTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    //Tests when the first item schedule goes first
    public void testFirstGoesFirst() {
        SimpleTimer t=new SimpleTimer(false);  //not daemon: test thread dies
        sleep(T);    //make timer thread block
        TimerTestTask a=new TimerTestTask("a");
        TimerTestTask b=new TimerTestTask("b");
        long start=System.currentTimeMillis();
        t.schedule(a, 2*T, 2*T);
        sleep(T);
        t.schedule(b, 2*T, 3*T);
        sleep(8*T+T/2);
        t.cancel();
        sleep(3*T);  //to check that cancel really worked

        a.checkMatch(start+2*T, 4, 2*T);
        b.checkMatch(start+3*T, 3, 3*T);

        try {
            t.schedule(new TimerTestTask("c"), 0, T);
            fail("illegalstateexception should have been thrown");
        } catch (IllegalStateException pass) { }
    }

    //Tests when the second item scheduled goes first
    public void testSecondGoesFirst() {
        SimpleTimer t=new SimpleTimer(false);  //not daemon: test thread dies
        TimerTestTask b=new TimerTestTask("b2");
        TimerTestTask a=new TimerTestTask("a2");
        long start=System.currentTimeMillis();
        t.schedule(b, 3*T, 3*T);
        sleep(T);
        t.schedule(a, T, 2*T);
        sleep(8*T+T/2);
        t.cancel();
        sleep(3*T);  //to check that cancel really worked

        a.checkMatch(start+2*T, 4, 2*T);
        b.checkMatch(start+3*T, 3, 3*T);
    }

    //Test the priority queue with many tasks
    public void testManyTasks() {
        SimpleTimer t=new SimpleTimer(true);
        TimerTestTask[] tasks=new TimerTestTask[12];
        long start=System.currentTimeMillis();
        for (int i=0; i<tasks.length; i++) {
            tasks[i]=new TimerTestTask("T"+i);
            t.schedule(tasks[i], 0, 4*T);
        }

        sleep(5*T);
        t.cancel();

        for (int i=0; i<tasks.length; i++) {
            tasks[i].checkMatch(start, 2, 4*T);
        }
    }
    
    public void testExceptionIsCaught() {
        ErrorCallback old = ErrorService.getErrorCallback();
        ErrorCallbackStub now = new ErrorCallbackStub();
        try {
            SimpleTimer t = new SimpleTimer(false);
            ErrorService.setErrorCallback(now);
            TimerTestTask a = new TimerTestTask("a3", true);
            t.schedule(a, T, 2*T);
            sleep(T+T/2);
            t.cancel();
            sleep(3*T);
            
            assertEquals( 1, now.exceptions );
        } finally {
            ErrorService.setErrorCallback(old);
        }
    }

    void sleep(long msecs) {
        try { Thread.sleep(msecs); } catch (InterruptedException ignored) { }
    }    
    
    private static class TimerTestTask implements Runnable {
        //The system times this was executed, as Long.
        private ArrayList<Long> _runs=new ArrayList<Long>();
        // Amount of allowed variation, in msecs.
        private static long FUDGE_FACTOR=40;
        private boolean _throwException;
    
        TimerTestTask(String name) {
            this(name, false);
        }
    
        TimerTestTask(String name, boolean throwException) {
            this._throwException=throwException;
        }
    
        public void run() {
            long now=System.currentTimeMillis();
            _runs.add(new Long(now));
            if (_throwException) 
                throw new IndexOutOfBoundsException();
        }
        
        //Checks that this' execution times approximately match.
        //Runs must be 2 or greater
        void checkMatch(long start, int runs, long period) {
            assertEquals(runs, _runs.size());
            assertEquals("start times not equal", start, get(0), FUDGE_FACTOR);
            for (int i=1; i<runs; i++) {
                long actualPeriod=get(i)-get(i-1);
                assertEquals("bad spacing in runs",
                             period, actualPeriod, FUDGE_FACTOR);
            }
        }
        
        long get(int i) {
            return _runs.get(i).longValue();
        }
    }	    
}