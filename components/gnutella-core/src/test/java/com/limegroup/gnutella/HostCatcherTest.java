package com.limegroup.gnutella;

import junit.framework.*;
import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.settings.*;

public class HostCatcherTest extends com.limegroup.gnutella.util.BaseTestCase {  
    private HostCatcher hc;

    public HostCatcherTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(HostCatcherTest.class);
    }

    /** Returns a new HostCatcher connected to stubs.  YOU MAY WANT TO CALL
     *  EXPIRE to force bootstrap pongs. */
    public void setUp() {

        //explicitly allow all ips to test.
        FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(new String[] {});
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(
            new String[] { "*.*" });
        
        HostCatcher.DEBUG=true;
		new RouterService(new ActivityCallbackStub());

        hc = new HostCatcher();

        //move gnutella.dot before we initialize
        //test it specifically in other places.
        File gdotnet = new File(CommonUtils.getUserSettingsDir(), 
                                "gnutella.net");
        if ( gdotnet.exists() ) {
            File tmpFile = new File("gdotnet.tmp");
            tmpFile.delete();
            gdotnet.renameTo( tmpFile );
        }
        
        
		hc.initialize();		
    }

	public void tearDown() {
	    
	    // put the gnutella.dot file back.
	    File gtmp = new File( "gdotnet.tmp" );
	    if( gtmp.exists() ) {
            File gdotnet = new File(CommonUtils.getUserSettingsDir(), 
                                    "gnutella.net");
            gdotnet.delete();
            gtmp.renameTo( gdotnet );
	    }
	    
	}

    /**
     * Tests to make sure that recovering used hosts works as expected.  This
     * method is used when the user's network connection goes down.
     *
     * @throws Exception if any error occurs
     */    
    public void testRecoversUsedHosts() throws Exception {
        // write data to gnutella.net
        hc.add(new Endpoint("18.239.0.1"), false);
        hc.add(new Endpoint("18.239.0.2"), false);
        hc.add(new Endpoint("18.239.0.3"), false);
        hc.add(new Endpoint("18.239.0.4"), false);
        hc.add(new Endpoint("18.239.0.5"), false);
        hc.add(new Endpoint("18.239.0.6"), false);
        hc.add(new Endpoint("18.239.0.7"), false);
        hc.add(new Endpoint("18.239.0.8"), false);
        hc.add(new Endpoint("18.239.0.9"), false);
        hc.add(new Endpoint("18.239.0.10"), false);

        hc.write();
        int numHosts = hc.getNumHosts();
        
        System.out.println("HostCatcherTest::num hosts: "+numHosts);
        for(int i=0; i<10; i++) {
            hc.getAnEndpoint();
        }
        
        hc.recoverHosts();
        assertEquals("hosts should have been recovered", 
            numHosts, hc.getNumHosts());
    }
    
    /** Tests that FixedsizePriorityQueue can hold two endpoints with same
     *  priority but different ip's.  This was a problem at one point. */
    public void testEndpointPriorities() {
        Endpoint e1=new ExtendedEndpoint("18.239.0.146", 6346);
        Endpoint e2=new ExtendedEndpoint("18.239.0.147", 6347);
        assertNotEquals("e1 vs e2", e1, e2);
        assertNotEquals("e2 vs e1", e2, e1);
        assertEquals("compareTo on e1", 0, e1.compareTo(e2));
        assertEquals("compareTo on e2", 0, e2.compareTo(e1));
        
        FixedsizePriorityQueue queue=new FixedsizePriorityQueue(
            ExtendedEndpoint.priorityComparator(),
            10);
        assertNull(queue.insert(e1));
        assertNull(queue.insert(e2));
        assertEquals(2, queue.size());
    }


    public void testAddPriorities() {
        
        // Adding a private host should add 1 more to the numPrivateHosts...
        hc.add(new Endpoint("192.168.0.1"), false);
        assertEquals("private endpoint added as ultrapeer",
					 0, hc.getNumUltrapeerHosts());

        assertEquals("private endpoint added at all",
					 0, hc.getNumHosts());

        setUp();
        // Adding a normal host should add 1 more to numNormalHosts
        hc.add(new Endpoint("18.239.0.1"), false);
        assertEquals("normal endpoint added as ultrapeer",
                0, hc.getNumUltrapeerHosts());

        setUp();
        // Adding a ultrapeer should add 1 more to numUltrapeerHosts
        hc.add(new Endpoint("18.239.0.1"), true);
        assertEquals("ultrapeer endpoint not added as ultrapeer",
                1, hc.getNumUltrapeerHosts());

        //PingReply's.
        setUp();
        // Adding a private should add 1 more to numPrivateHosts
        hc.add(PingReply.createExternal(new byte[16], (byte)3, 6346, 
                                        new byte[] {(byte)192,(byte)168,(byte)0,(byte)1}, 
                                        false));
        assertEquals("private PingReply added as ultrapeer",
					 0 ,hc.getNumUltrapeerHosts());

        setUp();
        hc.add(PingReply.createExternal(new byte[16], (byte)3, 6346, 
                                        new byte[] {(byte)18,(byte)239,(byte)0,(byte)1},
                                        false));
        assertEquals("normal PingReply added as ultrapeer",
                0, hc.getNumUltrapeerHosts());


        setUp();
        hc.add(PingReply.createExternal(new byte[16], (byte)3, 6346, 
                                        new byte[] {(byte)18,(byte)239,(byte)0,(byte)1},
                                        true));
        assertEquals("ultrapeer PingReply not added as ultrapeer",
                1, hc.getNumUltrapeerHosts());
    }

    public void testIterators() {
        //System.out.println("-Testing iterators");

        Iterator iter=hc.getUltrapeerHosts(10);
        assertTrue("should not have Ultrapeer hosts", ! iter.hasNext());

        assertEquals("unexpected number of ultrapeer hosts", 
            0, hc.getNumUltrapeerHosts());
        hc.add(new Endpoint("18.239.0.1", 6346), true);
        assertEquals("unexpected number of ultrapeer hosts",
            1, hc.getNumUltrapeerHosts());
        hc.add(new Endpoint("18.239.0.2", 6346), true);
        hc.add(new Endpoint("128.103.60.1", 6346), false);
        hc.add(new Endpoint("128.103.60.2", 6346), false);
        assertEquals("unexpected number of ultrapeer hosts",
            2, hc.getNumUltrapeerHosts());

        iter=hc.getUltrapeerHosts(100);
        assertTrue(iter.hasNext());
        assertEquals("unexpected host",
            new Endpoint("18.239.0.2", 6346), iter.next());
        assertTrue(iter.hasNext());
        assertEquals("unexpected host",
            new Endpoint("18.239.0.1", 6346), iter.next());
        assertTrue(! iter.hasNext());

        iter=hc.getUltrapeerHosts(1);
        assertTrue(iter.hasNext());
        assertEquals("unexpected host",
            new Endpoint("18.239.0.2", 6346), iter.next());
        assertTrue(! iter.hasNext());
    }

    public void testPermanent() throws Exception {
        //Systm.out.println("-Testing write of permanent nodes to Gnutella.net");
        //1. Create HC, add entries, write to disk.
        hc.add(new Endpoint("18.239.0.141", 6341), false);//default time=345
        hc.add(PingReply.createExternal(GUID.makeGuid(), (byte)7, 6342,
                                        new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                                        1000,
                                        false));
        hc.add(PingReply.createExternal(GUID.makeGuid(), (byte)7, 6342,
                                        new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                                        1000,
                                        false));  //duplicate
        hc.add(PingReply.createExternal(GUID.makeGuid(), (byte)7, 6343,
                                        new byte[] {(byte)18, (byte)239, (byte)0, (byte)143},
                                        30,
                                        false));
        hc.add(PingReply.createExternal(GUID.makeGuid(), (byte)7, 6343,
                                        new byte[] {(byte)18, (byte)239, (byte)0, (byte)143},
                                        30,
                                        false));  //duplicate (well, with lower uptime)
        hc.add(PingReply.createExternal(GUID.makeGuid(), (byte)7, 6343,
                                        new byte[] {(byte)192, (byte)168, (byte)0, (byte)1},
                                        3000,
                                        false));  //private address (ignored)
        File tmp=File.createTempFile("hc_test", ".net" );
        hc.write(tmp);

        //2. read HC from file.
        setUp();
        hc.read(tmp);
        assertTrue("Got: "+hc.getNumHosts(), hc.getNumHosts()==3);
        assertEquals(new Endpoint("18.239.0.142", 6342),
                     hc.getAnEndpoint());
        assertEquals(new Endpoint("18.239.0.141", 6341),
                     hc.getAnEndpoint());
        assertEquals(new Endpoint("18.239.0.143", 6343),
                     hc.getAnEndpoint());
        assertEquals(0, hc.getNumHosts());

        //Cleanup.
        tmp.delete();
    }

    /** Tests that only the best hosts are remembered.  */
    public void testBestPermanent() throws Exception  {  
        HostCatcher.DEBUG=false;  //Too darn slow
        //1. Fill up host catcher with PERMANENT_SIZE+1 mid-level pongs
        //(various uptimes).
        final int N=HostCatcher.PERMANENT_SIZE;
        for (int i=0; i<=N; i++) {            
            hc.add(PingReply.createExternal(GUID.makeGuid(), (byte)7, i+1,
                                            new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                                            i+10,
                                            false));
        }
        //Now add bad pong--which isn't really added
        hc.add(PingReply.createExternal(GUID.makeGuid(), (byte)7, N+2,
                                        new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                                        0,
                                        false));
        //Now re-add port 1 (which was kicked out earlier).  Note that this
        //would fail if line 346 of HostCatcher were not executed.
        hc.add(PingReply.createExternal(GUID.makeGuid(), (byte)7, 1,
                                        new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                                        N+101,
                                        false));

        File tmp=File.createTempFile("hc_test", ".net" );
        hc.write(tmp);            

        //2. Read
        setUp();
        HostCatcher.DEBUG=false;  //Too darn slow
        hc.read(tmp);
        assertEquals(0, hc.getNumUltrapeerHosts());
        assertEquals(new Endpoint("18.239.0.142", 1),
                     hc.getAnEndpoint());
        // TODO: make sure this test is really working
        for (int i=N; i>601; i--) {
            assertGreaterThan("No more hosts after "+i, 0, hc.getNumHosts());
            assertEquals(new Endpoint("18.239.0.142", i+1),
                         hc.getAnEndpoint());
        }
        assertEquals(0, hc.getNumHosts());

        //Cleanup.
        tmp.delete();
    }

    /** Test that connection history is recorded. */
    public void testDoneWithConnect() throws Exception {
        hc.add(new Endpoint("18.239.0.1"), true);  
        hc.add(new Endpoint("18.239.0.2"), true);  //will succeed
        hc.add(new Endpoint("18.239.0.3"), true);  //will fail

        ExtendedEndpoint e3=(ExtendedEndpoint)hc.getAnEndpoint();
        assertEquals(new Endpoint("18.239.0.3"), e3);
        ExtendedEndpoint e2=(ExtendedEndpoint)hc.getAnEndpoint();
        assertEquals(new Endpoint("18.239.0.2"), e2);

        //record success (increases priority)
        hc.doneWithConnect(e2, true); 
        //record failure (lowers priority) with alternate form of method
        hc.doneWithConnect(e3, false);
        //Garbage (ignored)
        hc.doneWithConnect(new Endpoint("1.2.3.4", 6346), false);  
        hc.doneWithConnect(new Endpoint("18.239.0.3", 6349), true); //port

        //Check that permanent hosts are re-arranged.
        //Note that iterator yields worst to best.
        Iterator iter=hc.getPermanentHosts();
        ExtendedEndpoint e=(ExtendedEndpoint)iter.next();
        assertEquals(new Endpoint("18.239.0.3"), e);
        assertTrue(!e.getConnectionSuccesses().hasNext());
        assertTrue(e.getConnectionFailures().hasNext());

        e=(ExtendedEndpoint)iter.next();
        assertEquals(new Endpoint("18.239.0.1"), e);
        assertTrue(!e.getConnectionSuccesses().hasNext());
        assertTrue(!e.getConnectionFailures().hasNext());

        e=(ExtendedEndpoint)iter.next();
        assertEquals(new Endpoint("18.239.0.2"), e);
        assertTrue(e.getConnectionSuccesses().hasNext());
        assertTrue(!e.getConnectionFailures().hasNext());
    }

    public void testBadGnutellaDotNet() throws Exception {
        //System.out.println("-Testing bad Gnutella.net");
        //1. Write (mostly) corrupt file
        File tmp=File.createTempFile("hc_test", ".net" );
        FileWriter out=new FileWriter(tmp);
        out.write("18.239.0.141\n");                  //GOOD: port optional
        out.write("\n");                              //blank line
        out.write("18.239.0.144:A\n");                //bad port
        out.write("18.239.0.141:6347 A\n");           //bad uptime
        out.write("<html>total crap\n");              //not even close!
        out.write("  some garbage,1000,a,b,c,d,e,f,g\n");   //bad address
        out.write("18.239.0.142:6342,1000,a,b,c,d,e,f,g\n");//GOOD: ignore extra
        out.flush();
        out.close();

        //2. Read and verify
        setUp();
        hc.read(tmp);
        assertEquals("unexpected host",
            new Endpoint("18.239.0.142", 6342), hc.getAnEndpoint());
        assertEquals("unexpected host",
            new Endpoint("18.239.0.141", 6346), hc.getAnEndpoint());
        assertEquals("unexpected number of hosts", 0, hc.getNumHosts());
        assertEquals("unexpected number of ultrapeers",
            0, hc.getNumUltrapeerHosts());

        //Clean up
        tmp.delete();
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
}
