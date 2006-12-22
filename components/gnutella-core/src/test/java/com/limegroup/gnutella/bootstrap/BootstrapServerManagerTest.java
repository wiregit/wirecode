package com.limegroup.gnutella.bootstrap;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.EncodingUtils;

/**
 * Unit tests for BootstrapServerManager.
 */
public class BootstrapServerManagerTest extends LimeTestCase {
    public BootstrapServerManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BootstrapServerManagerTest.class);
    }

    ////////////////////////////////////////////////////////////////////////////
    final int RESPONSES_PER_SERVER=12;
    final static int SERVER_PORT=6700;
    final static String DIRECTORY="/path/to/script.php";
    final static String COMMON_PARAMS="client=LIME&version="
        +EncodingUtils.encode(LimeWireUtils.getLimeWireVersion());

    /** Our backend. */
    TestBootstrapServerManager bman;
    TestHostCatcher catcher;
    /**
     * Our servers. They're stored in the backend in order s3-s1.
     * For urlfile retrievals, we try them in order s3-s2-s1 (done by
     * BootstrapServerManager), but for all others we force it to
     *  "randomly" try them s3-s1; see TestBootstrapServerManager.randServer()*/
    BootstrapServer url1, url2, url3;
    TestBootstrapServer s1, s2, s3;

    public void setUp() throws Exception {
        url1=new BootstrapServer("http://127.0.0.1:"+(SERVER_PORT)+DIRECTORY);
        url2=new BootstrapServer("http://127.0.0.1:"+(SERVER_PORT+1)+DIRECTORY);
        url3=new BootstrapServer("http://127.0.0.1:"+(SERVER_PORT+2)+DIRECTORY);

        //Prepare backend
        catcher = new TestHostCatcher();
        PrivilegedAccessor.setValue(RouterService.class, "catcher", catcher);
        bman = new TestBootstrapServerManager();
        bman.addBootstrapServer(url3);
        bman.addBootstrapServer(url2);
        bman.addBootstrapServer(url1);
        //bman.bootstrapServersAdded(); NOT CALLED BECAUSE IT SHUFFLES

        //Prepare servers
        s1=new TestBootstrapServer(SERVER_PORT);
        s3=new TestBootstrapServer(SERVER_PORT+2);

        s2=new TestBootstrapServer(SERVER_PORT+1);
        StringBuffer response=new StringBuffer();
        for (int i=0; i<RESPONSES_PER_SERVER; i++)
            response.append("1.2.3."+i+":6346"+(i<5 ? "\r\n" : "\n"));
        s1.setResponseData(response.toString());
        s2.setResponseData(response.toString());
        s3.setResponseData(response.toString());
    }

    public void tearDown() {
        s1.shutdown();
        s2.shutdown();
        s3.shutdown();
        try { Thread.sleep(200); } catch (InterruptedException e) { }
    }

    
    ///////////////////////////////////////////////////////////////////////
    
    /** Checks hostfile=1 request.  Also checks that unreachable hosts are
     *  visited as needed.  */
    public void testFetchEndpointsAsync() {
        //Make first server unreachable.  (Remember try s3, s2, then s1.)
        s3.shutdown();

        //Connect.  Wait for data.
        bman.fetchEndpointsAsync();
        sleep();

        //Check that backend sent right requests.  Only the second host should
        //have been contacted.
        assertNull(s3.getRequest());   //wasn't reachable
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS+"&hostfile=1 HTTP/1.1", 
                     s2.getRequest());
        assertNull( s1.getRequest());   //wasn't contacted
        //...and got right results.
        for (int i=0; i<RESPONSES_PER_SERVER; i++) 
            assertEquals(new Endpoint("1.2.3."+i+":6346"),
                         catcher.list.get(i));
    }

    /** Checks urlfile=1 request.  Also checks that multiple hosts are visited
     *  as needed. */
    public void testFetchBootstrapServersAsync() throws ParseException {
        final int SIZE=12;
        //Prepare server.
        StringBuffer response=new StringBuffer();
        for (int i=0; i<SIZE/2; i++)
            response.append("http://1.2.3."+i+"/script.php\r\n");
        s3.setResponseData(response.toString());
        response=new StringBuffer();
        for (int i=SIZE/2; i<SIZE; i++)
            response.append("http://1.2.3."+i+"/script.php\r\n");
        s2.setResponseData(response.toString());        

        //Connect.  Wait for data.
        bman.fetchBootstrapServersAsync();
        sleep();

        //Check that backend sent right request.  We had to contact the second
        //host because the first didn't send enough data.
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS+"&urlfile=1 HTTP/1.1", 
                     s3.getRequest());
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS+"&urlfile=1 HTTP/1.1", 
                     s2.getRequest());
        assertEquals(null, s1.getRequest());   //wasn't contacted
        //Check that we got the right results.  First make sure we kept the old
        //server list...
        Iterator iter=bman.getBootstrapServers().iterator();
        assertEquals(url3, iter.next());
        assertEquals(url2, iter.next());
        assertEquals(url1, iter.next());
        //...and added new servers.
        for (int i=0; i<RESPONSES_PER_SERVER; i++) {
            assertTrue(iter.hasNext());
            assertEquals(iter.next(),
                         new BootstrapServer("http://1.2.3."+i+"/script.php"));
        }
        assertTrue(! iter.hasNext());
    }

    public void testRemoveUnreachableHosts() {
        //Make first server unreachable.
        s3.shutdown();
        bman.fetchEndpointsAsync();
        sleep();
        Iterator iter=bman.getBootstrapServers().iterator();
        assertEquals(url2, iter.next());
        assertEquals(url1, iter.next());
        if(iter.hasNext())
            fail("had another: " + iter.next());
        //We could also check sX.getRequest() and catcher.list, but that's
        //covered by previous tests.
    }

    public void testRemoveBadHTTPHosts() {
        //Make first server unreachable.
        s3.setResponse("HTTP/1.0 503 Service Unavailable");
        bman.fetchEndpointsAsync();
        sleep();
        Iterator iter=bman.getBootstrapServers().iterator();
        assertEquals(url2, iter.next());
        assertEquals(url1, iter.next());
        if(iter.hasNext())
            fail("had another: " + iter.next());
    }

    public void testRemoveErrorHosts() {
        //Make first server error.
        s3.setResponseData("ERROR\r\n");

        bman.fetchEndpointsAsync();
        sleep();

        Iterator iter=bman.getBootstrapServers().iterator();
        assertEquals(url2, iter.next());
        assertEquals(url1, iter.next());
        if(iter.hasNext())
            fail("had another: " + iter.next());
    }

    public void testRemoveBadIPHosts() {
        //Make first server error.
        s3.setResponseData("18.239.0.155:ABC\r\n");

        bman.fetchEndpointsAsync();
        sleep();

        Iterator iter=bman.getBootstrapServers().iterator();
        assertEquals(url2, iter.next());
        assertEquals(url1, iter.next());
        if(iter.hasNext())
            fail("had another: " + iter.next());
    }

    /** Test servers that send HTML for HOSTFILE requestss. */
    public void testRemoveBadIPHosts2() {
        //Make first server error.
        s3.setResponseData("<html>This\r\nis bad\r\ndata>\r\n");

        bman.fetchEndpointsAsync();
        sleep();

        Iterator iter=bman.getBootstrapServers().iterator();
        assertEquals(url2, iter.next());
        assertEquals(url1, iter.next());
        if(iter.hasNext())
            fail("had another: " + iter.next());

        assertEquals(RESPONSES_PER_SERVER, catcher.list.size());
        for (int i=0; i<RESPONSES_PER_SERVER; i++) 
            assertEquals(new Endpoint("1.2.3."+i+":6346"),
                         catcher.list.get(i));

    }

    public void testRemoveBadURLHosts() {
        //Make first server unreachable.
        s3.setResponseData("improper.url\r\n");
        //Make second server have data.
        StringBuffer response=new StringBuffer();
        for (int i=0; i<RESPONSES_PER_SERVER; i++)
            response.append("http://1.2.3."+i+"/script.php\r\n");
        s2.setResponseData(response.toString());

        bman.fetchBootstrapServersAsync();
        sleep();

        Iterator iter=bman.getBootstrapServers().iterator();
        assertEquals(url2, iter.next());
        assertEquals(url1, iter.next());
        assertTrue(! url3.equals(iter.next()));
    }

    public void testSendUpdatesAsyncNoURL() {
        s3.setResponseData("OK\r\n");
        bman.sendUpdatesAsync(new Endpoint("18.239.0.144", 6347));
        sleep();
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS
                           +"&ip=18.239.0.144:6347 HTTP/1.1", 
                     s3.getRequest());
        assertNull(s2.getRequest());
        assertNull(s1.getRequest());
    }

    public void testSendUpdatesAsyncURL() {
        //Force _lastConnectable to be url3
        bman.fetchEndpointsAsync();
        sleep();
        
        bman.randomServer(); // pretend we contacted s3 so we can contact s2
        
        s2.setResponseData("Ok\r\n");   //Test funny case; not required by spec
        bman.sendUpdatesAsync(new Endpoint("18.239.0.145", 6348));
        sleep();
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS
                     +"&ip=18.239.0.145:6348&url="
                     +"http%3A%2F%2F127.0.0.1%3A6702%2Fpath%2Fto%2Fscript.php"
                     +" HTTP/1.1", 
                     s2.getRequest());
        assertNull(s1.getRequest());
    }

    public void testSendUpdatesAsyncNoOK() {
        s3.setResponseData("");
        s2.setResponseData("OK\r\n");
        bman.sendUpdatesAsync(new Endpoint("18.239.0.144", 6347));
        sleep();
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS
                     +"&ip=18.239.0.144:6347 HTTP/1.1", 
                     s3.getRequest());
        //Yes, url3 is sent to s2 even though url3 never sent OK.  TODO: fix
        //this.
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS
                     +"&ip=18.239.0.144:6347&url="
                     +"http%3A%2F%2F127.0.0.1%3A6702%2Fpath%2Fto%2Fscript.php"
                     +" HTTP/1.1", 
                     s2.getRequest());
        assertNull(s1.getRequest());
    }

    public void testGiveUp() {    
        int old = BootstrapServerManager.MAX_HOSTS_PER_REQUEST;
        try {
            BootstrapServerManager.MAX_HOSTS_PER_REQUEST = 2;
            s3.shutdown();
            s2.shutdown();
            bman.fetchEndpointsAsync();
            sleep();
            assertEquals(null, s1.getRequest());
            assertEquals(0, catcher.list.size());
            Iterator iter=bman.getBootstrapServers().iterator();
            assertEquals(url1, iter.next());
            if(iter.hasNext())
                fail("had another: " + iter.next());
        } finally {
            BootstrapServerManager.MAX_HOSTS_PER_REQUEST = old;
        }
    }

    public void testAddBootstrapServer() {
        for (int i=0; i<5000; i++) {
            try {
                BootstrapServer server=new BootstrapServer(
                    "http://18.239.0.144:"+(i+80)+"/script.jsp");
                bman.addBootstrapServer(server);
            } catch (ParseException e) { 
                fail("Bad exception");
            }
        }
        int count=0;
        for (Iterator iter=bman.getBootstrapServers().iterator(); 
                 iter.hasNext(); 
                 iter.next()) { 
            count++; 
        }
        assertEquals(1000, count);   //e.g., BootstrapServer.MAX_BOOTSTRAP_SERVERS        
        //We could also test the replacement policy, but that's a bit of a pain.
    }

    public void testDefaults() {
        //Clear out the list of urls.
        s3.shutdown();
        s2.shutdown();
        s1.shutdown();
        bman.fetchEndpointsAsync();
        sleep();
        assertTrue(! bman.getBootstrapServers().iterator().hasNext());

        //Now second call should load defaults...and actually go on the network!
        bman.fetchEndpointsAsync();
        try { Thread.sleep(3000); } catch (InterruptedException e) { }

        //Make sure defaults were loaded
        int count=0;
        for (Iterator iter=bman.getBootstrapServers().iterator(); 
                 iter.hasNext(); 
                 iter.next()) { 
            count++; 
        }
        // start with our defaults, add 3 for s3->s1, subtract failed hosts.
        assertEquals(DefaultBootstrapServers.urls.length + 3 -
                     bman.failed, count);

        //Make sure this actually got some endpoints.  Note: this requires a
        //network connection, as it actually uses GWebCache.
        //assertTrue("Only got "+catcher.list.size()+" endpoints",
        //           catcher.list.size()>10);
    }

    public void testRedirect() {
        // we should not follow redirects!!!
        s3.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url1);
        s3.setResponseData("You have been redirected.");
        bman.fetchEndpointsAsync();
        sleep();
        assertNotNull(s3.getRequest());  //original location
        assertNull(s1.getRequest());  //was redirected here, but shouldn't go
        assertNotNull(s2.getRequest());  //should go here since s3 was crap
        assertEquals("invalid responses, got: " + catcher.list,
                RESPONSES_PER_SERVER, catcher.list.size());
    }

    private void sleep() {
        //Must be long enough for socket connect timeouts.  On Linux
        //it appears that a much shorter value will work.
        try { Thread.sleep(5000); } catch (InterruptedException e) { }
    }
}

/** Overrides the add(Endpoint, boolean) method) */
class TestHostCatcher extends HostCatcher {
    List /* of Endpoint */ list=new ArrayList(20);

    public TestHostCatcher() {
        super();
    }

    public boolean add(Endpoint e, boolean forceHighPriority) {
        list.add(e);
        return true;
    }

    public boolean add(Endpoint e, int priority) {
        list.add(e);
        return true;
    }
}

/** A BootstrapServerManager that tries host in a round-robin fashion. */
class TestBootstrapServerManager extends BootstrapServerManager {
    
    public int failed = 0;

    public TestBootstrapServerManager() {
        super();
    }

    /** The LAST value given out, or -1 if none.  Starts with s3 and works down
     *  to avoid problems when unreachable ers are removed from list by
     *  BootstrapServerManager. */
    private int i = -1;
    public int randomServer() {
        if (i >= size() || i < 0)
            return (i = 0);
        else 
            return ++i;
    }
    
    protected void removeServer(BootstrapServer server) {
        super.removeServer(server);
        i--;
        failed++;
    }
}
