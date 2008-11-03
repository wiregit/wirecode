package com.limegroup.gnutella;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.entity.ConsumingNHttpEntity;
import org.apache.http.nio.protocol.SimpleNHttpRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.io.Connectable;
import org.limewire.io.ConnectableImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.net.SocketsManager;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.FileManagerTestUtils;
import com.limegroup.gnutella.messages.QueryReply;
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
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        NetworkSettings.PORT.setValue(PORT);

        injector = LimeTestUtils.createInjectorAndStart(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ReplyHandler.class).annotatedWith(Names.named("forMeReplyHandler")).to(QueryReplyHandler.class);
            }
        });

        fileManager = injector.getInstance(FileManager.class);

        FileManagerTestUtils.waitForLoad(fileManager, 5000);
        
        File dir = TestUtils.getResourceFile("com/limegroup/gnutella");
        File[] testFiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return !file.isDirectory() && file.getName().endsWith(".class");
            }
        });
        for(File file : testFiles) {
            assertNotNull(fileManager.getGnutellaSharedFileList().add(file).get(1, TimeUnit.SECONDS));
        }
        assertGreaterThan("Not enough files to test against", 50, testFiles.length);
        assertGreaterThan(0, fileManager.getGnutellaSharedFileList().size());

        browseHostHandler = injector.getInstance(BrowseHostHandlerManager.class).createBrowseHostHandler(new GUID(), new GUID());
        socketsManager = injector.getInstance(SocketsManager.class);
        queryReplyHandler = (QueryReplyHandler) injector.getInstance(Key.get(ReplyHandler.class, Names.named("forMeReplyHandler")));
    }

    @Override
    protected void tearDown() throws Exception {
        injector.getInstance(LifecycleManager.class).shutdown();
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
            browseHostHandler.browseHost(socket, new AnonymousPresence());
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

        browseHostHandler.browseHost(socket, new AnonymousPresence());

        List<String> files = new ArrayList<String>();
        for(QueryReply reply : queryReplyHandler.replies) {
            Response[] results = reply.getResultsArray();
            for (Response result : results) {
                files.add(result.getName());
                assertTrue("Expected .class or LimeWire file, got: " + result.getName(),
                        result.getName().endsWith(".class") || result.getName().toLowerCase().startsWith("limewire"));
            }
        }

        assertEquals(fileManager.getGnutellaSharedFileList().size(), files.size());

        fileManager.getGnutellaSharedFileList().getReadLock().lock();
        try {
            for(FileDesc result : fileManager.getGnutellaSharedFileList()) {
                boolean contained = files.remove(result.getFileName());
                assertTrue("File is missing in browse response: "
                    + result.getFileName(), contained);
            }
        } finally {
            fileManager.getGnutellaSharedFileList().getReadLock().unlock();
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
            browseHostHandler.browseHost(socket, new AnonymousPresence());
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
            browseHostHandler.browseHost(socket, new AnonymousPresence());
            fail();
        } catch (IOException ioe) {
            // expected result
        }
    }
    
    public void testGetPathForNonAnonymousFriend() {
        assertEquals("/friend/browse/me%40you.com/", browseHostHandler.getPath(new StubFriendPresence("me@you.com")));
        assertEquals("/friend/browse/Hello+There/", browseHostHandler.getPath(new StubFriendPresence("Hello There")));
    }
    
    private static class AnonymousPresence implements FriendPresence {
        @Override
        public Friend getFriend() {
            return new Friend() {
                @Override
                public String getId() {
                    // TODO Auto-generated method stub
                    return null;
                }
                @Override
                public String getName() {
                    // TODO Auto-generated method stub
                    return null;
                }
                @Override
                public Network getNetwork() {
                    // TODO Auto-generated method stub
                    return null;
                }
                @Override
                public String getRenderName() {
                    // TODO Auto-generated method stub
                    return null;
                }
                @Override
                public boolean isAnonymous() {
                    return true;
                }
                @Override
                public void setName(String name) {
                    // TODO Auto-generated method stub
                    
                }
            };
        }

        public String getPresenceId() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ListenerSupport<FeatureEvent> getFeatureListenerSupport() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Collection<Feature> getFeatures() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Feature getFeature(URI id) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean hasFeatures(URI... id) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void addFeature(Feature feature) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void removeFeature(URI id) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
    
    private static class StubFriendPresence implements FriendPresence {

        private final String myId;

        public StubFriendPresence(String myId) {
            this.myId = myId;
        }

        @Override
        public Friend getFriend() {
            return new Friend() {

                @Override
                public String getId() {
                    return null;
                }

                @Override
                public String getName() {
                    return null;
                }

                @Override
                public Network getNetwork() {
                    return new Network() {
                        @Override
                        public String getMyID() {
                            return myId;
                        }
                        @Override
                        public String getNetworkName() {
                            return "";
                        }
                    };
                }

                @Override
                public String getRenderName() {
                    return null;
                }

                @Override
                public boolean isAnonymous() {
                    return false;
                }

                @Override
                public void setName(String name) {
                }
                
            };
        }

        @Override
        public String getPresenceId() {
            return null;
        }

        public ListenerSupport<FeatureEvent> getFeatureListenerSupport() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Collection<Feature> getFeatures() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Feature getFeature(URI id) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean hasFeatures(URI... id) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void addFeature(Feature feature) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void removeFeature(URI id) {
            //To change body of implemented methods use File | Settings | File Templates.
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
