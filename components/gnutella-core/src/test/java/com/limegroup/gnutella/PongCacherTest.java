package com.limegroup.gnutella;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.collection.BucketQueue;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.settings.ApplicationSettings;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.MessageTestUtils;
import com.limegroup.gnutella.util.UltrapeerConnectionManager;

/**
 * Tests the <tt>PongCacher</tt> class that maintains a cache of the best most
 * recent pongs seen.
 */
@SuppressWarnings( { "unchecked", "cast" } )
public final class PongCacherTest extends LimeTestCase {


    
    private static final PongCacher PC = ProviderHacks.getPongCacher();

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
        Map m = 
            (Map)PrivilegedAccessor.getValue(PC, "PONGS");
        if(m != null)
            m.clear();
    }

    public static void globalSetUp() throws Exception {
        ProviderHacks.getAcceptor().setAddress(InetAddress.getLocalHost());
    }

    /**
     * Test to make sure that expiring of pongs is working correctly.
     * @throws Exception
     */
    public void testPongExpiring() throws Exception {
        // Trick us into thinking we're an Ultrapeer.
      //  PrivilegedAccessor.setValue(RouterService.class, "manager",
      //      new TestManager());
        
        // Create a pong with the correct GGEP for our cacher to accept it.
        PingReply pr = MessageTestUtils.createPongWithFreeLeafSlots();
        ProviderHacks.getPongCacher().addPong(pr);
        
        // Make sure we get the pong successfully.
        List pongs = ProviderHacks.getPongCacher()
            .getBestPongs(ApplicationSettings.LANGUAGE.getValue());
        assertEquals("should be 1 pong",1,pongs.size());
        Iterator iter = pongs.iterator();
        PingReply retrievedPong = (PingReply)iter.next();
        assertEquals("unexpected pong", pr, retrievedPong);

        // Make sure we still get the pong successfully on a second pass.
        pongs = ProviderHacks.getPongCacher()
            .getBestPongs(ApplicationSettings.LANGUAGE.getValue());
        assertEquals("should be 1 pong",1,pongs.size());
        iter = pongs.iterator();
        retrievedPong = (PingReply)iter.next();
        assertEquals("unexpected pong", pr, retrievedPong);

        // Create a second pong with the correct GGEP for our cacher
        // to accept it.
        PingReply pr2 = MessageTestUtils.createPongWithFreeLeafSlots();
        pr2.hop();
        ProviderHacks.getPongCacher().addPong(pr2);
        
        // Make sure we get the 2 pongs successfully in the correct order.
        pongs = ProviderHacks.getPongCacher().getBestPongs(ApplicationSettings.DEFAULT_LOCALE.getValue());
        assertEquals("should be 2 pongs",2,pongs.size());
        assertContains("no p2", pongs, pr2);
        assertContains("no p", pongs, pr);

        
        // Finally, make sure the pong expires on a sleep -- add a bit to the
        // sleep to avoid thread scheduling craziness.
        Thread.sleep(PongCacher.EXPIRE_TIME+800);
        pongs = ProviderHacks.getPongCacher()
            .getBestPongs(ApplicationSettings.LANGUAGE.getValue());
        assertEquals("list should be empty", 0, pongs.size());
        
    }

    /**
     * Tests the method for getting the best set of pongs.
     */
    public void testGetBestPongs() throws Exception {
        @SuppressWarnings("all") // DPINJ: textfix
        ConnectionManager cm = new UltrapeerConnectionManager();
    //    PrivilegedAccessor.setValue(RouterService.class, "manager", cm);    
        
        List pongs = PC.getBestPongs(ApplicationSettings.LANGUAGE.getValue());

        PingReply pong = ProviderHacks.getPingReplyFactory().create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);        

        pongs = PC.getBestPongs(ApplicationSettings.LANGUAGE.getValue());
        assertEquals("unexpected number of cached pongs", 
                     1, pongs.size());        

        pong = ProviderHacks.getPingReplyFactory().create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);        


        pongs = PC.getBestPongs(ApplicationSettings.LANGUAGE.getValue());
        assertEquals("unexpected number of cached pongs", 
                     1, pongs.size());  

        // fill up the pongs at the default hop
        for(int i=0; i<30; i++) {
            PingReply curPong = 
                ProviderHacks.getPingReplyFactory().create(new GUID().bytes(), (byte)5);
            PC.addPong(curPong);
        }

        pongs = PC.getBestPongs(ApplicationSettings.LANGUAGE.getValue());

        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_PONGS_PER_HOP, pongs.size());

        PingReply highHopPong = 
            ProviderHacks.getPingReplyFactory().create(new GUID().bytes(), (byte)5);
        
        highHopPong.hop();
        highHopPong.hop();
        PC.addPong(highHopPong);

        //Thread.sleep(PongCacher.REFRESH_INTERVAL+200);
        pongs = PC.getBestPongs(ApplicationSettings.LANGUAGE.getValue());
        assertEquals("unexpected number of cached pongs", 
                     PongCacher.NUM_PONGS_PER_HOP+1, pongs.size());


        Iterator iter = pongs.iterator();
        PingReply pr = (PingReply)iter.next();
        assertEquals("first pong should be high hops", highHopPong, pr); 

        PingReply highHopPong2 = 
            ProviderHacks.getPingReplyFactory().create(new GUID().bytes(), (byte)5);
        highHopPong2.hop();
        highHopPong2.hop();
        highHopPong2.hop();
        highHopPong2.hop();
        PC.addPong(highHopPong2);

        //Thread.sleep(PongCacher.REFRESH_INTERVAL+200);
        pongs = PC.getBestPongs(ApplicationSettings.LANGUAGE.getValue());
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
        @SuppressWarnings("all") // DPINJ: textfix
        ConnectionManager cm = new UltrapeerConnectionManager();
    //    PrivilegedAccessor.setValue(RouterService.class, "manager", cm);    

        PingReply pong = ProviderHacks.getPingReplyFactory().create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);

        Map m = 
            (Map)PrivilegedAccessor.getValue(PongCacher.class,
                                             "PONGS");
        BucketQueue bq = 
            (BucketQueue)m.get(pong.getClientLocale());
        assertEquals("unexpected bucket queue size", 1, bq.size());

        pong = ProviderHacks.getPingReplyFactory().create(new GUID().bytes(), (byte)5);
        PC.addPong(pong);
        assertEquals("unexpected bucket queue size", 1, bq.size());
        assertEquals("unexpected bucket queue size", 1, bq.size(0));

        for(int i=bq.size(0); i<PongCacher.NUM_PONGS_PER_HOP+2; i++) {
            PC.addPong(pong);
        }
        
        assertEquals("unexpected bucket queue size", 
                     PongCacher.NUM_PONGS_PER_HOP, bq.size(0));
    }
    

    /**
     * Tests the locale preferencing of PongCacher.
     */
    public void testLocalePong() throws Exception {
        // Trick us into thinking we're an Ultrapeer.
     //   PrivilegedAccessor.setValue(RouterService.class, "manager",
      //                              new TestManager());
        
        // Create a pong with the correct GGEP for our cacher to accept it.
        PingReply pr = MessageTestUtils.createPongWithFreeLeafSlots();
        PrivilegedAccessor.setValue((Object)pr,
                                    "CLIENT_LOCALE",
                                    "en");
        PrivilegedAccessor.setValue((Object)pr,
                                    "hops",
                                    new Byte((byte)1));
        ProviderHacks.getPongCacher().addPong(pr);
        
        PingReply prj = MessageTestUtils.createPongWithFreeLeafSlots();
        PrivilegedAccessor.setValue((Object)prj,
                                    "CLIENT_LOCALE",
                                    "ja");
        PrivilegedAccessor.setValue((Object)prj,
                                    "hops",
                                    new Byte((byte)1));
        ProviderHacks.getPongCacher().addPong(prj);
        
        PingReply prj2 = MessageTestUtils.createPongWithFreeLeafSlots();
        PrivilegedAccessor.setValue((Object)prj2,
                                    "CLIENT_LOCALE",
                                    "ja");
        PrivilegedAccessor.setValue((Object)prj2,
                                    "hops",
                                    new Byte((byte)2));
        ProviderHacks.getPongCacher().addPong(prj2);

        //should only return en (en)
        List pongs = ProviderHacks.getPongCacher().getBestPongs("en");
        assertEquals("unexpected size returned from PongCacher when asking for en locale pongs",
                     1, pongs.size());
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(0)).getClientLocale(),
                     "en");
        
        //should return "ja" pongs in the beggining (ja, ja, en)
        pongs = ProviderHacks.getPongCacher().getBestPongs("ja");
        assertEquals("unexpected size returned from PongCacher when asking for ja locale pongs",
                     3, pongs.size());
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(0)).getClientLocale(),
                     "ja");
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(1)).getClientLocale(),
                     "ja");
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(2)).getClientLocale(),
                     "en");


        //expire default locale pong but the "ja" locale pongs should be
        //around
        Thread.sleep(PongCacher.EXPIRE_TIME+800);
        pongs = ProviderHacks.getPongCacher().getBestPongs("ja");
        assertEquals("unexpected size returned from PongCacher when asking for ja locale pongs",
                     2, pongs.size());
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(0)).getClientLocale(),
                     "ja");
        assertEquals("pong's locale doesn't match",
                     ((PingReply)pongs.get(1)).getClientLocale(),
                     "ja");
    }

    
    @SuppressWarnings("all") // DPINJ: textfix
    private static class TestManager extends HackConnectionManager {


        public boolean isSupernode() {
            return true;
        }
    }
}










