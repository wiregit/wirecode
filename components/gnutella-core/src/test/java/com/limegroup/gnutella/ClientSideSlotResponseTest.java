package com.limegroup.gnutella;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.auth.ContentManager;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.uploader.HttpRequestHandlerFactory;
import com.limegroup.gnutella.uploader.UploadSlotManager;

/**
 * Tests how the availability of upload slots affects responses, as well
 * as chocking of queries to a leaf.
 */
public class ClientSideSlotResponseTest extends ClientSideTestCase {
    public ClientSideSlotResponseTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ClientSideSlotResponseTest.class);
    }    
    
    private static Set<String> someFileMatches = new HashSet<String>(2);
    
    private static final String SOME_FILE = "somefile";
    private static final String TEXT_FILE = SOME_FILE+".txt";
    private static final String TORRENT_FILE = SOME_FILE+".torrent";
    private static final String USER_TORRENT = SOME_FILE+"2.torrent";
    private static final String APP_TXT = SOME_FILE+"2.txt";
    private static final String OTHER_TORRENT = "other.torrent";
    
    @Override
    public void setSettings() throws Exception {
    	SharingSettings.EXTENSIONS_TO_SHARE.setValue(".torrent;.txt");
    	File textFile = new File(_sharedDir,TEXT_FILE);
    	File torrentFile = new File(SharingUtils.APPLICATION_SPECIAL_SHARE,TORRENT_FILE);
    	File userTorrentFile = new File(_sharedDir,USER_TORRENT);
    	File appTextFile = new File(SharingUtils.APPLICATION_SPECIAL_SHARE,APP_TXT);
    	File appTorrentFile = new File(SharingUtils.APPLICATION_SPECIAL_SHARE, OTHER_TORRENT);
    	someFileMatches.add(TEXT_FILE);
    	someFileMatches.add(TORRENT_FILE);
    	someFileMatches.add(USER_TORRENT);
    	someFileMatches.add(APP_TXT);
    	FileUtils.copy(TestUtils.getResourceFile("com/limegroup/gnutella/gui/GUIBaseTestCase.java"), textFile);
    	FileUtils.copy(TestUtils.getResourceFile("com/limegroup/gnutella/ClientSideTestCase.java"), torrentFile);
    	FileUtils.copy(TestUtils.getResourceFile("com/limegroup/gnutella/ClientSideSlotResponseTest.java"), userTorrentFile);
    	FileUtils.copy(TestUtils.getResourceFile("com/limegroup/gnutella/ServerSideTestCase.java"), appTextFile);
    	FileUtils.copy(TestUtils.getResourceFile("com/limegroup/gnutella/util/LimeTestCase.java"), appTorrentFile);
        FileEventListenerWaiter waiter = new FileEventListenerWaiter(5);
        fileManager.addFileAlways(textFile, waiter);
        fileManager.addFileAlways(torrentFile, waiter);
        fileManager.addFileAlways(userTorrentFile, waiter);
        fileManager.addFileAlways(appTextFile, waiter);
        fileManager.addFileAlways(appTorrentFile, waiter);
        waiter.waitForLoad();
    	assertEquals(5, fileManager.getNumFiles());
    }
    
    private UploadManagerStub uploadManagerStub;

    private FileManager fileManager;

    private NetworkManagerStub networkManagerStub;

    private QueryRequestFactory queryRequestFactory;
    
    @Override
    public void setUp() throws Exception {
        networkManagerStub = new NetworkManagerStub();
        Injector injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(UploadManager.class).to(UploadManagerStub.class);
                bind(NetworkManager.class).toInstance(networkManagerStub);
            }
        });
        // done before super.setUp since needed in setSettings which is called from there
        fileManager = injector.getInstance(FileManager.class);
        super.setUp(injector);
        
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        uploadManagerStub = (UploadManagerStub) injector.getInstance(UploadManager.class);
        
        uploadManagerStub.isServiceable = true;
        uploadManagerStub.mayBeServiceable = true;

        networkManagerStub.setAcceptedIncomingConnection(true);
        
        assertTrue("should be open", testUP[0].isOpen());
        assertTrue("should be up -> leaf",
                testUP[0].getConnectionCapabilities().isSupernodeClientConnection());
        BlockingConnectionUtils.drain(testUP[0], 500);
    }

    
    @Override
    public int getNumberOfPeers() {
        return 3;
    }

    @Singleton
    private static class UploadManagerStub extends HTTPUploadManager {
    	
        boolean isServiceable, mayBeServiceable;
    	
        @Inject
        UploadManagerStub(UploadSlotManager slotManager,
                HttpRequestHandlerFactory httpRequestHandlerFactory,
                Provider<ContentManager> contentManager, Provider<HTTPAcceptor> httpAcceptor,
                Provider<FileManager> fileManager, Provider<ActivityCallback> activityCallback,
                TcpBandwidthStatistics tcpBandwidthStatistics) {
            super(slotManager, httpRequestHandlerFactory, contentManager, httpAcceptor,
                    fileManager, activityCallback, tcpBandwidthStatistics);
        }
		@Override
		public synchronized boolean isServiceable() {
			return isServiceable;
		}
		@Override
		public synchronized boolean mayBeServiceable() {
			return mayBeServiceable;
		}
    }
    
    
    /**
     * tests that if there are enough slots, responses are processed and
     * returned. 
     */
    public void testResponsesSent() throws Exception {
    	uploadManagerStub.isServiceable = true;
    	uploadManagerStub.mayBeServiceable = true;
    	QueryRequest query = queryRequestFactory.createQuery(SOME_FILE);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(someFileMatches.size(), responses.size());
    	for(Response r: responses)
    		assertTrue(someFileMatches.contains(r.getName()));
    }
    
    /**
     * Tests that if no queries can be serviceable nothing gets returned.
     */
    public void testNothingSent() throws Exception {
    	uploadManagerStub.mayBeServiceable = false;
    	QueryRequest query = queryRequestFactory.createQuery(SOME_FILE);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	assertNull(BlockingConnectionUtils.getFirstQueryReply(testUP[0]));
    }
    
    /**
     * Tests that if only metafiles can be serviced but
     * there are no metafile results, nothing gets returned.
     */
    public void testAllFiltered() throws Exception {
    	uploadManagerStub.mayBeServiceable = true;
    	uploadManagerStub.isServiceable = false;
    	QueryRequest query = queryRequestFactory.createQuery(TEXT_FILE);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	assertNull(BlockingConnectionUtils.getFirstQueryReply(testUP[0]));
    }
    
    /**
     * tests that if only metafiles can be serviced and
     * all results are for metafiled, nothing gets filetered
     */
    public void testNoneFiltered() throws Exception {
    	uploadManagerStub.mayBeServiceable = true;
    	uploadManagerStub.isServiceable = false;
    	QueryRequest query = queryRequestFactory.createQuery(OTHER_TORRENT);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(1, responses.size());
    	assertEquals(OTHER_TORRENT, responses.get(0).getName());
    }
    
    /**
     * Tests that if only metafiles can be serviced 
     * only results about application-shared metafiles are returned. 
     */
    public void testMetaFilesSent() throws Exception {
    	uploadManagerStub.isServiceable = false;
    	uploadManagerStub.mayBeServiceable = true;
    	QueryRequest query = queryRequestFactory.createQuery(SOME_FILE);
    	BlockingConnectionUtils.drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = BlockingConnectionUtils.getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(1, responses.size());
    	assertEquals(TORRENT_FILE, responses.get(0).getName());
    }
    
    private static class FileEventListenerWaiter implements FileEventListener {
        private final CountDownLatch latch;
        
        public FileEventListenerWaiter(int waitings) {
            this.latch = new CountDownLatch(waitings);
        }

        public void handleFileEvent(FileManagerEvent evt) {
            latch.countDown();            
        }
        
        public void waitForLoad() throws Exception {
            latch.await(5, TimeUnit.SECONDS);
        }
        
    }
}





