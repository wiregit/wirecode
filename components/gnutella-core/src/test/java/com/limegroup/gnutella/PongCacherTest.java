package com.limegroup.gnutella;

import java.net.InetAddress;

import junit.framework.Test;

import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.security.ServerAuthenticator;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.BucketQueue;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.UltrapeerConnectionManager;
import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.List;

/**
 * Tests the <tt>PongCacher</tt> class that maintains a cache of the best most
 * recent pongs seen.
 */
public final class PongCacherTest extends BaseTestCase {

    /**
     * Cached constant for the vendor GGEP extension.
     */
    private static final byte[] CACHED_VENDOR = new byte[5];
    
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

    public void setUp() throws Exception {
        BucketQueue bq = 
            (BucketQueue)PrivilegedAccessor.getValue(PC, "PONGS");
        if(bq != null) {
            bq.clear();
        }
    }

    public static void globalSetUp() throws Exception {
        RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());
    }

    /**
     * Test to make sure that expiring of pongs is working correctly.
     * @throws Exception
     */
    public void testPongExpiring() throws Exception {
        // Trick us into thinking we're an Ultrapeer.
        PrivilegedAccessor.setValue(RouterService.class, "manager",
            new TestManager());
        
        // Create a pong with the correct GGEP for our cacher to accept it.
        GGEP ggep = newGGEP(10, true, true);
        PingReply pr = PingReply.create(GUID.makeGuid(), (byte)2, 
            10, new byte[]{20,2,4,3}, 10, 10, true, ggep);
        
        PongCacher.instance().addPong(pr);
        
        // Make sure we get the pong successfully.
        List pongs = PongCacher.instance().getBestPongs();
        assertEquals("should be 1 pong",1,pongs.size());
        Iterator iter = pongs.iterator();
        PingReply retrievedPong = (PingReply)iter.next();
        assertEquals("unexpected pong", pr, retrievedPong);

        // Make sure we still get the pong successfully on a second pass.
        pongs = PongCacher.instance().getBestPongs();
        assertEquals("should be 1 pong",1,pongs.size());
        iter = pongs.iterator();
        retrievedPong = (PingReply)iter.next();
        assertEquals("unexpected pong", pr, retrievedPong);
        
        // Finally, make sure the pong expires on a sleep -- add a bit to the
        // sleep to avoid thread scheduling craziness.
        Thread.sleep(PongCacher.EXPIRE_TIME+800);
        pongs = PongCacher.instance().getBestPongs();
        assertEquals("list should be empty", 0, pongs.size());
        
    }

    /**
     * Tests the method for getting the best set of pongs.
     */
    public void testGetBestPongs() throws Exception {
        ConnectionManager cm = 
            new UltrapeerConnectionManager(new ServerAuthenticator());
        PrivilegedAccessor.setValue(RouterService.class, "manager", cm);    
        
        List pongs = PC.getBestPongs();

        PingReply pong = PingReply.create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);        

        pongs = PC.getBestPongs();
        assertEquals("unexpected number of cached pongs", 
                     1, pongs.size());        

        pong = PingReply.create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);        


        pongs = PC.getBestPongs();
        assertEquals("unexpected number of cached pongs", 
                     1, pongs.size());  

        // fill up the pongs at the default hop
        for(int i=0; i<30; i++) {
            PingReply curPong = 
                PingReply.create(new GUID().bytes(), (byte)5);
            PC.addPong(curPong);
        }

        pongs = PC.getBestPongs();

        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_PONGS_PER_HOP, pongs.size());

        PingReply highHopPong = 
            PingReply.create(new GUID().bytes(), (byte)5);
        
        highHopPong.hop();
        highHopPong.hop();
        PC.addPong(highHopPong);

        //Thread.sleep(PongCacher.REFRESH_INTERVAL+200);
        pongs = PC.getBestPongs();
        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_PONGS_PER_HOP+1, pongs.size());


        Iterator iter = pongs.iterator();
        PingReply pr = (PingReply)iter.next();
        assertEquals("first pong should be high hops", highHopPong, pr); 

        PingReply highHopPong2 = 
            PingReply.create(new GUID().bytes(), (byte)5);
        highHopPong2.hop();
        highHopPong2.hop();
        highHopPong2.hop();
        highHopPong2.hop();
        PC.addPong(highHopPong2);

        //Thread.sleep(PongCacher.REFRESH_INTERVAL+200);
        pongs = PC.getBestPongs();
        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_PONGS_PER_HOP+2, pongs.size());   

        iter = pongs.iterator();
        pr = (PingReply)iter.next();
        assertEquals("first pong should be high hops", highHopPong2, pr); 
        
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
                                                     "PONGS");
        assertEquals("unexpected bucket queue size", 1, bq.size());

        pong = PingReply.create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);
        assertEquals("unexpected bucket queue size", 1, bq.size());
        assertEquals("unexpected bucket queue size", 1, bq.size(0));

        for(int i=bq.size(0); i<PongCacher.NUM_PONGS_PER_HOP+2; i++) {
            PC.addPong(pong);
        }
        
        assertEquals("unexpected bucket queue size", 
                     PongCacher.NUM_PONGS_PER_HOP, bq.size(0));
    }
    
    /** Returns the GGEP payload bytes to encode the given uptime */
    private static GGEP newGGEP(int dailyUptime, boolean isUltrapeer,
                                boolean isGUESSCapable) {
        GGEP ggep=new GGEP(true);
        
        if (dailyUptime >= 0)
            ggep.put(GGEP.GGEP_HEADER_DAILY_AVERAGE_UPTIME, dailyUptime);
        
        if (isGUESSCapable && isUltrapeer) {
            // indicate guess support
            byte[] vNum = {
                convertToGUESSFormat(CommonUtils.getGUESSMajorVersionNumber(),
                                     CommonUtils.getGUESSMinorVersionNumber())};
            ggep.put(GGEP.GGEP_HEADER_UNICAST_SUPPORT, vNum);
        }
        
        if (isUltrapeer) { 
            // indicate UP support
            addUltrapeerExtension(ggep);
        }
        
        // all pongs should have vendor info
        ggep.put(GGEP.GGEP_HEADER_VENDOR_INFO, CACHED_VENDOR); 

        return ggep;
    }


    /**
     * Adds the ultrapeer GGEP extension to the pong.  This has the version of
     * the Ultrapeer protocol that we support as well as the number of free
     * leaf and Ultrapeer slots available.
     * 
     * @param ggep the <tt>GGEP</tt> instance to add the extension to
     */
    private static void addUltrapeerExtension(GGEP ggep) {
        byte[] payload = new byte[3];
        // put version
        payload[0] = convertToGUESSFormat(CommonUtils.getUPMajorVersionNumber(),
                                          CommonUtils.getUPMinorVersionNumber()
                                          );
        payload[1] = (byte)10;
        payload[2] = (byte)10;

        // add it
        ggep.put(GGEP.GGEP_HEADER_UP_SUPPORT, payload);
    }

    /** 
     * puts major as the high order bits, minor as the low order bits.
     * @exception IllegalArgumentException thrown if major/minor is greater 
     *  than 15 or less than 0.
     */
    private static byte convertToGUESSFormat(int major, int minor) 
        throws IllegalArgumentException {
        if ((major < 0) || (minor < 0) || (major > 15) || (minor > 15))
            throw new IllegalArgumentException();
        // set major
        int retInt = major;
        retInt = retInt << 4;
        // set minor
        retInt |= minor;

        return (byte) retInt;
    }
    
    private static class TestManager extends ConnectionManager {
        /**
         * @param authenticator
         */
        public TestManager() {
            super(null);
        }

        public boolean isSupernode() {
            return true;
        }
    }
}










