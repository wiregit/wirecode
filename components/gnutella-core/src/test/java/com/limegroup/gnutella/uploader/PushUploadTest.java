package com.limegroup.gnutella.uploader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.util.CommonUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HTTPAcceptor;
import com.limegroup.gnutella.HTTPUploadManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.UltrapeerHeaders;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.xml.MetaFileManager;

public class PushUploadTest extends LimeTestCase {

    private static final int PORT = 6668;

    /** Our listening port for pushes. */
    private static final int PUSH_PORT = 6671;

    private static String testDirName = "com/limegroup/gnutella/uploader/data";

    private static String fileName = "alphabet test file#2.txt";

    private static String url = "/get/0/alphabet%20test+file%232.txt";

    private static byte[] guid;

    private static Socket socket;

    /** The file contents. */
    private static final String alphabet = "abcdefghijklmnopqrstuvwxyz";

    private MetaFileManager fm;

    private HTTPAcceptor httpAcceptor;

    private HTTPUploadManager upMan;

    private BufferedReader in;

    private BufferedWriter out;

    public PushUploadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushUploadTest.class);
    }

    public static void globalSetUp() throws Exception {
        RouterService rs = new RouterService(new ActivityCallbackStub());

        doSettings();

        rs.start();
        Thread.sleep(2000);

// // TODO acceptor shutdown in globalTearDown()
// Acceptor acceptor = RouterService.getAcceptor();
// acceptor.init();
// acceptor.start();
    }

    private static void doSettings() throws Exception {
        SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES
                .setValue(new String[] { "*.*.*.*" });
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] {
                "127.*.*.*", InetAddress.getLocalHost().getHostAddress() });
        RouterService.getIpFilter().refreshHosts();
        ConnectionSettings.PORT.setValue(PORT);

        SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt");
        UploadSettings.HARD_MAX_UPLOADS.setValue(10);
        UploadSettings.UPLOADS_PER_PERSON.setValue(10);
        UploadSettings.MAX_PUSHES_PER_HOST.setValue(9999);

        FilterSettings.FILTER_DUPLICATES.setValue(false);

        ConnectionSettings.NUM_CONNECTIONS.setValue(8);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.ALLOW_WHILE_DISCONNECTED.setValue(true);
        UltrapeerSettings.FORCE_ULTRAPEER_MODE.setValue(true);
        UltrapeerSettings.EVER_ULTRAPEER_CAPABLE.setValue(true);
    }

    @Override
    protected void setUp() throws Exception {
        doSettings();

        // copy resources
        File testDir = CommonUtils.getResourceFile(testDirName);
        assertTrue("test directory could not be found", testDir.isDirectory());
        File testFile = new File(testDir, fileName);
        assertTrue("test file should exist", testFile.exists());
        File sharedFile = new File(_sharedDir, fileName);
        // we must use a separate copy method
        // because the filename has a # in it which can't be a resource.
        LimeTestUtils.copyFile(testFile, sharedFile);
        assertTrue("should exist", new File(_sharedDir, fileName).exists());
        assertGreaterThan("should have data", 0, new File(_sharedDir, fileName)
                .length());

        // start services
        fm = new MetaFileManager();
        fm.startAndWait(4000);
        PrivilegedAccessor.setValue(RouterService.class, "fileManager", fm);

        httpAcceptor = new HTTPAcceptor();
        PrivilegedAccessor.setValue(RouterService.class, "httpUploadAcceptor",
                httpAcceptor);

        upMan = new HTTPUploadManager(new UploadSlotManager());
        upMan.setFileManager(fm);
        PrivilegedAccessor
                .setValue(RouterService.class, "uploadManager", upMan);

        httpAcceptor.start(RouterService.getConnectionDispatcher());
        upMan.start(httpAcceptor);
    }

    @Override
    public void tearDown() throws IOException {
        closeConnection();

        upMan.stop(httpAcceptor);
        httpAcceptor.stop(RouterService.getConnectionDispatcher());
    }

    public void testDownloadHTTP10() throws Exception {
        establishPushConnection();

        HttpRequest request = new BasicHttpRequest("GET", url,
                HttpVersion.HTTP_1_0);
        HttpResponse response = sendRequest(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String body = readBody(response);
        assertEquals(alphabet, body);
    }

    public void testDownloadHTTP10PushRange() throws Exception {
        establishPushConnection();

        HttpRequest request = new BasicHttpRequest("GET", url,
                HttpVersion.HTTP_1_0);
        request.addHeader("Range", "bytes=2-5");
        HttpResponse response = sendRequest(request);
        assertEquals(206, response.getStatusLine().getStatusCode());
        String body = readBody(response);
        assertEquals("cdef", body);
    }

    public void testDownloadHTTP11() throws Exception {
        establishPushConnection();

        HttpRequest request = new BasicHttpRequest("GET", url,
                HttpVersion.HTTP_1_1);
        HttpResponse response = sendRequest(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String body = readBody(response);
        assertEquals(alphabet, body);
        assertFalse(socket.isClosed());

        response = sendRequest(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        body = readBody(response);
        assertEquals(alphabet, body);
    }

    public void testDownloadHeadHTTP11() throws Exception {
        establishPushConnection();

        HttpRequest request = new BasicHttpRequest("HEAD", url,
                HttpVersion.HTTP_1_1);
        HttpResponse response = sendRequest(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertFalse(socket.isClosed());

        request.addHeader("Connection", "close");
        response = sendRequest(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals(-1, in.read());
    }

    public void testHTTP11PushRange() throws Exception {
        establishPushConnection();
        HttpRequest request = new BasicHttpRequest("GET", url,
                HttpVersion.HTTP_1_1);
        request.addHeader("Range", "bytes=2-5");
        HttpResponse response = sendRequest(request);
        assertEquals(206, response.getStatusLine().getStatusCode());
        String body = readBody(response);
        assertEquals("cdef", body);

        response = sendRequest(request);
        assertEquals(206, response.getStatusLine().getStatusCode());
        body = readBody(response);
        assertEquals("cdef", body);
    }

    public void testHTTP11PipeliningDownloadPush() throws Exception {
        establishPushConnection();

        HttpRequest request = new BasicHttpRequest("GET", url,
                HttpVersion.HTTP_1_1);
        writeRequest(out, request);
        writeRequest(out, request);

        HttpResponse response = readResponse(in);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String body = readBody(response);
        assertEquals(alphabet, body);
        assertFalse(socket.isClosed());

        response = readResponse(in);
        assertEquals(200, response.getStatusLine().getStatusCode());
        body = readBody(response);
        assertEquals(alphabet, body);
        assertFalse(socket.isClosed());

        writeRequest(out, request);

        response = readResponse(in);
        assertEquals(200, response.getStatusLine().getStatusCode());
        body = readBody(response);
        assertEquals(alphabet, body);
        assertFalse(socket.isClosed());
    }

    /**
     * Tests that the node sends a proper proxies header.
     */
    public void testForPushProxyHeaderWithoutProxy() throws Exception {
        // try when we are not firewalled
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
                "_acceptedIncoming", new Boolean(true));
        assertTrue(RouterService.acceptedIncomingConnection());

        establishConnection();
        HttpRequest request = new BasicHttpRequest("GET", url,
                HttpVersion.HTTP_1_1);
        HttpResponse response = sendRequest(request);
        UploadTestUtils.assertNotHasHeader(response, "X-Push-Proxy: 1.2.3.4:5");

        // now try with an empty set of proxies
        establishPushConnection();
        
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
                "_acceptedIncoming", new Boolean(false));
        assertFalse(RouterService.acceptedIncomingConnection());

        response = sendRequest(request);
        UploadTestUtils.assertNotHasHeader(response, "X-Push-Proxy: 1.2.3.4:5");
    }

    public void testForPushProxyHeaderWithProxy() throws Exception {
        establishPushConnection();

        // now try with some proxies
        ConnectionManager original = RouterService.getConnectionManager();
        try {
            final Set<IpPort> proxies = new TreeSet<IpPort>(IpPort.COMPARATOR);
            IpPort ppi = new IpPortImpl("1.2.3.4", 5);
            proxies.add(ppi);

            ConnectionManagerStub cmStub = new ConnectionManagerStub() {
                @Override
                public java.util.Set<IpPort> getPushProxies() {
                    return proxies;
                }
            };
            PrivilegedAccessor.setValue(RouterService.class, "manager", cmStub);

            PrivilegedAccessor.setValue(RouterService.getAcceptor(),
                    "_acceptedIncoming", new Boolean(false));
            assertFalse(RouterService.acceptedIncomingConnection());

            HttpRequest request = new BasicHttpRequest("GET", url,
                    HttpVersion.HTTP_1_1);
            HttpResponse response = sendRequest(request);
            UploadTestUtils.assertHasHeader(response, "X-Push-Proxy: 1.2.3.4:5");
        } finally {
            PrivilegedAccessor.setValue(RouterService.class, "manager", original);
        }
    }

    /**
     * Tests the scenario where we receive the same push request message more
     * than once.
     */
    public void testDuplicatePushes() throws Exception {
        Connection connection = new Connection("localhost", PORT);
        try {
            connection.initialize(new UltrapeerHeaders(null),
                    new EmptyResponder(), 1000);
            QueryRequest query = QueryRequest.createQuery("txt", (byte) 3);
            connection.send(query);
            connection.flush();
            QueryReply reply = null;
            for (int i = 0; i < 10; i++) {
                Message m = connection.receive(2000);
                if (m instanceof QueryReply) {
                    reply = (QueryReply) m;
                    break;
                }
            }

            if (reply == null)
                throw new IOException("didn't get query reply in time");

            PushRequest push = new PushRequest(GUID.makeGuid(), (byte) 3, reply
                    .getClientGUID(), 0, new byte[] { (byte) 127, (byte) 0,
                    (byte) 0, (byte) 1 }, PUSH_PORT);

            // Create listening socket, then send the push a few times
            ServerSocket serverSocket = new ServerSocket(PUSH_PORT);
            try {
                serverSocket.setSoTimeout(1000);

                connection.send(push);
                connection.send(push);
                connection.send(push);
                connection.flush();

                assertNotNull(serverSocket.accept()); // get one.

                // the last two shouldn't be gotten.
                try {
                    serverSocket.accept();
                    fail("Node replied to duplicate push request");
                } catch (IOException expected) {
                }

                try {
                    serverSocket.accept();
                    fail("Node replied to duplicate push request");
                } catch (IOException expected) {
                }
            } finally {
                serverSocket.close();
            }
        } finally {
            connection.close();
        }
    }

    /**
     * Does a push and gets a socket from the incoming connection.
     */
    private static Socket getSocketFromPush() throws IOException,
            BadPacketException {
        Connection connection = new Connection("localhost", PORT);
        try {
            connection.initialize(new UltrapeerHeaders(null),
                    new EmptyResponder(), 1000);

            // send query
            QueryRequest query = QueryRequest.createQuery("txt", (byte) 3);
            connection.send(query);
            connection.flush();

            // look for reply
            QueryReply reply = null;
            for (int i = 0; i < 10; i++) {
                Message m = connection.receive(2000);
                if (m instanceof QueryReply) {
                    reply = (QueryReply) m;
                    break;
                }
            }
            if (reply == null) {
                throw new IOException("Did not get query reply in time");
            }

            // create listening socket and wait for push connect
            ServerSocket serverSocket = new ServerSocket(PUSH_PORT);

            // send push
            guid = reply.getClientGUID();
            PushRequest push = new PushRequest(GUID.makeGuid(), (byte) 3, guid,
                    0, new byte[] { (byte) 127, (byte) 0, (byte) 0, (byte) 1 },
                    PUSH_PORT);
            connection.send(push);
            connection.flush();

            try {
                serverSocket.setSoTimeout(1000);
                return serverSocket.accept();
            } finally {
                serverSocket.close();
            }
        } finally {
            connection.close();
        }
    }

    /**
     * Does a push download.
     */
    private void establishPushConnection() throws BadPacketException,
            IOException {
        closeConnection();
        
        socket = getSocketFromPush();

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket
                .getOutputStream()));

        assertEquals("Expected 'GIV', got null", "GIV 0:"
                + new GUID(guid).toString() + "/file", in.readLine());
        assertEquals("Expected blank line, got null", "", in.readLine());
    }

    private void closeConnection() throws IOException {
        if (socket != null) {
            // close connection
            socket.close();
            socket = null;
        }
    }

    private void establishConnection() throws IOException {
        closeConnection();
        
        socket = new Socket("localhost", PORT);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket
                .getOutputStream()));
    }

    private HttpResponse sendRequest(HttpRequest request) throws Exception {
        writeRequest(out, request);
        return readResponse(in);
    }

    private void writeRequest(BufferedWriter out, HttpRequest request)
            throws IOException {
        out.write(request.getRequestLine().toString());
        out.write("\r\n");
        for (org.apache.http.Header header : request.getAllHeaders()) {
            out.write(header.toString());
            out.write("\r\n");
        }
        out.write("\r\n");
        out.flush();
    }

    private HttpResponse readResponse(BufferedReader in) throws IOException,
            ProtocolException, UnsupportedEncodingException {
        // read status line
        String line = in.readLine();
        assertNotNull("Unexpected end of stream", line);
        BasicHttpResponse response = new BasicHttpResponse(BasicStatusLine
                .parse(line));

        // read headers
        while ((line = in.readLine()) != null) {
            if ("".equals(line)) {
                break;
            }

            int i = line.indexOf(":");
            assertNotEquals("Malformed header: " + line, -1, i);
            String name = line.substring(0, i);
            String value = line.substring(i + 2);
            response.addHeader(name, value);
        }
        assertNotNull("Unexpected end of stream while reading headers", line);
        return response;
    }

    private String readBody(HttpResponse response) throws IOException {
        int contentLength = -1;
        for (org.apache.http.Header header : response.getAllHeaders()) {
            if ("Content-Length".equals(header.getName())) {
                contentLength = Integer.parseInt(header.getValue());
            }
        }
        // read body
        StringBuilder body = new StringBuilder();
        while (contentLength == -1 || body.length() < contentLength) {
            int c = in.read();
            if (c == -1) {
                fail("Unexpected end of stream while reading body (read "
                        + body.length() + ", expected " + contentLength + "): "
                        + body.toString());
            }
            body.append((char) c);
        }
        return body.toString();
    }

    private static class EmptyResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                boolean outgoing) {
            return HandshakeResponse.createResponse(new Properties());
        }

        public void setLocalePreferencing(boolean b) {
        }
    }

}
