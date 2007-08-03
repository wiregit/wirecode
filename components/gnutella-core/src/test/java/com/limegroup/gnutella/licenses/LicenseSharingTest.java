package com.limegroup.gnutella.licenses;

import java.io.File;
import java.io.InterruptedIOException;
import java.util.Iterator;

import junit.framework.Test;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ClientSideTestCase;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.routing.PatchTableMessage;
import com.limegroup.gnutella.routing.QueryRouteTable;
import com.limegroup.gnutella.routing.ResetTableMessage;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public final class LicenseSharingTest extends ClientSideTestCase {

	public LicenseSharingTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(LicenseSharingTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
    }
   
    // needed by ClientSideTestCase
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    // needed by ClientSideTestCase
    public static Integer numUPs() {
        return new Integer(3);
    }
	
	// used by ClientSideTestCase
	@SuppressWarnings("unused")
    private static void doSettings() {
	    SharingSettings.EXTENSIONS_TO_SHARE.setValue("mp3;ogg;wma");
        // get the resource file for com/limegroup/gnutella
        File cc1 = CommonUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest0.mp3");
        File cc2 = CommonUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest1.mp3");
        File cc3 = CommonUtils.getResourceFile("com/limegroup/gnutella/licenses/cc1.mp3");
        File cc4 = CommonUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverifytest0.ogg");
        File wma5 = CommonUtils.getResourceFile("com/limegroup/gnutella/licenses/weed-PUSA-LoveEverybody.wma");
        assertTrue(cc1.exists());
        assertTrue(cc2.exists());
        assertTrue(cc3.exists());
        assertTrue(cc4.exists());
        assertTrue(wma5.exists());
        // now move them to the share dir
        FileUtils.copy(cc1, new File(_sharedDir, "cc1.mp3"));
        FileUtils.copy(cc2, new File(_sharedDir, "cc2.mp3"));	    
        FileUtils.copy(cc3, new File(_sharedDir, "cc3.mp3"));
        FileUtils.copy(cc4, new File(_sharedDir, "cc4.ogg"));
        FileUtils.copy(wma5, new File(_sharedDir, "wma5.wma"));
    }
	
	@Override
	public void setUp() throws Exception {
	    super.setUp();
	    
	    RouterService.getFileManager().loadSettingsAndWait(4000);
	}
	
	public void testFileDescKnowsLicense() throws Exception {
	    FileManager fm = RouterService.getFileManager();
	    FileDesc[] fds = fm.getAllSharedFileDescriptors();
	    assertEquals(5, fds.length);
	    for(int i = 0; i < fds.length; i++)
	        assertTrue(fds[i].toString(), fds[i].isLicensed());
    }
    
    public void testQRPExchange() throws Exception {
        assertEquals(5, RouterService.getNumSharedFiles());

        for (int i = 0; i < testUP.length; i++) {
            assertTrue("should be open", testUP[i].isOpen());
            assertTrue("should be up -> leaf", testUP[i].isSupernodeClientConnection());
            if (i != testUP.length - 1)
                drain(testUP[i], 500);
        }

        final int upIndex = testUP.length - 1;
        QueryRouteTable qrt = new QueryRouteTable();
        assertEquals(0.0, qrt.getPercentFull(), 0);
        // need to wait for QRP table to be sent
        Thread.sleep(15000);
        try {
            Message m = null;
            while (true) {
                m = testUP[upIndex].receive(500);
                if (m instanceof ResetTableMessage)
                    qrt.reset((ResetTableMessage) m);
                else if (m instanceof PatchTableMessage)
                    qrt.patch((PatchTableMessage) m);
            }
        } catch (InterruptedIOException bad) {}

        assertGreaterThan(0, qrt.getPercentFull());

        // send a query that should hit in the qrt
        // Check CC
        String richQuery = "<?xml version=\"1.0\"?><audios><audio licensetype=\"creativecommons.org/licenses/\"/></audios>";
        new LimeXMLDocument(richQuery); // make sure it can be constructed.
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("", richQuery);
        assertTrue(qrt.contains(query));
        
        // Check Weed
        richQuery = "<?xml version=\"1.0\"?><audios><audio licensetype=\"http://www.shmedlic.com/license/3play.aspx\"/></audios>";
        new LimeXMLDocument(richQuery); // make sure it can be constructed.
        query = ProviderHacks.getQueryRequestFactory().createQuery("", richQuery);
        assertTrue(qrt.contains(query));
    }
    
    
    public void testCCResultsXMLSearch() throws Exception {
        setAcceptedIncoming();

        String richQuery = "<?xml version=\"1.0\"?><audios><audio licensetype=\"creativecommons.org/licenses/\"/></audios>";
        new LimeXMLDocument(richQuery);
        // we should send a query to the leaf and get results.
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("", richQuery);
        testUP[1].send(query);
        testUP[1].flush();

        QueryReply reply = getFirstQueryReply(testUP[1]);
        assertNotNull(reply);
        assertEquals(query.getGUID(), reply.getGUID());
        assertEquals(reply.getResultsAsList().toString(), 4, reply.getResultCount());
        
        if(!addXMLToResponses(reply))
            fail("Couldn't add XML to response. :(");
        
        for(Iterator i = reply.getResults(); i.hasNext(); ) {
            Response r = (Response)i.next();
            assertNotNull(r.getDocument());
            LimeXMLDocument doc = r.getDocument();
            assertTrue(r.toString(), doc.isLicenseAvailable());
            assertNotNull(r.toString(), doc.getLicense());
            assertInstanceof(CCLicense.class, doc.getLicense());
        }
    }
    
    public void testWeedResultsXMLSearch() throws Exception {
        setAcceptedIncoming();

        String richQuery = "<?xml version=\"1.0\"?><audios><audio licensetype=\"http://www.shmedlic.com/license/3play.aspx\"/></audios>";
        new LimeXMLDocument(richQuery);
        // we should send a query to the leaf and get results.
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("", richQuery);
        testUP[1].send(query);
        testUP[1].flush();

        QueryReply reply = getFirstQueryReply(testUP[1]);
        assertNotNull(reply);
        assertEquals(query.getGUID(), reply.getGUID());
        assertEquals(reply.getResultsAsList().toString(), 1, reply.getResultCount());
        
        if(!addXMLToResponses(reply))
            fail("Couldn't add XML to response. :(");
        
        for(Iterator i = reply.getResults(); i.hasNext(); ) {
            Response r = (Response)i.next();
            LimeXMLDocument doc = r.getDocument();
            assertNotNull(doc);
            assertTrue(r.toString(), doc.isLicenseAvailable());
            assertNotNull(r.toString(), doc.getLicense());
            assertInstanceof(WeedLicense.class, doc.getLicense());
        }
    }
	
	/**
	 * Tests whether a search with a license in the query returns results
	 * without a matching license.
	 */
	public void testLicenseRequiredXMLSearch() throws Exception {
		setAcceptedIncoming();
		
		String richQuery = "<?xml version=\"1.0\"?><audios><audio title=\"love\" licensetype=\"http://www.shmedlic.com/license/3play.aspx\"/></audios>";
        new LimeXMLDocument(richQuery);

        // we should send a query to the leaf and get results.
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("", richQuery);
        testUP[1].send(query);
        testUP[1].flush();

        QueryReply reply = getFirstQueryReply(testUP[1]);
        assertNotNull(reply);
        assertEquals(query.getGUID(), reply.getGUID());
        assertEquals(reply.getResultsAsList().toString(), 1, reply.getResultCount());
        
        if (!addXMLToResponses(reply))
            fail("Couldn't add XML to response. :(");
        
        for (Iterator i = reply.getResults(); i.hasNext(); ) {
            Response r = (Response)i.next();
			LimeXMLDocument doc = r.getDocument();
            assertNotNull(doc);
            assertTrue(r.toString(), doc.isLicenseAvailable());
            assertNotNull(r.toString(), doc.getLicense());
            assertInstanceof(WeedLicense.class, doc.getLicense());
        }
	}
    
	/**
	 * Tests whether a search without a license returns results
	 * with both licenses.
	 */
	public void testLicenselessXMLSearch() throws Exception {
		setAcceptedIncoming();
		
		String richQuery = "<?xml version=\"1.0\"?><audios><audio title=\"love\"/></audios>";
        new LimeXMLDocument(richQuery);

        // we should send a query to the leaf and get results.
        QueryRequest query = ProviderHacks.getQueryRequestFactory().createQuery("", richQuery);
        testUP[1].send(query);
        testUP[1].flush();

        QueryReply reply = getFirstQueryReply(testUP[1]);
        assertNotNull(reply);
        assertEquals(query.getGUID(), reply.getGUID());
        assertEquals(reply.getResultsAsList().toString(), 2, reply.getResultCount());
        
        if (!addXMLToResponses(reply))
            fail("Couldn't add XML to response. :(");
        
        for (Iterator i = reply.getResults(); i.hasNext(); ) {
            Response r = (Response)i.next();
			LimeXMLDocument doc = r.getDocument();
            assertNotNull(doc);
            assertTrue(r.toString(), doc.isLicenseAvailable());
            assertNotNull(r.toString(), doc.getLicense());
        }
	}
    
    private void setAcceptedIncoming() throws Exception {
        Acceptor ac = ProviderHacks.getAcceptor();
        PrivilegedAccessor.setValue(ac, "_acceptedIncoming", Boolean.TRUE);
    }
    
    private boolean addXMLToResponses(QueryReply qr) throws Exception {
        return ((Boolean)PrivilegedAccessor.invokeMethod(
                    ProviderHacks.getForMeReplyHandler(), "addXMLToResponses", qr)).booleanValue();
    }
}
            
