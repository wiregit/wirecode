package com.limegroup.gnutella;

import junit.framework.*;
import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.util.FixedsizePriorityQueue;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class HostCatcherTest extends TestCase {  
    private HostCatcher hc;

    public HostCatcherTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(HostCatcherTest.class);
    }

    /** Returns a new HostCatcher connected to stubs.  YOU MAY WANT TO CALL
     *  EXPIRE to force bootstrap pongs. */
    public void setUp() {
        HostCatcher.DEBUG=true;
        //This creates an acceptor thread.  We should probably use an Acceptor
        //stub or write a tearDown() method.
        hc=new HostCatcher(new ActivityCallbackStub());
        hc.initialize(new Acceptor(6346, null),
                      new ConnectionManager(null, null));
    }
    
    /** Tests that FixedsizePriorityQueue can hold two endpoints with same
     *  priority but different ip's.  This was a problem at one point. */
    public void testEndpointPriorities() {
        Endpoint e1=new ExtendedEndpoint("18.239.0.146", 6346);
        Endpoint e2=new ExtendedEndpoint("18.239.0.147", 6347);
        assertTrue(! e1.equals(e2));
        assertTrue(! e2.equals(e1));
        assertTrue(e1.compareTo(e2)==0);
        assertTrue(e2.compareTo(e1)==0);
        
        FixedsizePriorityQueue queue=new FixedsizePriorityQueue(
            ExtendedEndpoint.priorityComparator(),
            10);
        assertNull(queue.insert(e1));
        assertNull(queue.insert(e2));
        assertEquals(2, queue.size());
    }


    public void testAddPriorities() {
        //Endpoints.
        setUp();
        hc.add(new Endpoint("192.168.0.1"), false);
        assertTrue(hc.getNumUltrapeerHosts()==0);
        assertTrue(hc.getNumNormalHosts()==0);
        assertTrue(hc.getNumPrivateHosts()==1);

        setUp();
        hc.add(new Endpoint("18.239.0.1"), false);
        assertTrue(hc.getNumUltrapeerHosts()==0);
        assertTrue(hc.getNumNormalHosts()==1);
        assertTrue(hc.getNumPrivateHosts()==0);

        setUp();
        hc.add(new Endpoint("18.239.0.1"), true);
        assertTrue(hc.getNumUltrapeerHosts()==1);
        assertTrue(hc.getNumNormalHosts()==0);
        assertTrue(hc.getNumPrivateHosts()==0);

        //PingReply's.
        setUp();
        hc.add(new PingReply(new byte[16], (byte)3, 6346, 
                             new byte[] {(byte)192,(byte)168,(byte)0,(byte)1},
                             0, 0),
               null);
        assertTrue(hc.getNumUltrapeerHosts()==0);
        assertTrue(hc.getNumNormalHosts()==0);
        assertTrue(hc.getNumPrivateHosts()==1);

        setUp();
        hc.add(new PingReply(new byte[16], (byte)3, 6346, 
                             new byte[] {(byte)18,(byte)239,(byte)0,(byte)1},
                             0, 0),
               null);
        assertTrue(hc.getNumUltrapeerHosts()==0);
        assertTrue(hc.getNumNormalHosts()==1);
        assertTrue(hc.getNumPrivateHosts()==0);


        setUp();
        hc.add(new PingReply(new byte[16], (byte)3, 6346, 
                             new byte[] {(byte)18,(byte)239,(byte)0,(byte)1},
                             0, 0, true),
               null);
        assertTrue(hc.getNumUltrapeerHosts()==1);
        assertTrue(hc.getNumNormalHosts()==0);
        assertTrue(hc.getNumPrivateHosts()==0);
    }

    public void testBootstraps() {
        try {
            SettingsManager.instance().setQuickConnectHosts(
                new String[] { "r1.b.c.d:6346", "r2.b.c.d:6347", "crap:crap"});
            hc.expire();

            hc.add(new Endpoint("128.103.60.3", 6346), false);
            hc.add(new Endpoint("128.103.60.2", 6346), false);
            hc.add(new Endpoint("128.103.60.1", 6346), false);

            Endpoint router1=hc.getAnEndpoint();
            assertTrue(router1.equals(new Endpoint("r1.b.c.d", 6346)));         
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.1", 6346)));
            hc.add(new Endpoint("18.239.0.144", 6346), true);
            hc.doneWithConnect(router1, false);    //got pong
            hc.doneWithMessageLoop(router1);
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("18.239.0.144", 6346)));        

            Endpoint router2=hc.getAnEndpoint();
            assertTrue(router2.equals(new Endpoint("r2.b.c.d", 6347)));        
            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.2", 6346)));        
            hc.doneWithConnect(router2, false);
            hc.doneWithMessageLoop(router2);

            assertTrue(hc.getAnEndpoint().equals(
                new Endpoint("128.103.60.3", 6346))); //no more bootstraps
        } catch (InterruptedException e) {
            assertTrue("Mysterious InterruptedException", false);
        }
    }

    public void testExpireBig() {
        try {
            HostCatcher.DEBUG=false;
            final int SIZE=100;
            assertTrue(SIZE>HostCatcher.HOSTS_BEFORE_BOOTSTRAP);
            //Tests that some messages are copied from GOOD priority to NORMAL,
            //but some aren't.
            SettingsManager.instance().setQuickConnectHosts(
                 new String[] { "r1.b.c.d:6346", "r2.b.c.d:6347"});
            for (int i=SIZE; i>=1; i--) 
                hc.add(new Endpoint("18.239.0.144", i), true);
            assertEquals(SIZE, 
                         hc.getNumUltrapeerHosts());

            hc.expire();

            assertEquals(HostCatcher.HOSTS_BEFORE_BOOTSTRAP, 
                         hc.getNumUltrapeerHosts());
            int i=1;
            for (; i<=HostCatcher.HOSTS_BEFORE_BOOTSTRAP; i++)
                assertEquals(new Endpoint("18.239.0.144", i),
                             hc.getAnEndpoint());
            assertTrue(hc.getAnEndpoint().equals(new Endpoint("r1.b.c.d", 6346)));
            for (; i<=SIZE; i++)
                assertEquals(new Endpoint("18.239.0.144", i),
                             hc.getAnEndpoint());
            assertEquals(0, hc.getNumHosts());
        } catch (InterruptedException e) { 
            assertTrue("Mysterious InterruptedException", false);
        }
    }

    public void testExpireSmall() {
        try {
            HostCatcher.DEBUG=false;
            final int SIZE=5;
            assertTrue(SIZE<HostCatcher.HOSTS_BEFORE_BOOTSTRAP);
            //Tests that some messages are copied from GOOD priority to NORMAL,
            //but some aren't.
            SettingsManager.instance().setQuickConnectHosts(
                 new String[] { "r1.b.c.d:6346", "r2.b.c.d:6347"});
            for (int i=SIZE; i>=1; i--) 
                hc.add(new Endpoint("18.239.0.144", i), true);
            assertEquals(SIZE, 
                         hc.getNumUltrapeerHosts());

            hc.expire();

            assertEquals(SIZE, hc.getNumUltrapeerHosts());            
            for (int i=1; i<=SIZE; i++)
                assertEquals(new Endpoint("18.239.0.144", i),
                             hc.getAnEndpoint());
            assertTrue(hc.getAnEndpoint().equals(new Endpoint("r1.b.c.d", 6346)));
            assertEquals(0, hc.getNumHosts());
        } catch (InterruptedException e) { 
            assertTrue("Mysterious InterruptedException", false);
        }
    }

    /** Ensures threads woken up when waiting for a bootstrap host. */
    public void testExpireEmpty() {
        //Start fetcher and give it time to block in getAnEndpoint().
        SettingsManager.instance().setQuickConnectHosts(new String[0]);
        TestFetcher fetcher=new TestFetcher();
        fetcher.start();
        try { Thread.sleep(100); } catch (InterruptedException e) { }

        //Make sure fetcher hasn't mad progress.
        assertNull(fetcher.result);

        //Add quick-connect hosts and notify.
        SettingsManager.instance().setQuickConnectHosts(
            new String[] {"r1.b.c.d:6346"});
        hc.expire();

        //Make sure fetcher has made progress.  Timeout just prevents test from
        //blocking.
        try {
            fetcher.join(1000);
        } catch (InterruptedException e) { }
        assertEquals(new Endpoint("r1.b.c.d", 6346), fetcher.result);
    }

    class TestFetcher extends Thread {
        volatile Endpoint result;
        public void run() {
            try {
                result=hc.getAnEndpoint();
            } catch (InterruptedException e) { }
        }
    }


    public void testIterators() {
        //System.out.println("-Testing iterators");

        Iterator iter=hc.getNormalHosts(10);
        assertTrue(! iter.hasNext());
        iter=hc.getUltrapeerHosts(10);
        assertTrue(! iter.hasNext());

        assertTrue(hc.getNumUltrapeerHosts()==0);
        hc.add(new Endpoint("18.239.0.1", 6346), true);
        assertTrue(hc.getNumUltrapeerHosts()==1);
        hc.add(new Endpoint("18.239.0.2", 6346), true);
        hc.add(new Endpoint("128.103.60.1", 6346), false);
        hc.add(new Endpoint("128.103.60.2", 6346), false);
        assertTrue(hc.getNumUltrapeerHosts()==2);

        iter=hc.getUltrapeerHosts(100);
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("18.239.0.2", 6346)));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("18.239.0.1", 6346)));
        assertTrue(! iter.hasNext());

        iter=hc.getUltrapeerHosts(1);
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("18.239.0.2", 6346)));
        assertTrue(! iter.hasNext());

        iter=hc.getNormalHosts(100);
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("128.103.60.2", 6346)));
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("128.103.60.1", 6346)));
        assertTrue(! iter.hasNext());

        iter=hc.getNormalHosts(1);
        assertTrue(iter.hasNext());
        assertTrue(iter.next().equals(new Endpoint("128.103.60.2", 6346)));
        assertTrue(! iter.hasNext());
    }

    public void testPermanent() {
        //System.out.println("-Testing write of permanent nodes to Gnutella.net");
        try {
            //1. Create HC, add entries, write to disk.
            hc.add(new Endpoint("18.239.0.141", 6341), false);//default time=345
            hc.add(new PingReply(GUID.makeGuid(), (byte)7, 6342,
                          new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                          0l, 0l, false, 1000),
                   null);
            hc.add(new PingReply(GUID.makeGuid(), (byte)7, 6342,
                          new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                          0l, 0l, false, 1000),
                   null);  //duplicate
            hc.add(new PingReply(GUID.makeGuid(), (byte)7, 6343,
                          new byte[] {(byte)18, (byte)239, (byte)0, (byte)143},
                          0l, 0l, false, 30),
                   null);
            hc.add(new PingReply(GUID.makeGuid(), (byte)7, 6343,
                          new byte[] {(byte)18, (byte)239, (byte)0, (byte)143},
                          0l, 0l, false, 30),
                   null);  //duplicate (well, with lower uptime)
            hc.add(new PingReply(GUID.makeGuid(), (byte)7, 6343,
                          new byte[] {(byte)192, (byte)168, (byte)0, (byte)1},
                          0l, 0l, false, 3000),
                   null);  //private address (ignored)
            File tmp=File.createTempFile("hc_test", ".net" );
            hc.write(tmp.getAbsolutePath());

            //2. read HC from file.
            SettingsManager.instance().setQuickConnectHosts(new String[0]);
            setUp();
            hc.read(tmp.getAbsolutePath());
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
        } catch (IOException e) {
            assertTrue("Unexpected IO problem: "+e, false);
        } catch (InterruptedException e) {
            assertTrue("Unexpected InterruptedException "+e, false);
        }
    }

    /** Tests that only the best hosts are remembered.  */
    public void testBestPermanent() {  
        HostCatcher.DEBUG=false;  //Too darn slow
        try {
            //1. Fill up host catcher with PERMANENT_SIZE+1 mid-level pongs
            //(various uptimes).
            final int N=HostCatcher.PERMANENT_SIZE;
            for (int i=0; i<=N; i++) {
                hc.add(new PingReply(GUID.makeGuid(), (byte)7, i,
                           new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                           0l, 0l, false, i+10),
                       null);
            }
            //Now add bad pong--which isn't really added
            hc.add(new PingReply(GUID.makeGuid(), (byte)7, N+1,
                           new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                           0l, 0l, false, 0),
                       null);
            //Now re-add port 0 (which was kicked out earlier).  Note that this
            //would fail if line 346 of HostCatcher were not executed.
            hc.add(new PingReply(GUID.makeGuid(), (byte)7, 0,
                           new byte[] {(byte)18, (byte)239, (byte)0, (byte)142},
                           0l, 0l, false, N+100),
                   null);

            File tmp=File.createTempFile("hc_test", ".net" );
            hc.write(tmp.getAbsolutePath());            

            //2. Read
            SettingsManager.instance().setQuickConnectHosts(new String[0]);
            setUp();
            HostCatcher.DEBUG=false;  //Too darn slow
            hc.read(tmp.getAbsolutePath());
            assertEquals(HostCatcher.HOSTS_BEFORE_BOOTSTRAP,
                         hc.getNumUltrapeerHosts());
            assertEquals(new Endpoint("18.239.0.142", 0),
                         hc.getAnEndpoint());
            for (int i=N; i>1; i--) {
                assertTrue("No more hosts after "+i, hc.getNumHosts()>0);
                assertEquals(new Endpoint("18.239.0.142", i),
                             hc.getAnEndpoint());
            }
            assertEquals(0, hc.getNumHosts());

            //Cleanup.
            tmp.delete();
        } catch (IOException e) {
            assertTrue("Unexpected IO problem: "+e, false);
        } catch (InterruptedException e) {
            assertTrue("Unexpected InterruptedException "+e, false);
        }
    }

    /** Test that connection history is recorded. */
    public void testDoneWithConnect() {
        try {
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
        } catch (InterruptedException fail) {
            fail("InterruptedException");
        }
    }

    public void testBadGnutellaDotNet() {
        //System.out.println("-Testing bad Gnutella.net");
        try {
            //1. Write (mostly) corrupt file
            File tmp=File.createTempFile("hc_test", ".net" );
            FileWriter out=new FileWriter(tmp);
            out.write("18.239.0.141\n");                  //GOOD: port optional
            out.write("\n");                              //blank line
            out.write("18.239.0.144:A\n");                //bad port
            out.write("18.239.0.141:6347 A\n");           //bad uptime
            out.write("18.239.0.142:6342,1000,a,b,c,d,e,f,g\n");//GOOD: ignore extra
            out.flush();
            out.close();

            //2. Read and verify
            SettingsManager.instance().setQuickConnectHosts(new String[0]);
            setUp();
            hc.read(tmp.getAbsolutePath());
            assertTrue(hc.getAnEndpoint().equals( 
                new Endpoint("18.239.0.142", 6342)));
            assertTrue(hc.getAnEndpoint().equals( 
                new Endpoint("18.239.0.141", 6346)));
            assertTrue(hc.getNumHosts()==0);
            assertTrue(hc.getNumUltrapeerHosts()==0);

            //Clean up
            tmp.delete();
        } catch (IOException e) { 
            assertTrue("Unexpected IO problem", false);
        } catch (InterruptedException e) {
            assertTrue("Unexpected InterruptedException "+e, false);
        }
    }

    /** Tests the randomization when large initial gnutella.net involved. */
    public void testRandomizedGnutellaDotNet() {
        //System.out.println("-Testing bad Gnutella.net");
        try {
            //1. Write initial gnutella.net file in order
            File tmp=File.createTempFile("hc_test", ".net" );
            FileWriter out=new FileWriter(tmp);
            for (int i=1; i<=10000; i++)
                out.write("18.239.0."+(i%255)+":"+i+"\n");
            out.flush();
            out.close();

            //2. Read
            SettingsManager.instance().setQuickConnectHosts(new String[0]);
            setUp();
            HostCatcher.DEBUG=false;  //Too darn slow
            hc.read(tmp.getAbsolutePath());

            assertEquals(HostCatcher.NORMAL_SIZE, hc.getNumHosts());
            assertEquals(HostCatcher.HOSTS_BEFORE_BOOTSTRAP, 
                         hc.getNumUltrapeerHosts());

            //3. Verify it's random.  This is hard to check.  We just verify
            //that that the first three entries aren't the last three entries
            //from gnutella.net.  The probability of there order remaining
            //unchanged by the shuffle is very small.
            Endpoint e1=hc.getAnEndpoint();  
            Endpoint e2=hc.getAnEndpoint();
            Endpoint e3=hc.getAnEndpoint();
            assertTrue(   !(new Endpoint("18.239.0.235", 1000)).equals(e1)
                       || !(new Endpoint("18.239.0.234", 999)).equals(e2)
                       || !(new Endpoint("18.239.0.233", 998)).equals(e3));
            //Clean up
            tmp.delete();
        } catch (IOException e) { 
            assertTrue("Unexpected IO problem", false);
        } catch (InterruptedException e) {
            assertTrue("Unexpected InterruptedException "+e, false);
        }
    }

    public static void main(String argv[]) {
        junit.textui.TestRunner.run(suite());
    }
}
