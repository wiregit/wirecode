package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Random;

import junit.framework.Test;

/**
 * Tests BandwidthThrottle and ThrottledOutputStream.
 */
public class BandwidthThrottleTest extends com.limegroup.gnutella.util.BaseTestCase {
    public BandwidthThrottleTest(String name) {
        super(name);
    }

    /** Time per test. */
    final int TIME=1000;        //1 second
    /** bytesSent should be the desired value + or - FUDGE_FACTOR */
    final float FUDGE_FACTOR=0.12f;  //12 percent

    BandwidthThrottle throttle;
    Random random;
    long startTime;
    long stopTime;       
    int bytesSent;

    /** The following are not used for testBandwidthThrottle */
    PipedOutputStream pout;
    PipedInputStream pin;
    OutputStream out;
    
    public static Test suite() {
        return buildTestSuite(BandwidthThrottleTest.class);
    }  

    protected void setUp() {
        throttle=new BandwidthThrottle(0);  //Each test sets rate
        random=new Random();
        startTime=System.currentTimeMillis();
        bytesSent=0;

        try {
            pout=new PipedOutputStream();
            pin=new PipedInputStream(pout);
            out=new ThrottledOutputStream(pout, throttle);
        } catch (IOException e) {
            fail("Couldn't set up stream", e);
        }
        
        stopTime=startTime+TIME;    //Do this last, since time matters
    }

    ////////////////////////////////////////////////////////////////
    
    public void testBandwidthThrottle() {
        final int RATE=100;              //Slow, 100 bytes/second
        throttle.setRate(RATE);
        final int BYTES=TIME/1000*RATE;  

        while (System.currentTimeMillis()<stopTime) {
            int bytesToSend=random.nextInt(15);     //more or less than N
            bytesSent+=throttle.request(bytesToSend);
        }
        assertLessThan("Wrong number of bytes: "+bytesSent,
                   FUDGE_FACTOR*BYTES, Math.abs(bytesSent-BYTES));
    }

    public void testThrottledOutputStreamByte() throws Exception {
        final int RATE=20000;            //Fast! 10KB/second
        throttle.setRate(RATE);
        final int BYTES=TIME/1000*RATE;  //200 bytes

        while (System.currentTimeMillis()<stopTime) {
            byte b=(byte)random.nextInt();
            out.write(b);
            assertTrue("Bad byte", (byte)pin.read()==b);
            bytesSent++;
        }
        assertLessThan("Wrong number of bytes: "+bytesSent,
                   FUDGE_FACTOR*BYTES, Math.abs(bytesSent-BYTES));
    }
    
    public void testThrottledOutputStreamBytes() throws Exception  {
        final int RATE=1000;            //Medium fast
        throttle.setRate(RATE);
        final int BYTES=TIME/1000*RATE;  

        byte[] buf=new byte[150];
        while (System.currentTimeMillis()<stopTime) {
            random.nextBytes(buf);
            int n=random.nextInt(buf.length);
            out.write(buf, 0, n);
            for (int i=0; i<n; i++)   
                assertTrue("Bad byte", (byte)pin.read()==buf[i]);
            bytesSent+=n;
        }
        assertLessThan("Wrong number of bytes: "+bytesSent,
                   FUDGE_FACTOR*BYTES, Math.abs(bytesSent-BYTES));
    }
    
    public void testThrottledOutputStreamBytes2() throws Exception {
        final int RATE=1000;            //Medium fast
        throttle.setRate(RATE);
        final int BYTES=TIME/1000*RATE;  

        byte[] buf=new byte[150];
        while (System.currentTimeMillis()<stopTime) {
            random.nextBytes(buf);
            out.write(buf);
            for (int i=0; i<buf.length;i++)   
                assertTrue("Bad byte", (byte)pin.read()==buf[i]);
            bytesSent+=buf.length;
        }
        assertLessThan("Wrong number of bytes: "+bytesSent,
                   FUDGE_FACTOR*BYTES, Math.abs(bytesSent-BYTES)); 
    }

    //TODO: test fairness when sharing one throttle among multiple streams
}
