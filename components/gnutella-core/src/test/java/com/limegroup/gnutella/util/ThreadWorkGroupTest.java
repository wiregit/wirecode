package com.limegroup.gnutella.util;

import java.util.Random;

import junit.framework.Test;

public class ThreadWorkGroupTest extends BaseTestCase {
    
    private int _total = 0;
    private int _numTasks = 0;
    private final int NUM_WORKERS=10;

	public ThreadWorkGroupTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(ThreadWorkGroupTest.class);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

    public synchronized void increment(int inc) {
        _total += inc;
    }

    public void testWorkGroupWithCleaner() throws Exception {
        Counter[] workers = new Counter[NUM_WORKERS];
        for (int i = 0; i < workers.length; i++)
            workers[i] = new Counter();
        ThreadWorkGroup workGroup = new ThreadWorkGroup(workers, new Cleaner());
        assertTrue(workGroup.isActive());
        Random rand = new Random();
        int localTotal = 0;

        // make sure all workers are woken up, and then set them to just process
        // ---------------------------------------------------------------------
        _numTasks += 200;
        for (int i = 0; i < _numTasks; i++) {
            int currInt = rand.nextInt(200);
            workGroup.addTask(new Object[] { new Integer(currInt) });
            localTotal += currInt;
        }
        Thread.sleep(500);  // give each thread time to wake up...
        for (int i = 0; i < workers.length; i++)
            assertTrue(workers[i].wakeUp());
        Thread.sleep(4*1000); // let the workers work....
        assertEquals(_total, localTotal);
        assertTrue(workGroup.isActive());
        // make sure NO workers are working
        for (int i = 0; i < workers.length; i++)
            assertTrue(!workers[i].wakeUp());
        // ---------------------------------------------------------------------

        // put in a bad task and make sure it doesn't get executed, though
        // this is only half testing ThreadWorkGroup
        // ---------------------------------------------------------------------
        _numTasks++;
        workGroup.addTask(new Object[] { new Long(0) });
        Thread.sleep(100);
        assertEquals(_total, localTotal);
        assertTrue(workGroup.isActive());
        // make sure NO workers are working
        for (int i = 0; i < workers.length; i++)
            assertTrue(!workers[i].wakeUp());
        // ---------------------------------------------------------------------

        // now make sure that enough workers are woken up as necessary
        // ---------------------------------------------------------------------
        for (int i = 0; i < workers.length; i++)
            workers[i].makeWait();
        int halfOfWorkers = NUM_WORKERS/2;
        _numTasks += halfOfWorkers;
        for (int i = 0; i < halfOfWorkers; i++) {
            int currInt = rand.nextInt(200);
            workGroup.addTask(new Object[] { new Integer(currInt) });
            localTotal += currInt;
        }
        {
            // give the scheduler some time and then....
            Thread.sleep(1*1000);
            // make sure some workers are woken up, and then set them to just
            // process
            int numAlive = 0;
            for (int i = 0; i < workers.length; i++) {
                if (workers[i].wakeUp())
                    numAlive++;
            }
            assertEquals(halfOfWorkers, numAlive);
        }
        Thread.sleep(500); // let the workers work....
        assertEquals(_total, localTotal);
        assertTrue(workGroup.isActive());
        // make sure NO workers are working
        for (int i = 0; i < workers.length; i++)
            assertTrue(!workers[i].wakeUp());
        // ---------------------------------------------------------------------

        // make sure after stop no work is done....
        // ---------------------------------------------------------------------
        workGroup.stop(0);
        assertTrue(!workGroup.isActive());
        // anybody woken up should wait so i can tell....
        for (int i = 0; i < workers.length; i++)
            workers[i].makeWait();
        workGroup.addTask(new Object[] { new Integer(rand.nextInt(200)) });
        // give the scheduler some time and then....
        Thread.sleep(300);
        // make sure NO workers are woken up
        for (int i = 0; i < workers.length; i++)
            assertTrue(!workers[i].wakeUp());
        assertEquals(_total, localTotal);
        // ---------------------------------------------------------------------
    }


    private class Cleaner implements ThreadWorkGroup.CleanUpInterface {
        private int _calls = 0;
        public void cleanUp(Object[] input) {
            assertTrue(++_calls <= _numTasks);
        }
    }

    private class Counter implements ThreadWorkGroup.WorkerInterface {
        private Integer _int;
        private boolean shouldWait = true;
        private boolean waiting = false;
        public boolean checkInput(Object[] input) {
            if ((input.length == 1) &&
                (input[0] instanceof Integer)) {
                _int = (Integer) input[0];
                return true;
            }
            return false;
        }
        public void run() {
            if (shouldWait) {
                synchronized (_int) {
                    waiting = true;
                    try {
                        _int.wait();
                    }
                    catch (InterruptedException ie) {}
                    waiting = false;
                }
            }
            increment(_int.intValue());           
        }
        // use this to make sure a guy is running and then let him proceed
        // return whether this guy was waiting....
        public boolean wakeUp() {
            if (_int == null)
                return waiting;
            synchronized (_int) {
                shouldWait = false;
                _int.notify();
                return waiting;
            }
        }
        // if you want a guy to wait after having wakeUp()'ed
        public void makeWait() {
            if (_int == null)
                shouldWait = true;
            else {
                synchronized (_int) {
                    shouldWait = true;
                }
            }
        }
    }


}
