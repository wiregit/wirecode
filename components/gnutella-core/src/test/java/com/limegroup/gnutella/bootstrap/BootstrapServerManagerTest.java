package com.limegroup.gnutella.bootstrap;

import junit.framework.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.text.ParseException;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;

/**
 * Unit tests for BootstrapServerManager.
 */
public class BootstrapServerManagerTest extends TestCase {
    public BootstrapServerManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(BootstrapServerManagerTest.class);
    }

    /////////////////////////////////////////////////////////////////////////////

    final static int PORT=6700;
    final static String DIRECTORY="/path/to/script.php";
    /** Our backend. */
    TestBootstrapServerManager bman;
    TestHostCatcher catcher;
    /** Our servers. */
    TestBootstrapServer s1;
    TestBootstrapServer s2;
    TestBootstrapServer s3;

    public void setUp() throws IOException, ParseException {
        //Prepare backend
        catcher=new TestHostCatcher();
        bman=new TestBootstrapServerManager(catcher);
        bman.addBootstrapServer(
            new BootstrapServer("http://127.0.0.1:"+(PORT)+DIRECTORY));
        bman.addBootstrapServer(
            new BootstrapServer("http://127.0.0.1:"+(PORT+1)+DIRECTORY));
        bman.addBootstrapServer(
            new BootstrapServer("http://127.0.0.1:"+(PORT+2)+DIRECTORY));

        //Prepare servers
        s1=new TestBootstrapServer(PORT);
        s2=new TestBootstrapServer(PORT+1);
        s3=new TestBootstrapServer(PORT+2);
    }

    public void tearDown() {
        s1.shutdown();
        s2.shutdown();
        s3.shutdown();
    }

    ///////////////////////////////////////////////////////////////////////

    public void testFetchEndpointsAsync() {
        final int SIZE=12;
        //Preparse server.
        StringBuffer response=new StringBuffer();
        for (int i=0; i<SIZE; i++)
            response.append("1.2.3."+i+":6346"+(i<5 ? "\r\n" : "\n"));
        s1.setResponseData(response.toString());

        //Connect.  Wait for data.
        bman.fetchEndpointsAsync();
        sleep();

        //Check that backend sent right requests...
        assertEquals("GET "+DIRECTORY+"?hostfile=1 HTTP/1.1", s1.getRequest());
        assertEquals(null, s2.getRequest());   //wasn't contacted
        assertEquals(null, s3.getRequest());   //wasn't contacted
        //...and got right results.
        for (int i=0; i<SIZE; i++) 
            assertEquals(new Endpoint("1.2.3."+i+":6346"),
                         catcher.list.get(i));
    }


    public void testFetchBootstrapServersAsync() throws ParseException {
        final int SIZE=12;
        //Preparse server.
        StringBuffer response=new StringBuffer();
        for (int i=0; i<SIZE; i++)
            response.append("http://1.2.3."+i+"/script.php"+(i<5 ? "\r\n" : "\n"));
        s1.setResponseData(response.toString());

        //Connect.  Wait for data.
        bman.fetchBootstrapServersAsync();
        sleep();

        //Check that backend sent right request...
        assertEquals("GET "+DIRECTORY+"?urlfile=1 HTTP/1.1", s1.getRequest());
        assertEquals(null, s2.getRequest());   //wasn't contacted
        assertEquals(null, s3.getRequest());   //wasn't contacted
        //...kept old server list...
        Iterator iter=bman.getBootstrapServers();
        assertEquals(new BootstrapServer("http://127.0.0.1:"+(PORT)+DIRECTORY),
                     iter.next());
        assertEquals(new BootstrapServer("http://127.0.0.1:"+(PORT+1)+DIRECTORY),
                     iter.next());
        assertEquals(new BootstrapServer("http://127.0.0.1:"+(PORT+2)+DIRECTORY),
                     iter.next());
        //...and added new servers.
        for (int i=0; i<SIZE; i++) {
            assertTrue(iter.hasNext());
            assertEquals(iter.next(),
                         new BootstrapServer("http://1.2.3."+i+"/script.php"));
        }
        assertTrue(! iter.hasNext());
    }

    private void sleep() {
        //Wait 0.5 second--that should be long enough for request to happen.
        try { Thread.sleep(500); } catch (InterruptedException e) { }
    }

    //urlfile=1 and hostfile=1 requests return right values
    //try hosts until we get "enough" endpoints, skipping any failed hosts
    //unreachable or error hosts removed from cache
    //gracefully skip bad data.  Various newline combos (trailing at file too)
    //test no infinite loops
    //handles 303 redirects properly
    //test real randomness
}

/** Overrides the add(Endpoint, boolean) method) */
class TestHostCatcher extends HostCatcher {
    List /* of Endpoint */ list=new ArrayList(20);

    public TestHostCatcher() {
        super(new ActivityCallbackStub());
    }

    public boolean add(Endpoint e, boolean forceHighPriority) {
        list.add(e);
        return true;
    }
}

/** A BootstrapServerManager that tries host in a round-robin fashion. */
class TestBootstrapServerManager extends BootstrapServerManager {
    public TestBootstrapServerManager(HostCatcher hc) {
        super(hc);
    }

    private int i=0;
    protected int randomServer() {
        int ret=i;
        i=(i+1) % super.size();
        return ret;
    }
}
