package com.limegroup.gnutella;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Set;
import java.util.StringTokenizer;

import junit.framework.Test;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.IpPortSet;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc.  The test includes a leaf attached to 3 
 * Ultrapeers.
 */
public class ClientSideBrowseHostTest extends ClientSideTestCase {

    private MyActivityCallback callback;

    public ClientSideBrowseHostTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ClientSideBrowseHostTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    ///////////////////////// Actual Tests ////////////////////////////
    
    // Tests the following behaviors:
    // ------------------------------
    // 1. that the client makes a correct direct connection if possible
    // 2. that the client makes a correct push proxy connection if necessary
    // 3. if all else fails the client sends a PushRequest

    public static void globalSetUp() throws Exception {
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),"_acceptedIncoming", Boolean.TRUE);
    }

    public void testHTTPRequest() throws Exception {
        callback = (MyActivityCallback) getCallback();

        drain(testUP[0]);
        // some setup
        final byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "boalt.org");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        ss.setReuseAddress(true);
        ss.setSoTimeout(TIMEOUT);

        // send a reply with some PushProxy info
        IpPort[] proxies = new IpPortImpl[1];
        proxies[0] = new IpPortImpl("127.0.0.1", 7000);
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "boalt.org");
        m = new QueryReply(m.getGUID(), (byte) 1, 7000, 
                           InetAddress.getLocalHost().getAddress(), 0, res, 
                           clientGUID, new byte[0], false, false, true,
                           true, false, false, null,null);
        testUP[0].send(m);
        testUP[0].flush();

        // wait a while for Leaf to process result
        Thread.sleep(1000);
        assertNotNull(callback.getRFD());

        // tell the leaf to browse host the file, should result in direct HTTP
        // request
        RouterService.doAsynchronousBrowseHost(callback.getRFD().getHost(),
                                callback.getRFD().getPort(),
                                new GUID(GUID.makeGuid()), new GUID(clientGUID),
                                null, false);

        // wait for the incoming HTTP request
        Socket httpSock = ss.accept();
        assertNotNull(httpSock);

        // start reading and confirming the HTTP request
        String currLine = null;
        BufferedReader reader = 
            new BufferedReader(new
                               InputStreamReader(httpSock.getInputStream()));

        // confirm a GET/HEAD pushproxy request
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("GET / HTTP/1.1"));
        
        // make sure the node sends the correct Host val
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("Host:"));
        StringTokenizer st = new StringTokenizer(currLine, ":");
        assertEquals(st.nextToken(), "Host");
        InetAddress addr = InetAddress.getByName(st.nextToken().trim());
        Arrays.equals(addr.getAddress(), RouterService.getAddress());
        assertEquals(Integer.parseInt(st.nextToken()), SERVER_PORT);

        // let the other side do its thing
        Thread.sleep(500);

        // send back a 200 and make sure no PushRequest is sent via the normal
        // way
        BufferedWriter writer = 
            new BufferedWriter(new
                               OutputStreamWriter(httpSock.getOutputStream()));

        writer.write("HTTP/1.1 200 OK\r\n");
        writer.flush();
        writer.write("\r\n");
        writer.flush();
        //TODO: should i send some Query Hits?  Might be a good test.
        httpSock.close();

        try {
            do {
                m = testUP[0].receive(TIMEOUT);
                assertTrue(!(m instanceof PushRequest));
            } while (true) ;
        }
        catch (InterruptedIOException expected) {}

        // awesome - everything checks out!
        ss.close();
    }


    public void testPushProxyRequest() throws Exception {
        Thread.sleep(6000);
        callback = (MyActivityCallback) getCallback();
        drain(testUP[0]);
        // some setup
        final byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "nyu.edu");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // set up a server socket to wait for proxy request
        ServerSocket ss = new ServerSocket(7000);
        ss.setReuseAddress(true);
        ss.setSoTimeout(TIMEOUT*4);

        // send a reply with some PushProxy info
        //final PushProxyInterface[] proxies = 
        //  new QueryReply.PushProxyContainer[1];
        final Set proxies = new IpPortSet();
        proxies.add(new IpPortImpl("127.0.0.1", 7000));
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "nyu.edu");
        m = new QueryReply(m.getGUID(), (byte) 1, 6999, 
                           InetAddress.getLocalHost().getAddress(), 0, res, 
                           clientGUID, new byte[0], false, false, true,
                           true, false, false, proxies, null);
        testUP[0].send(m);
        testUP[0].flush();

        // wait a while for Leaf to process result
        Thread.sleep(2000);
        assertTrue(callback.getRFD() != null);

        // tell the leaf to browse host the file, should result in PushProxy
        // request
        RouterService.doAsynchronousBrowseHost(callback.getRFD().getHost(),
                                callback.getRFD().getPort(),
                                new GUID(GUID.makeGuid()), new GUID(clientGUID),
                                proxies, false);

        // wait for the incoming PushProxy request
        // increase the timeout since we send udp pushes first
        ss.setSoTimeout(7000);
        Socket httpSock = ss.accept();
        assertNotNull(httpSock);
        
        BufferedWriter sockWriter  = 
            new BufferedWriter(new
                               OutputStreamWriter(httpSock.getOutputStream()));
        sockWriter.write("HTTP/1.1 202 OK\r\n");
        sockWriter.flush();
        

        // start reading and confirming the HTTP request
        String currLine = null;
        BufferedReader reader = 
            new BufferedReader(new
                               InputStreamReader(httpSock.getInputStream()));

        // confirm a GET/HEAD pushproxy request
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("GET /gnutella/push-proxy") ||
                   currLine.startsWith("HEAD /gnutella/push-proxy"));
        
        // make sure it sends the correct client GUID
        int beginIndex = currLine.indexOf("ID=") + 3;
        String guidString = currLine.substring(beginIndex, beginIndex+26);
        GUID guidFromBackend = new GUID(clientGUID);
        GUID guidFromNetwork = new GUID(Base32.decode(guidString));
        assertEquals(guidFromNetwork, guidFromBackend);

        // make sure the node sends the correct X-Node
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("X-Node:"));
        StringTokenizer st = new StringTokenizer(currLine, ":");
        assertEquals(st.nextToken(), "X-Node");
        InetAddress addr = InetAddress.getByName(st.nextToken().trim());
        Arrays.equals(addr.getAddress(), RouterService.getAddress());
        assertEquals(Integer.parseInt(st.nextToken()), SERVER_PORT);

        // now we need to GIV
        Socket push = new Socket(InetAddress.getLocalHost(), SERVER_PORT);
        BufferedWriter writer = 
            new BufferedWriter(new
                               OutputStreamWriter(push.getOutputStream()));
        writer.write("GIV 0:" + new GUID(clientGUID).toHexString() + "/\r\n");
        writer.write("\r\n");
        writer.flush();

        // confirm a BrowseHost request
        reader = 
            new BufferedReader(new
                               InputStreamReader(push.getInputStream()));
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("GET / HTTP/1.1"));
        
        // make sure the node sends the correct Host val
        currLine = reader.readLine();
        assertTrue(currLine.startsWith("Host:"));
        st = new StringTokenizer(currLine, ":");
        assertEquals(st.nextToken(), "Host");
        addr = InetAddress.getByName(st.nextToken().trim());
        Arrays.equals(addr.getAddress(), RouterService.getAddress());
        assertEquals(Integer.parseInt(st.nextToken()), SERVER_PORT);

        // let the other side do its thing
        Thread.sleep(500);

        // send back a 200 and make sure no PushRequest is sent via the normal
        // way
        writer = 
            new BufferedWriter(new
                               OutputStreamWriter(push.getOutputStream()));

        writer.write("HTTP/1.1 200 OK\r\n");
        writer.flush();
        writer.write("\r\n");
        writer.flush();
        httpSock.close();

        try {
            do {
                m = testUP[0].receive(TIMEOUT);
                assertNotInstanceof(m.toString(), PushRequest.class, m);
            } while (true) ;
        }
        catch (InterruptedIOException expected) {}

        // awesome - everything checks out!
        ss.close();
    }


    public void testSendsPushRequest() throws Exception {
        callback = (MyActivityCallback) getCallback();
        drain(testUP[0]);
        // some setup
        final byte[] clientGUID = GUID.makeGuid();

        // construct and send a query        
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "anita");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest)) ;

        // send a reply with some BAD PushProxy info
        //final PushProxyInterface[] proxies = 
        //  new QueryReply.PushProxyContainer[1];
        final Set proxies = new IpPortSet();
        proxies.add(new IpPortImpl("127.0.0.1", 7001));
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "anita");
        m = new QueryReply(m.getGUID(), (byte) 1, 7000, 
                           InetAddress.getLocalHost().getAddress(), 0, res, 
                           clientGUID, new byte[0], false, false, true,
                           true, false, false, proxies, null);
        testUP[0].send(m);
        testUP[0].flush();

        // wait a while for Leaf to process result
        Thread.sleep(1000);
        assertTrue(callback.getRFD() != null);

        // tell the leaf to browse host the file,
        RouterService.doAsynchronousBrowseHost(callback.getRFD().getHost(),
                                callback.getRFD().getPort(),
                                new GUID(GUID.makeGuid()), new GUID(clientGUID),
                                proxies, false);

        // nothing works for the guy, we should get a PushRequest
        do {
            m = testUP[0].receive(TIMEOUT*30);
        } while (!(m instanceof PushRequest));

        // awesome - everything checks out!
    }




    //////////////////////////////////////////////////////////////////
    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    private static class MyActivityCallback extends ActivityCallbackStub {
        private RemoteFileDesc _rfd = null;
        public RemoteFileDesc getRFD() {
            return _rfd;
        }
        
        public void handleQueryResult(RemoteFileDesc rfd,
                                      HostData data,
                                      Set locs) {
            _rfd = rfd;
        }
    }
}

