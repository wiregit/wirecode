package com.limegroup.gnutella;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
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
    
    @SuppressWarnings("unused")
    private static void doSettings() throws Exception {
    	SharingSettings.EXTENSIONS_TO_SHARE.setValue(".torrent;.txt");
    	File textFile = new File(_sharedDir,TEXT_FILE);
    	File torrentFile = new File(FileManager.APPLICATION_SPECIAL_SHARE,TORRENT_FILE);
    	File userTorrentFile = new File(_sharedDir,USER_TORRENT);
    	File appTextFile = new File(FileManager.APPLICATION_SPECIAL_SHARE,APP_TXT);
    	File appTorrentFile = new File(FileManager.APPLICATION_SPECIAL_SHARE, OTHER_TORRENT);
    	someFileMatches.add(TEXT_FILE);
    	someFileMatches.add(TORRENT_FILE);
    	someFileMatches.add(USER_TORRENT);
    	someFileMatches.add(APP_TXT);
    	FileUtils.copy(CommonUtils.getResourceFile("com/limegroup/gnutella/gui/GUIBaseTestCase.java"), textFile);
    	FileUtils.copy(CommonUtils.getResourceFile("com/limegroup/gnutella/ClientSideTestCase.java"), torrentFile);
    	FileUtils.copy(CommonUtils.getResourceFile("com/limegroup/gnutella/ClientSideSlotResponseTest.java"), userTorrentFile);
    	FileUtils.copy(CommonUtils.getResourceFile("com/limegroup/gnutella/ServerSideTestCase.java"), appTextFile);
    	FileUtils.copy(CommonUtils.getResourceFile("com/limegroup/gnutella/util/LimeTestCase.java"), appTorrentFile);
        FileEventListenerWaiter waiter = new FileEventListenerWaiter(5);
        ProviderHacks.getFileManager().addFileAlways(textFile, waiter);
        ProviderHacks.getFileManager().addFileAlways(torrentFile, waiter);
        ProviderHacks.getFileManager().addFileAlways(userTorrentFile, waiter);
        ProviderHacks.getFileManager().addFileAlways(appTextFile, waiter);
        ProviderHacks.getFileManager().addFileAlways(appTorrentFile, waiter);
        waiter.waitForLoad();
    	assertEquals(5, ProviderHacks.getFileManager().getNumFiles());
    }
    
    public static Integer numUPs() {
        return new Integer(3);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    private static class UploadManagerStub extends HTTPUploadManager {
    	boolean isServiceable, mayBeServiceable;
    	UploadManagerStub() {
    		super(new UploadSlotManager(), ProviderHacks.getHttpRequestHandlerFactory());
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
    
    private static UploadManagerStub uStub = new UploadManagerStub();
    
    public void setUp() throws Exception {
    	super.setUp();
    	assertTrue("should be open", testUP[0].isOpen());
    	assertTrue("should be up -> leaf",
    			testUP[0].isSupernodeClientConnection());
    	drain(testUP[0], 500);
    	uStub.isServiceable = true;
    	uStub.mayBeServiceable = true;
    	//PrivilegedAccessor.setValue(rs, "uploadManager", uStub);
    	PrivilegedAccessor.setValue(ProviderHacks.getAcceptor(),"_acceptedIncoming",Boolean.TRUE);
    }

    /**
     * tests that if there are enough slots, responses are processed and
     * returned. 
     */
    public void testResponsesSent() throws Exception {
    	uStub.isServiceable = true;
    	uStub.mayBeServiceable = true;
    	QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery(SOME_FILE);
    	drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(someFileMatches.size(), responses.size());
    	for(Response r: responses)
    		assertTrue(someFileMatches.contains(r.getName()));
    }
    
    /**
     * Tests that if no queries can be serviceable nothing gets returned.
     */
    public void testNothingSent() throws Exception {
    	uStub.mayBeServiceable = false;
    	QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery(SOME_FILE);
    	drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	assertNull(getFirstQueryReply(testUP[0]));
    }
    
    /**
     * Tests that if only metafiles can be serviced but
     * there are no metafile results, nothing gets returned.
     */
    public void testAllFiltered() throws Exception {
    	uStub.mayBeServiceable = true;
    	uStub.isServiceable = false;
    	QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery(TEXT_FILE);
    	drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	assertNull(getFirstQueryReply(testUP[0]));
    }
    
    /**
     * tests that if only metafiles can be serviced and
     * all results are for metafiled, nothing gets filetered
     */
    public void testNoneFiltered() throws Exception {
    	uStub.mayBeServiceable = true;
    	uStub.isServiceable = false;
    	QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery(OTHER_TORRENT);
    	drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(1, responses.size());
    	assertEquals(OTHER_TORRENT, responses.get(0).getName());
    }
    
    /**
     * Tests that if only metafiles can be serviced 
     * only results about application-shared metafiles are returned. 
     */
    public void testMetaFilesSent() throws Exception {
    	uStub.isServiceable = false;
    	uStub.mayBeServiceable = true;
    	QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery(SOME_FILE);
    	drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = getFirstQueryReply(testUP[0]);
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





