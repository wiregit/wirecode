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

    public PongCacherTest(String name) {
        super(name);        
    }

    public static Test suite() {
        return buildTestSuite(PongCacherTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * Tests the method for adding a pong to the cacher.
     */
    public void testAddPong() throws Exception {
        ConnectionManager cm = 
            new UltrapeerConnectionManager(new ServerAuthenticator());
        PrivilegedAccessor.setValue(RouterService.class, "manager", cm);    
        PongCacher pc = PongCacher.instance();
        
        PingReply pong = PingReply.create(new GUID().bytes(), (byte)5);
        pc.addPong(pong);

        BucketQueue bq = (BucketQueue)PrivilegedAccessor.getValue(PongCacher.class, "_pongs");
        assertEquals("unexpected bucket queue size", 1, bq.size());

        pong = PingReply.create(new GUID().bytes(), (byte)5);
        pc.addPong(pong);
        assertEquals("unexpected bucket queue size", 2, bq.size());
        assertEquals("unexpected bucket queue size", 2, bq.size(0));

        int pongsPerHop = 
            ((Integer)PrivilegedAccessor.getValue(PongCacher.class, "PONGS_PER_HOP")).intValue();
        for(int i=bq.size(0); i<pongsPerHop+2; i++) {
            pc.addPong(pong);
        }
        
        assertEquals("unexpected bucket queue size", pongsPerHop, bq.size(0));
    }

    /**
     * Tests the method for getting the best set of pongs.
     */
    public void testGetBestPongs() throws Exception {
        ConnectionManager cm = 
            new UltrapeerConnectionManager(new ServerAuthenticator());
        PrivilegedAccessor.setValue(RouterService.class, "manager", cm);    
        PongCacher pc = PongCacher.instance();        
        pc.start();
        
        Set pongs = pc.getBestPongs();
        int startSize = pongs.size();

        PingReply pong = PingReply.create(new GUID().bytes(), (byte)5);
        pc.addPong(pong);        

        Thread.sleep(2000);
        pongs = pc.getBestPongs();
        assertEquals("unexpected number of cached pongs", startSize+1, pongs.size());        

        pong = PingReply.create(new GUID().bytes(), (byte)5);
        pc.addPong(pong);        

        Thread.sleep(2000);
        pongs = pc.getBestPongs();
        assertEquals("unexpected number of cached pongs", startSize+2, pongs.size());  
    }
}
