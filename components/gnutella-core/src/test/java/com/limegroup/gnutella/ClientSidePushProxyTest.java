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
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.service.ErrorService;
import org.limewire.util.Base32;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.messages.vendor.PushProxyAcknowledgement;
import com.limegroup.gnutella.messages.vendor.PushProxyRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Checks whether (multi)leaves avoid forwarding messages to ultrapeers, do
 * redirects properly, etc. The test includes a leaf attached to 3 Ultrapeers.
 */
public class ClientSidePushProxyTest extends ClientSideTestCase {
    
    protected static final int PORT = 6669;

    protected static int TIMEOUT = 1000; // should override super

    public ClientSidePushProxyTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ClientSidePushProxyTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ((MyActivityCallback) getCallback()).cleanup();

        // send a MessagesSupportedMessage
        testUP[0].send(MessagesSupportedVendorMessage.instance());
        testUP[0].flush();

        // we expect to get a PushProxy request
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof PushProxyRequest));

        // we should answer the push proxy request
        PushProxyAcknowledgement ack = new PushProxyAcknowledgement(InetAddress
                .getLocalHost(), 6355, new GUID(m.getGUID()));
        testUP[0].send(ack);
        testUP[0].flush();
    }

    public void testQueryReplyHasProxiesAndCanGIV() throws Exception {
        setAccepted(false);
        drain(testUP[0]);

        // make sure leaf is sharing
        assertEquals(2, RouterService.getFileManager().getNumFiles());

        // send a query that should be answered
        QueryRequest query = new QueryRequest(GUID.makeGuid(), (byte) 1,
                "berkeley", null, null, null, null, false, 0, false, 0);
        testUP[0].send(query);
        testUP[0].flush();

        // await a response
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryReply));

        // confirm it has proxy info
        QueryReply reply = (QueryReply) m;
        assertNotNull(reply.getPushProxies());

        // check out PushProxy info
        Set proxies = reply.getPushProxies();
        assertEquals(1, proxies.size());
        Iterator iter = proxies.iterator();
        IpPort ppi = (IpPort) iter.next();
        assertEquals(ppi.getPort(), 6355);
        assertTrue(ppi.getInetAddress().equals(testUP[0].getInetAddress()));

        // set up a ServerSocket to get give on
        ServerSocket ss = new ServerSocket(9000);
        try {
            ss.setReuseAddress(true);
            ss.setSoTimeout(TIMEOUT);

            // test that the client responds to a PushRequest
            PushRequest pr = new PushRequest(GUID.makeGuid(), (byte) 1,
                    RouterService.getMessageRouter()._clientGUID, 0,
                    InetAddress.getLocalHost().getAddress(), 9000);

            // send the PR off
            testUP[0].send(pr);
            testUP[0].flush();

            // we should get a incoming GIV
            Socket givSock = ss.accept();
            try {
                assertNotNull(givSock);

                // start reading and confirming the HTTP request
                String currLine = null;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(givSock.getInputStream()));

                // confirm a GIV
                currLine = reader.readLine();
                GUID guid = new GUID(
                        RouterService.getMessageRouter()._clientGUID);
                String givLine = "GIV 0:" + guid.toHexString();
                assertTrue(currLine.startsWith(givLine));
            } finally {
                givSock.close();
            }
        } finally {
            ss.close();
        }
    }

    public void testHTTPRequest() throws Exception {
        setAccepted(true);
        drain(testUP[0]);
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "boalt.org");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest));

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        try {
            ss.setReuseAddress(true);
            ss.setSoTimeout(25 * TIMEOUT);

            // send a reply with some PushProxy info
            Set<IpPort> proxies = new TreeSet<IpPort>(IpPort.COMPARATOR);
            proxies.add(new IpPortImpl("127.0.0.1", 7000));
            Response[] res = new Response[1];
            res[0] = new Response(10, 10, "boalt.org");
            m = new QueryReply(m.getGUID(), (byte) 1, 6355, myIP(), 0, res,
                    clientGUID, new byte[0], true, false, true, true, false,
                    false, proxies);
            testUP[0].send(m);
            testUP[0].flush();

            // wait a while for Leaf to process result
            assertNotNull(((MyActivityCallback) getCallback()).getRFD());

            // tell the leaf to download the file, should result in push proxy
            // request
            RouterService
                    .download(
                            (new RemoteFileDesc[] { ((MyActivityCallback) getCallback())
                                    .getRFD() }), true, new GUID(m.getGUID()));

            // wait for the incoming HTTP request
            Socket httpSock = ss.accept();
            try {
                assertNotNull(httpSock);

                // start reading and confirming the HTTP request
                String currLine = null;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpSock.getInputStream()));

                // confirm a GET/HEAD pushproxy request
                currLine = reader.readLine();
                assertTrue(currLine.startsWith("GET /gnutella/push-proxy")
                        || currLine.startsWith("HEAD /gnutella/push-proxy"));

                // make sure it sends the correct client GUID
                int beginIndex = currLine.indexOf("ID=") + 3;
                String guidString = currLine.substring(beginIndex,
                        beginIndex + 26);
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
                assertEquals(Integer.parseInt(st.nextToken()), PORT);

                // send back a 202 and make sure no PushRequest is sent via the
                // normal way
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(httpSock.getOutputStream()));

                writer.write("HTTP/1.1 202 gobbledygook");
                writer.flush();
            } finally {
                httpSock.close();
            }

            try {
                do {
                    m = testUP[0].receive(TIMEOUT);
                    assertTrue(!(m instanceof PushRequest));
                } while (true);
            } catch (InterruptedIOException expected) {
            }

            // now make a connection to the leaf to confirm that it will
            // send a correct download request
            Socket push = new Socket(InetAddress.getLocalHost(), PORT);

            try {
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(push.getOutputStream()));
                writer.write("GIV ");
                writer.flush();
                LimeTestUtils.waitForNIO();

                // the PUSH request is not matched in PushList.getBestHost() if
                // this is set to false: the RemoteFileDesc contains the IP
                // 192.168.0.1 but since we are connecting from a different IP
                // it is not matched but it'll accept it this is set to true and
                // both IPs are private
                ConnectionSettings.LOCAL_IS_PRIVATE.setValue(true);

                writer.write("0:" + new GUID(clientGUID).toHexString()
                        + "/\r\n");

                writer.write("\r\n");
                writer.flush();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(push.getInputStream()));
                String currLine = reader.readLine();
                assertEquals("GET /get/10/boalt.org HTTP/1.1", currLine);
            } finally {
                push.close();
            }
        } finally {
            ss.close();
        }
    }

    public void testNoProxiesSendsPushNormal() throws Exception {
        drain(testUP[0]);
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "golf is awesome");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest));

        // send a reply with NO PushProxy info
        Response[] res = new Response[1];
        res[0] = new Response(10, 10, "golf is awesome");
        m = new QueryReply(m.getGUID(), (byte) 1, 6355, myIP(), 0, res,
                clientGUID, new byte[0], false, false, true, true, false,
                false, null);
        testUP[0].send(m);
        testUP[0].flush();

        // wait a while for Leaf to process result
        assertNotNull(((MyActivityCallback) getCallback()).getRFD());

        // tell the leaf to download the file, should result in normal TCP
        // PushRequest
        RouterService.download(
                (new RemoteFileDesc[] { ((MyActivityCallback) getCallback())
                        .getRFD() }), true, new GUID(m.getGUID()));

        // await a PushRequest
        do {
            m = testUP[0].receive(25 * TIMEOUT);
        } while (!(m instanceof PushRequest));
    }

    public void testCanReactToBadPushProxy() throws Exception {
        drain(testUP[0]);
        // some setup
        byte[] clientGUID = GUID.makeGuid();

        // construct and send a query
        byte[] guid = GUID.makeGuid();
        RouterService.query(guid, "berkeley.edu");

        // the testUP[0] should get it
        Message m = null;
        do {
            m = testUP[0].receive(TIMEOUT);
        } while (!(m instanceof QueryRequest));

        // set up a server socket
        ServerSocket ss = new ServerSocket(7000);
        try {
            ss.setReuseAddress(true);
            ss.setSoTimeout(25 * TIMEOUT);

            // send a reply with some BAD PushProxy info
            // PushProxyInterface[] proxies = new
            // QueryReply.PushProxyContainer[2];
            Set<IpPort> proxies = new TreeSet<IpPort>(IpPort.COMPARATOR);
            proxies.add(new IpPortImpl("127.0.0.1", 7000));
            proxies.add(new IpPortImpl("127.0.0.1", 8000));
            Response[] res = new Response[1];
            res[0] = new Response(10, 10, "berkeley.edu");
            m = new QueryReply(m.getGUID(), (byte) 1, 6355, myIP(), 0, res,
                    clientGUID, new byte[0], true, false, true, true, false,
                    false, proxies);
            testUP[0].send(m);
            testUP[0].flush();

            // wait a while for Leaf to process result
            assertNotNull(((MyActivityCallback) getCallback()).getRFD());

            // tell the leaf to download the file, should result in push proxy
            // request
            RouterService
                    .download(
                            (new RemoteFileDesc[] { ((MyActivityCallback) getCallback())
                                    .getRFD() }), true, new GUID((m.getGUID())));

            // wait for the incoming HTTP request
            Socket httpSock = ss.accept();
            try {
                // send back a error and make sure the PushRequest is sent via
                // the normal way
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(httpSock.getOutputStream()));

                writer.write("HTTP/1.1 410 gobbledygook");
                writer.flush();
            } finally {
                httpSock.close();
            }

            // await a PushRequest
            do {
                m = testUP[0].receive(TIMEOUT * 8);
            } while (!(m instanceof PushRequest));
        } finally {
            ss.close();
        }
    }

    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    private static byte[] myIP() {
        return new byte[] { (byte) 192, (byte) 168, 0, 1 };
    }

    public static class MyActivityCallback extends ActivityCallbackStub {

        private Lock rfdLock = new ReentrantLock();

        private Condition rfdCondition = rfdLock.newCondition();

        private RemoteFileDesc rfd = null;

        public RemoteFileDesc getRFD() throws InterruptedException {
            rfdLock.lock();
            try {
                if (rfd == null) {
                    rfdCondition.await(TIMEOUT, TimeUnit.MILLISECONDS);
                }
            } finally {
                rfdLock.unlock();
            }
            return rfd;
        }

        public void handleQueryResult(RemoteFileDesc rfd, HostData data,
                Set locs) {
            rfdLock.lock();
            try {
                this.rfd = rfd;
                rfdCondition.signal();
            } finally {
                rfdLock.unlock();
            }
        }

        public void cleanup() {
            rfd = null;
        }
    }

    private static void setAccepted(boolean accepted) {
        try {
            PrivilegedAccessor.setValue(RouterService.getAcceptor(),
                    "_acceptedIncoming", accepted);
        } catch (Exception bad) {
            ErrorService.error(bad);
        }
    }

}
