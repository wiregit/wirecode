package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.io.NetworkUtils;
import org.limewire.net.SocketsManager;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.NetworkSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class BrowseHostHandlerTest extends LimeTestCase {

    private final int PORT = 6668;

    private FileManager fileManager;

    private BrowseHostHandler browseHostHandler;

    private SocketsManager socketsManager;

    private QueryReplyHandler queryReplyHandler;
    private Injector injector;

    public BrowseHostHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BrowseHostHandlerTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {

        String directoryName = "com/limegroup/gnutella";
        File sharedDirectory = TestUtils.getResourceFile(directoryName);
        sharedDirectory = sharedDirectory.getCanonicalFile();
        assertTrue("Could not find directory: " + directoryName,
                sharedDirectory.isDirectory());

        File[] testFiles = sharedDirectory.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory() && file.getName().endsWith(".class");
            }
        });
        assertNotNull("No files to test against", testFiles);
        assertGreaterThan("Not enough files to test against", 50,
                testFiles.length);


        SharingSettings.EXTENSIONS_TO_SHARE.setValue("class");
        SharingSettings.DIRECTORIES_TO_SHARE.setValue(Collections
                .singleton(sharedDirectory));

        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        NetworkSettings.PORT.setValue(PORT);

        injector = LimeTestUtils.createInjectorAndStart(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ReplyHandler.class).annotatedWith(Names.named("forMeReplyHandler")).to(QueryReplyHandler.class);
            }
        });

        fileManager = injector.getInstance(FileManager.class);

        fileManager.loadSettingsAndWait(100000);

        assertGreaterThan(0, fileManager.getNumFiles());

        browseHostHandler = injector.getInstance(BrowseHostHandlerManager.class).createBrowseHostHandler(new GUID(), new GUID());
        socketsManager = injector.getInstance(SocketsManager.class);
        queryReplyHandler = (QueryReplyHandler) injector.getInstance(Key.get(ReplyHandler.class, Names.named("forMeReplyHandler")));
    }

    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    public void testCreateInvalidHost() throws Exception {
        Connectable host = BrowseHostHandler.createInvalidHost();
        assertNotNull(host);
        assertEquals("0.0.0.0", host.getAddress());
        assertFalse(NetworkUtils.isValidAddress(host.getAddress()));
        assertTrue(NetworkUtils.isValidPort(host.getPort()));
    }

    public void testBrowseHostBadHTTPStatus() throws Exception {
        HTTPAcceptor httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        httpAcceptor.registerHandler("/", new SimpleNHttpRequestHandler() {
            public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                    HttpContext context) throws HttpException, IOException {
                return null;
            }
            
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                httpResponse.setStatusCode(400);
            }
        });

        Connectable host = new ConnectableImpl("localhost", PORT, false); // TODO true
        SocketsManager.ConnectType type = SocketsManager.ConnectType.PLAIN;  // TODO ConnectType.TLS
        Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                BrowseHostHandler.DIRECT_CONNECT_TIME, type);

        try {
            browseHostHandler.browseHost(socket);
            fail();
        } catch (IOException ioe) {
            // expected result
        }
    }

    public void testBrowseHost() throws Exception {
        Connectable host = new ConnectableImpl("localhost", PORT, false); // TODO true
        SocketsManager.ConnectType type = SocketsManager.ConnectType.PLAIN;  // TODO ConnectType.TLS
        Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                BrowseHostHandler.DIRECT_CONNECT_TIME, type);

        browseHostHandler.browseHost(socket);

        List<String> files = new ArrayList<String>();
        for(QueryReply reply : queryReplyHandler.replies) {
            Response[] results = reply.getResultsArray();
            for (Response result : results) {
                files.add(result.getName());
                assertTrue("Expected .class or LimeWire file, got: " + result.getName(),
                        result.getName().endsWith(".class") || result.getName().toLowerCase().startsWith("limewire"));
            }
        }

        assertEquals(fileManager.getNumFiles(), files.size());

        for (Iterator<Response> it = fileManager.getIndexingIterator(false); it.hasNext();) {
            Response result = it.next();
            boolean contained = files.remove(result.getName());
            assertTrue("File is missing in browse response: "
                    + result.getName(), contained);
        }
        assertTrue("Browse returned more results than shared: " + files,
                files.isEmpty());
    }



    public void testBrowseHostBadContentType() throws Exception {
        HTTPAcceptor httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        httpAcceptor.registerHandler("/", new SimpleNHttpRequestHandler() {
            public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                    HttpContext context) throws HttpException, IOException {
                return null;
            }
            
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                httpResponse.setHeader("Content-Type", "application/foo-bar");
            }
        });

        Connectable host = new ConnectableImpl("localhost", PORT, false); // TODO true
        SocketsManager.ConnectType type = SocketsManager.ConnectType.PLAIN;  // TODO ConnectType.TLS
        Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                BrowseHostHandler.DIRECT_CONNECT_TIME, type);
        try {
            browseHostHandler.browseHost(socket);
            fail();
        } catch (IOException ioe) {
            // expected result
        }
    }

    public void testBrowseHostBadContentEncoding() throws Exception {
        HTTPAcceptor httpAcceptor = injector.getInstance(HTTPAcceptor.class);
        httpAcceptor.registerHandler("/", new SimpleNHttpRequestHandler() {
            public ConsumingNHttpEntity entityRequest(HttpEntityEnclosingRequest request,
                    HttpContext context) throws HttpException, IOException {
                return null;
            }
            
            @Override
            public void handle(HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) throws HttpException, IOException {
                httpResponse.setHeader("Content-Encoding", "zip");
            }
        });

        Connectable host = new ConnectableImpl("localhost", PORT, false); // TODO true
        SocketsManager.ConnectType type = SocketsManager.ConnectType.PLAIN;  // TODO ConnectType.TLS
        Socket socket = socketsManager.connect(new InetSocketAddress(host.getAddress(), host.getPort()),
                                                BrowseHostHandler.DIRECT_CONNECT_TIME, type);

        try {
            browseHostHandler.browseHost(socket);
            fail();
        } catch (IOException ioe) {
            // expected result
        }
    }

    @Singleton
    public static class QueryReplyHandler extends ReplyHandlerStub {

        @Inject
        public QueryReplyHandler(){}

        ArrayList<QueryReply> replies = new ArrayList<QueryReply>();

        @Override
        public void handleQueryReply(QueryReply queryReply, ReplyHandler receivingConnection) {
            replies.add(queryReply);
        }

    }
}
