package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.util.*;
import junit.framework.*;
import java.util.Random;
import java.io.*;

/**
 * Tests BandwidthThrottle and ThrottledOutputStream.
 */
public class BandwidthThrottleTest extends TestCase {
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
            fail("Couldn't set up stream: "+e);
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
        assertTrue("Wrong number of bytes: "+bytesSent,
                   Math.abs(bytesSent-BYTES)<FUDGE_FACTOR*BYTES);
    }

    public void testThrottledOutputStreamByte() {
        final int RATE=20000;            //Fast! 10KB/second
        throttle.setRate(RATE);
        final int BYTES=TIME/1000*RATE;  //200 bytes

        try {
            while (System.currentTimeMillis()<stopTime) {
                byte b=(byte)random.nextInt();
                out.write(b);
                assertTrue("Bad byte", (byte)pin.read()==b);
                bytesSent++;
            }
        } catch (IOException e) {
            fail("Mysterious IO problem "+e);
        }
        assertTrue("Wrong number of bytes: "+bytesSent,
                   Math.abs(bytesSent-BYTES)<FUDGE_FACTOR*BYTES);
    }
    
    public void testThrottledOutputStreamBytes() {
        final int RATE=1000;            //Medium fast
        throttle.setRate(RATE);
        final int BYTES=TIME/1000*RATE;  

        try {
            byte[] buf=new byte[150];
            while (System.currentTimeMillis()<stopTime) {
                random.nextBytes(buf);
                int n=random.nextInt(buf.length);
                out.write(buf, 0, n);
                for (int i=0; i<n; i++)   
                    assertTrue("Bad byte", (byte)pin.read()==buf[i]);
                bytesSent+=n;
            }
        } catch (IOException e) {
            fail("Mysterious IO problem "+e);
        }
        assertTrue("Wrong number of bytes: "+bytesSent,
                   Math.abs(bytesSent-BYTES)<FUDGE_FACTOR*BYTES);
    }
    
    public void testThrottledOutputStreamBytes2() {
        final int RATE=1000;            //Medium fast
        throttle.setRate(RATE);
        final int BYTES=TIME/1000*RATE;  

        try {
            byte[] buf=new byte[150];
            while (System.currentTimeMillis()<stopTime) {
                random.nextBytes(buf);
                out.write(buf);
                for (int i=0; i<buf.length;i++)   
                    assertTrue("Bad byte", (byte)pin.read()==buf[i]);
                bytesSent+=buf.length;
            }
        } catch (IOException e) {
            fail("Mysterious IO problem "+e);
        }
        assertTrue("Wrong number of bytes: "+bytesSent,
                   Math.abs(bytesSent-BYTES)<FUDGE_FACTOR*BYTES); 
    }

    //TODO: test fairness when sharing one throttle among multiple streams
}
