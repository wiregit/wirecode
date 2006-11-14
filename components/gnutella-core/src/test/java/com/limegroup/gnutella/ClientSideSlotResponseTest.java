package com.limegroup.gnutella;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;

import junit.framework.Test;

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
    
    private static Set<String> sharedFiles = new HashSet<String>(2);
    
    private static final String TEXT = "somefile.txt";
    private static final String TORRENT = "somefile.torrent";
    private static void doSettings() throws Exception {
    	SharingSettings.EXTENSIONS_TO_SHARE.setValue(".torrent;.txt");
    	File textFile = new File(_sharedDir,TEXT);
    	File torrentFile = new File(FileManager.APPLICATION_SPECIAL_SHARE,TORRENT);
    	sharedFiles.add(TEXT);
    	sharedFiles.add(TORRENT);
    	CommonUtils.copy(CommonUtils.getResourceFile("com/limegroup/gnutella/util/BaseTestCase.java"), textFile);
    	CommonUtils.copy(CommonUtils.getResourceFile("com/limegroup/gnutella/ClientSideTestCase.java"), torrentFile);
    	rs.getFileManager().addFileAlways(textFile);
    	rs.getFileManager().addFileAlways(torrentFile);
    	Thread.sleep(500);
    	assertEquals(2, rs.getFileManager().getNumFiles());
    }
    
    public static Integer numUPs() {
        return new Integer(3);
    }

    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    private static class UploadManagerStub extends UploadManager {
    	boolean isServiceable, mayBeServiceable;
    	UploadManagerStub() {
    		super(null);
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
    	PrivilegedAccessor.setValue(rs, "uploadManager", uStub);
    	PrivilegedAccessor.setValue(rs.getAcceptor(),"_acceptedIncoming",Boolean.TRUE);
    }

    /**
     * tests that if there are enough slots, responses are processed and
     * returned. 
     */
    public void testResponsesSent() throws Exception {
    	uStub.isServiceable = true;
    	uStub.mayBeServiceable = true;
    	QueryRequest query = QueryRequest.createQuery("somefile");
    	drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(2, responses.size());
    	for(Response r: responses)
    		assertTrue(sharedFiles.contains(r.getName()));
    }
    
    /**
     * Tests that if no queries can be serviceable nothing gets returned.
     */
    public void testNothingSent() throws Exception {
    	uStub.mayBeServiceable = false;
    	QueryRequest query = QueryRequest.createQuery("somefile");
    	drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	assertNull(getFirstQueryReply(testUP[0]));
    }
    
    /**
     * Tests that if only metafiles can be serviced 
     * only results about metafiles are returned. 
     */
    public void testMetaFilesSent() throws Exception {
    	uStub.isServiceable = false;
    	uStub.mayBeServiceable = true;
    	QueryRequest query = QueryRequest.createQuery("somefile");
    	drain(testUP[0]);
    	testUP[0].send(query);
    	testUP[0].flush();
    	Thread.sleep(1000);
    	QueryReply reply = getFirstQueryReply(testUP[0]);
    	List<Response> responses = reply.getResultsAsList();
    	assertEquals(1, responses.size());
    	assertEquals(TORRENT, responses.get(0).getName());
    }
}





