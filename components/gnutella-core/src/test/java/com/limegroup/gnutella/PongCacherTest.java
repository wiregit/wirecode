package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.security.*;
import junit.framework.*;
import com.sun.java.util.collections.*;

/**
 * Tests the <tt>PongCacher</tt> class that maintains a cache of the best most
 * recent pongs seen.
 */
public final class PongCacherTest extends BaseTestCase {

    private static final PongCacher PC = PongCacher.instance();

    public PongCacherTest(String name) {
        super(name);        
    }

    public static Test suite() {
        return buildTestSuite(PongCacherTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static void globalSetUp() throws Exception {
        PC.start();
    }

    /**
     * Tests the method for adding a pong to the cacher.
     */
    public void testAddPong() throws Exception {
        ConnectionManager cm = 
            new UltrapeerConnectionManager(new ServerAuthenticator());
        PrivilegedAccessor.setValue(RouterService.class, "manager", cm);    

        PingReply pong = PingReply.create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);

        BucketQueue bq = 
            (BucketQueue)PrivilegedAccessor.getValue(PongCacher.class, 
                                                     "_pongs");
        assertEquals("unexpected bucket queue size", 1, bq.size());

        pong = PingReply.create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);
        assertEquals("unexpected bucket queue size", 2, bq.size());
        assertEquals("unexpected bucket queue size", 2, bq.size(0));

        for(int i=bq.size(0); i<PongCacher.PONGS_PER_HOP+2; i++) {
            PC.addPong(pong);
        }
        
        assertEquals("unexpected bucket queue size", 
                     PongCacher.PONGS_PER_HOP, bq.size(0));
    }

    /**
     * Tests the method for getting the best set of pongs.
     */
    public void testGetBestPongs() throws Exception {
        ConnectionManager cm = 
            new UltrapeerConnectionManager(new ServerAuthenticator());
        PrivilegedAccessor.setValue(RouterService.class, "manager", cm);    
        Thread.sleep(2000);
        
        Set pongs = PC.getBestPongs();
        int startSize = pongs.size();

        PingReply pong = PingReply.create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);        

        Thread.sleep(2000);
        pongs = PC.getBestPongs();
        assertEquals("unexpected number of cached pongs", 
                     startSize+1, pongs.size());        

        pong = PingReply.create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);        

        Thread.sleep(2000);
        pongs = PC.getBestPongs();
        assertEquals("unexpected number of cached pongs", 
                     startSize+2, pongs.size());  

        // fill up the pongs at the default hop
        for(int i=0; i<30; i++) {
            PingReply curPong = 
                PingReply.create(new GUID().bytes(), (byte)5);
            PC.addPong(curPong);
        }

        Thread.sleep(2000);
        pongs = PC.getBestPongs();

        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_CACHED_PONGS, pongs.size());

        PingReply highHopPong = 
            PingReply.create(new GUID().bytes(), (byte)5);
        
        highHopPong.hop();
        highHopPong.hop();
        PC.addPong(highHopPong);

        Thread.sleep(2000);
        pongs = PC.getBestPongs();
        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_CACHED_PONGS, pongs.size());

        BucketQueue queue = 
            (BucketQueue)PrivilegedAccessor.getValue(PC, "_pongs");

        assertEquals("first pong should be high hops", highHopPong, 
                     queue.extractMax());        

        Iterator iter = pongs.iterator();
        boolean equal = false;
        PingReply pr = null;
        int z = 0;
        while(iter.hasNext()) {
            pr = (PingReply)iter.next();
            if(pr.equals(highHopPong)) {
                System.out.println("high hops found at index: "+z); 
                equal = true;
            }
            z++;
        }
        //assertTrue("should have had high hops pong",equal);
        //assertEquals("first pong should be high hops", highHopPong, 
        //           pongs.extractMax());
        //assertEquals("first pong should be high hops");
    }
}










