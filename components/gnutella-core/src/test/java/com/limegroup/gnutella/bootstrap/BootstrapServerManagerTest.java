package com.limegroup.gnutella.bootstrap;

import junit.framework.*;
import com.sun.java.util.collections.*;
import java.io.*;
import java.net.URLEncoder;
import java.text.ParseException;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.CommonUtils;

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

    ////////////////////////////////////////////////////////////////////////////
    final int RESPONSES_PER_SERVER=12;
    final static int PORT=6700;
    final static String DIRECTORY="/path/to/script.php";
    final static String COMMON_PARAMS="client=LIME&version="
        +URLEncoder.encode(CommonUtils.getLimeWireVersion());

    /** Our backend. */
    TestBootstrapServerManager bman;
    TestHostCatcher catcher;
    /** Our servers. They're stored in the backend in order s1-s3, but we
     *  "randomly" try them s3-s1; see TestBootstrapServerManager.randServer()*/
    BootstrapServer url1, url2, url3;
    TestBootstrapServer s1, s2, s3;

    public void setUp() throws IOException, ParseException {
        url1=new BootstrapServer("http://127.0.0.1:"+(PORT)+DIRECTORY);
        url2=new BootstrapServer("http://127.0.0.1:"+(PORT+1)+DIRECTORY);
        url3=new BootstrapServer("http://127.0.0.1:"+(PORT+2)+DIRECTORY);

        //Prepare backend
        catcher=new TestHostCatcher();
        bman=new TestBootstrapServerManager(catcher);
        bman.addBootstrapServer(url1);
        bman.addBootstrapServer(url2);
        bman.addBootstrapServer(url3);

        //Prepare servers
        s1=new TestBootstrapServer(PORT);
        s2=new TestBootstrapServer(PORT+1);
        s3=new TestBootstrapServer(PORT+2);

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
        assertEquals(null, s3.getRequest());   //wasn't reachable
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS+"&hostfile=1 HTTP/1.1", 
                     s2.getRequest());
        assertEquals(null, s1.getRequest());   //wasn't contacted
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
        Iterator iter=bman.getBootstrapServers();
        assertEquals(url1, iter.next());
        assertEquals(url2, iter.next());
        assertEquals(url3, iter.next());
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
        Iterator iter=bman.getBootstrapServers();
        assertEquals(url1, iter.next());
        assertEquals(url2, iter.next());
        assertTrue(! iter.hasNext());
        //We could also check sX.getRequest() and catcher.list, but that's
        //covered by previous tests.
    }

    public void testRemoveBadHTTPHosts() {
        //Make first server unreachable.
        s3.setResponse("HTTP/1.0 503 Service Unavailable");
        bman.fetchEndpointsAsync();
        sleep();
        Iterator iter=bman.getBootstrapServers();
        assertEquals(url1, iter.next());
        assertEquals(url2, iter.next());
        assertTrue(! iter.hasNext());
    }

    public void testRemoveErrorHosts() {
        //Make first server error.
        s3.setResponseData("ERROR\r\n");

        bman.fetchEndpointsAsync();
        sleep();

        Iterator iter=bman.getBootstrapServers();
        assertEquals(url1, iter.next());
        assertEquals(url2, iter.next());
        assertTrue(! iter.hasNext());
    }

    public void testRemoveBadIPHosts() {
        //Make first server error.
        s3.setResponseData("18.239.0.155:ABC\r\n");

        bman.fetchEndpointsAsync();
        sleep();

        Iterator iter=bman.getBootstrapServers();
        assertEquals(url1, iter.next());
        assertEquals(url2, iter.next());
        assertTrue(! iter.hasNext());
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

        Iterator iter=bman.getBootstrapServers();
        assertEquals(url1, iter.next());
        assertEquals(url2, iter.next());
        assertTrue(! url3.equals(iter.next()));
    }

    public void testSendUpdatesAsyncNoURL() {
        s3.setResponseData("OK\r\n");
        bman.sendUpdatesAsync(new Endpoint("18.239.0.144", 6347));
        sleep();
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS
                           +"&ip=18.239.0.144:6347 HTTP/1.1", 
                     s3.getRequest());
        assertEquals(null, s2.getRequest());
        assertEquals(null, s1.getRequest());
    }

    public void testSendUpdatesAsyncURL() {
        //Force _lastConnectable to be url3
        bman.fetchEndpointsAsync();
        sleep();
        
        s2.setResponseData("Ok\r\n");   //Test funny case; not required by spec
        bman.sendUpdatesAsync(new Endpoint("18.239.0.145", 6348));
        sleep();
        assertEquals("GET "+DIRECTORY+"?"+COMMON_PARAMS
                     +"&ip=18.239.0.145:6348&url="
                     +"http%3A%2F%2F127.0.0.1%3A6702%2Fpath%2Fto%2Fscript.php"
                     +" HTTP/1.1", 
                     s2.getRequest());
        assertEquals(null, s1.getRequest());
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
        assertEquals(null, s1.getRequest());
    }

    public void testGiveUp() {    
        int old=bman.MAX_HOSTS_PER_REQUEST;
        try {
            bman.MAX_HOSTS_PER_REQUEST=2;
            s3.shutdown();
            s2.shutdown();
            bman.fetchEndpointsAsync();
            sleep();
            assertEquals(null, s1.getRequest());
            assertEquals(0, catcher.list.size());
            Iterator iter=bman.getBootstrapServers();
            assertEquals(url1, iter.next());
            assertTrue(! iter.hasNext());
        } finally {
            bman.MAX_HOSTS_PER_REQUEST=old;
        }
    }

    public void testAddBootstrapServer() {
        for (int i=0; i<1000; i++) {
            try {
                BootstrapServer server=new BootstrapServer(
                    "http://18.239.0.144:"+(i+80)+"/script.jsp");
                bman.addBootstrapServer(server);
            } catch (ParseException e) { 
                fail("Bad exception");
            }
        }
        int count=0;
        for (Iterator iter=bman.getBootstrapServers(); 
                 iter.hasNext(); 
                 iter.next()) { 
            count++; 
        }
        assertEquals(200, count);   //e.g., BootstrapServer.MAX_BOOTSTRAP_SERVERS        
        //We could also test the replacement policy, but that's a bit of a pain.
    }

    public void testDefaults() {
        //Clear out the list of urls.
        s3.shutdown();
        s2.shutdown();
        s1.shutdown();
        bman.fetchEndpointsAsync();
        sleep();
        assertTrue(! bman.getBootstrapServers().hasNext());

        //Now second call should load defaults...and actually go on the network!
        bman.fetchEndpointsAsync();
        try { Thread.sleep(3000); } catch (InterruptedException e) { }

        //Make sure defaults were loaded
        int count=0;
        for (Iterator iter=bman.getBootstrapServers(); 
                 iter.hasNext(); 
                 iter.next()) { 
            count++; 
        }        
        assertTrue(count>100);

        //Make sure this actually got some endpoints.  Note: this requires a
        //network connection, as it actually uses GWebCache.
        //assertTrue("Only got "+catcher.list.size()+" endpoints",
        //           catcher.list.size()>10);
    }

    public void testRedirect() {
        s3.setResponse("HTTP/1.1 303 Redirect\r\nLocation: "+url1);
        s3.setResponseData("You have been redirected.");
        bman.fetchEndpointsAsync();
        sleep();
        assertTrue(s3.getRequest()!=null);  //original location
        assertTrue(s1.getRequest()!=null);  //was redirected here
        assertTrue(s2.getRequest()==null);  //didn't go here
        assertTrue(catcher.list.size()==RESPONSES_PER_SERVER);
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

    /** The LAST value given out, or -1 if none.  Starts with s3 and works down
     *  to avoid problems when unreachable ers are removed from list by
     *  BootstrapServerManager. */
    private int i=-1;
    protected int randomServer() {
        if (i<=0)
            i=super.size()-1;
        else 
            //i=Math.min(i-1, super.size()-1);    //i--, but make sure valid index    
            i--;
        return i;
    }
}
