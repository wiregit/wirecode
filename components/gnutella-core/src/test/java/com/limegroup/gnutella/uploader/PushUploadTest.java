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
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineParser;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.net.ConnectionDispatcher;
import org.limewire.net.SocketsManager;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ConnectionManagerImpl;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.NetworkManagerImpl;
import com.limegroup.gnutella.NodeAssigner;
import com.limegroup.gnutella.QueryUnicaster;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.connection.BlockingConnectionFactory;
import com.limegroup.gnutella.connection.ConnectionCheckerManager;
import com.limegroup.gnutella.connection.RoutedConnectionFactory;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.handshaking.HandshakeResponder;
import com.limegroup.gnutella.handshaking.HandshakeResponse;
import com.limegroup.gnutella.handshaking.HeadersFactory;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequestFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.PushRequestImpl;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.settings.NetworkSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.settings.UltrapeerSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.statistics.OutOfBandStatistics;
import com.limegroup.gnutella.util.LimeTestCase;

//ITEST
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

    private FileManager fm;

    private BufferedReader in;

    private BufferedWriter out;

    private LifecycleManager lifeCycleManager;

    private MyNetworkManager networkManager;

    private MyConnectionManager connectionManager;

    private Injector injector;

    public PushUploadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PushUploadTest.class);
    }

    private static void doSettings() throws Exception {
        SharingSettings.ADD_ALTERNATE_FOR_SELF.setValue(false);
        FilterSettings.BLACK_LISTED_IP_ADDRESSES
                .setValue(new String[] { "*.*.*.*" });
        FilterSettings.WHITE_LISTED_IP_ADDRESSES.setValue(new String[] {
                "127.*.*.*", InetAddress.getLocalHost().getHostAddress() });
        NetworkSettings.PORT.setValue(PORT);

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
        File testDir = TestUtils.getResourceFile(testDirName);
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

        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(NetworkManager.class).to(MyNetworkManager.class);
                bind(ConnectionManager.class).to(MyConnectionManager.class);
            }
        });

        // start services
        fm = injector.getInstance(FileManager.class);
        fm.startAndWait(4000);
        
        lifeCycleManager = injector.getInstance(LifecycleManager.class);
        lifeCycleManager.start();
        
        networkManager = (MyNetworkManager) injector.getInstance(NetworkManager.class);
        
        connectionManager = (MyConnectionManager) injector.getInstance(ConnectionManager.class);
    }

    @Override
    public void tearDown() throws Exception {
        closeConnection();

        if (lifeCycleManager != null) {
            lifeCycleManager.shutdown();
        }
        
        LimeTestUtils.waitForNIO();
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
        networkManager.acceptedIncomingConnection = true;
        establishConnection();
        HttpRequest request = new BasicHttpRequest("GET", url,
                HttpVersion.HTTP_1_1);
        HttpResponse response = sendRequest(request);
        UploadTestUtils.assertNotHasHeader(response, "X-Push-Proxy: 1.2.3.4:5");

        // now try with an empty set of proxies
        establishPushConnection();       
        networkManager.acceptedIncomingConnection = false;
        response = sendRequest(request);
        UploadTestUtils.assertNotHasHeader(response, "X-Push-Proxy: 1.2.3.4:5");
    }

    public void testForPushProxyHeaderWithProxy() throws Exception {
        establishPushConnection();

        // now try with some proxies
        Connectable ppi = new ConnectableImpl("1.2.3.4", 5, false);
        connectionManager.proxies.add(ppi);

        networkManager.acceptedIncomingConnection = false;
        HttpRequest request = new BasicHttpRequest("GET", url,
                HttpVersion.HTTP_1_1);
        HttpResponse response = sendRequest(request);
        UploadTestUtils.assertHasHeader(response, "X-Push-Proxy: 1.2.3.4:5");
    }

    /**
     * Tests the scenario where we receive the same push request message more
     * than once.
     */
    public void testDuplicatePushes() throws Exception {
        BlockingConnection connection = injector.getInstance(BlockingConnectionFactory.class).createConnection("localhost", PORT);
        try {
            connection.initialize(injector.getInstance(HeadersFactory.class).createUltrapeerHeaders(null),
                    new EmptyResponder(), 1000);
            QueryRequest query = injector.getInstance(QueryRequestFactory.class).createQuery("txt", (byte) 3);
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

            PushRequest push = new PushRequestImpl(GUID.makeGuid(), (byte) 3, reply
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
    private Socket getSocketFromPush() throws IOException,
            BadPacketException {
        BlockingConnection connection = injector.getInstance(BlockingConnectionFactory.class).createConnection("localhost", PORT);
        try {
            connection.initialize(injector.getInstance(HeadersFactory.class).createUltrapeerHeaders(null),
                    new EmptyResponder(), 1000);

            // send query
            QueryRequest query = injector.getInstance(QueryRequestFactory.class).createQuery("txt", (byte) 3);
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
            PushRequest push = new PushRequestImpl(GUID.makeGuid(), (byte) 3, guid,
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
        BasicHttpResponse response = new BasicHttpResponse(BasicLineParser.parseStatusLine(line, null));

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

    @Singleton
    private static class MyNetworkManager extends NetworkManagerImpl {

        private boolean acceptedIncomingConnection = true;

        @Inject
        public MyNetworkManager(Provider<UDPService> udpService, Provider<Acceptor> acceptor,
                Provider<DHTManager> dhtManager, Provider<ConnectionManager> connectionManager,
                Provider<ActivityCallback> activityCallback, OutOfBandStatistics outOfBandStatistics, 
                NetworkInstanceUtils networkInstanceUtils, Provider<CapabilitiesVMFactory> capabilitiesVMFactory) {
            super(udpService, acceptor, dhtManager, connectionManager, activityCallback,
                    outOfBandStatistics, networkInstanceUtils, capabilitiesVMFactory);
        }

        @Override
        public boolean acceptedIncomingConnection() {
            return acceptedIncomingConnection;
        }

    }

    @Singleton
    private static class MyConnectionManager extends ConnectionManagerImpl {

        private Set<Connectable> proxies = new TreeSet<Connectable>();

        @Inject
        public MyConnectionManager(NetworkManager networkManager,
                Provider<HostCatcher> hostCatcher,
                @Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                Provider<SimppManager> simppManager,
                CapabilitiesVMFactory capabilitiesVMFactory,
                RoutedConnectionFactory managedConnectionFactory,
                Provider<MessageRouter> messageRouter,
                Provider<QueryUnicaster> queryUnicaster,
                SocketsManager socketsManager,
                ConnectionServices connectionServices,
                Provider<NodeAssigner> nodeAssigner,
                Provider<IPFilter> ipFilter,
                ConnectionCheckerManager connectionCheckerManager,
                PingRequestFactory pingRequestFactory,
                NetworkInstanceUtils networkInstanceUtils) {
            super(networkManager, hostCatcher, connectionDispatcher, backgroundExecutor,
                    simppManager, capabilitiesVMFactory, managedConnectionFactory,
                    messageRouter, queryUnicaster, socketsManager, connectionServices,
                    nodeAssigner, ipFilter, connectionCheckerManager, pingRequestFactory, networkInstanceUtils);
        }
        
        @Override
        public Set<Connectable> getPushProxies() {
            return proxies ;
        }
    };

}
