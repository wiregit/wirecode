package com.limegroup.gnutella.downloader;

import com.sun.java.util.collections.*;
import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import junit.framework.*;

/**
 * Tests that LimeWire queries by hash properly and starts resuming when
 * appropriate results come in.  This is an integration test covering code in
 * DownloadManager, ManagedDownloader, and ResumeDownloader.  Uses real
 * downloaders, a stubbed-out MessageRouter (no real messaging connections), and
 * test uploaders.
 */
public class RequeryDownloadTest extends com.limegroup.gnutella.util.BaseTestCase {
    /** The main test fixture.  Contains the incomplete file and hash below. */
    DownloadManager mgr; 
    /** Where to send and receive messages */
    TestMessageRouter router;
    /** The simulated downloads.dat file.  Used only to build mgr. */
    File snapshot;

    /** The name of the completed file. */
    String filename="some file.txt";
    /** The incomplete file to resume from. */
    File incompleteFile;    
    /** The hash of file when complete. */
    URN hash=TestFile.hash();

    /** The uploader */
    TestUploader uploader;

    class TestMessageRouter extends MessageRouterStub {
        List /* of QueryMessage */ broadcasts=new LinkedList();
		public void sendDynamicQuery(QueryRequest query) {
            broadcasts.add(query);
            super.sendDynamicQuery(query); //add GUID to route table
        }
    }


    //////////////////////////// Fixtures /////////////////////////

    public RequeryDownloadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RequeryDownloadTest.class);
        //return new RequeryDownloadTest("testRequeryScheduling");
    }

    public void setUp() throws Exception {
        ManagedDownloader.NO_DELAY = true;
		ConnectionSettings.KEEP_ALIVE.setValue(0);
		ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        SettingsManager.instance().setPort(6346);
        
        router=new TestMessageRouter();
        new RouterService(new ActivityCallbackStub(), router);        
        RouterService.setListeningPort(SettingsManager.instance().getPort());
        PrivilegedAccessor.setValue(
            RouterService.class, "manager", new ConnectionManagerStub());
        router.initialize();

        createSnapshot();
        mgr=RouterService.getDownloadManager();
        mgr.initialize();
        boolean ok=mgr.readSnapshot(snapshot);
        assertTrue("Couldn't read snapshot file", ok);
        uploader=new TestUploader("uploader 6666", 6666);
        uploader.setRate(Integer.MAX_VALUE);
        
        new File( getSaveDirectory(), filename).delete();
    }    

    /** Creates a downloads.dat file named SNAPSHOT with a faked up
     *  IncompleteFileManager in it.  All of this because we can't access
     *  DownloadManager.incompleteFileManager. */
    private void createSnapshot() throws Exception {
        try {
            //Make IncompleteFileManager with appropriate entries...
            IncompleteFileManager ifm=createIncompleteFile();
            //...and write it to downloads.dat.
            snapshot = File.createTempFile(
                "ResumeByHashTest", ".dat"
            );
            ObjectOutputStream out = 
                new ObjectOutputStream(new FileOutputStream(snapshot));
            out.writeObject(new ArrayList());   //downloads
            out.writeObject(ifm);
            out.close();
        } catch (IOException e) {
            fail("Couldn't create temp file", e);
        }
    }

    /** Creates the incomplete file and returns an IncompleteFileManager with
     *  info for that file. */
    public IncompleteFileManager createIncompleteFile() throws Exception {
       IncompleteFileManager ifm=new IncompleteFileManager();
       Set urns=new HashSet(1);
       urns.add(hash);
       RemoteFileDesc rfd=new RemoteFileDesc(
           "1.2.3.4", 6346, 13l,
           filename, TestFile.length(),
           new byte[16], 56, false, 4, true, null,
           urns);

       //Create incompleteFile, write a few bytes
       incompleteFile=ifm.getFile(rfd);
       try {
           incompleteFile.delete();
           incompleteFile.createNewFile();
           OutputStream out=new FileOutputStream(incompleteFile);
           out.write((byte)TestFile.getByte(0));
           out.write((byte)TestFile.getByte(1));
           out.close();
       } catch (IOException e) { 
           fail("Couldn't create incomplete file", e);
       }

       //Record information in IncompleteFileManager.
       VerifyingFile vf=new VerifyingFile(false);
       vf.addInterval(new Interval(0, 1));  //inclusive
       ifm.addEntry(incompleteFile, vf);       
       return ifm;
    }
       
    public void tearDown() {
        uploader.stopThread();
        if ( incompleteFile != null )
            incompleteFile.delete();
        if (snapshot!=null)
           snapshot.delete();
        new File(getSaveDirectory(), filename).delete();           
    }


    /////////////////////////// Actual Tests /////////////////////////////

    /** Gets response with exact match, starts downloading. */
    public void testExactMatch() throws Exception {
        doTest(filename, hash, true);
    }

    /** Gets response with same hash, different name, starts downloading. */
    public void testHashMatch() throws Exception {
        doTest("different name.txt", hash, true);
    }

    /** Gets a response that doesn't match--can't download. */
    public void testNoMatch() throws Exception {
        doTest("some other file.txt", null, false);
    }

    /**
     * Skeleton method for all tests.
     * @param responseName the file name to send in responses
     * @param responseURN the SHA1 urn to send in responses, or null for none
     * @param shouldDownload true if the downloader should actually start
     *  the download.  False if the response shouldn't satisfy it.
     */     
    private void doTest(String responseName, 
                        URN responseURN,
                        boolean shouldDownload) throws Exception {        
        //Start a download for the given incomplete file.  Give the thread time
        //to start up and send its requery.
        Downloader downloader=null;
        downloader=mgr.download(incompleteFile);
        
		// Make sure that you are through the QUEUED state.
        while (downloader.getState()!=Downloader.WAITING_FOR_RESULTS) {         
			if ( downloader.getState() != Downloader.QUEUED )
                assertEquals(Downloader.WAITING_FOR_RESULTS, 
				  downloader.getState());
            Thread.sleep(200);
		}
        assertEquals("downloader isn't waiting for results", 
            Downloader.WAITING_FOR_RESULTS, downloader.getState());

        //Check that we can get query of right type.
        //TODO: try resume without URN
        assertEquals("unexpected router.broadcasts size", 1, router.broadcasts.size());
        Object m=router.broadcasts.get(0);
        assertInstanceof("m should be a query request", QueryRequest.class, m);
        QueryRequest qr=(QueryRequest)m;
        // First query is not a requery
        //assertTrue(GUID.isLimeRequeryGUID(qr.getGUID()));
        assertEquals("should not have queried for filename", "\\", qr.getQuery());
        assertNotNull("should have some requested urn types", 
            qr.getRequestedUrnTypes() );
        assertEquals("unexpected amount of requested urn types",
            1, qr.getRequestedUrnTypes().size() );
        Set urns=qr.getQueryUrns();
        assertNotNull("urns shouldn't be null", urns);
        assertEquals("should only have one urn", 1, urns.size());
        assertTrue("urns should contain the hash", urns.contains(hash));

        //Send a response to the query.
        Set responseURNs=null;
        if (responseURN!=null) {
            responseURNs=new HashSet(1);
            responseURNs.add(responseURN);
        }
        Response response=new Response(0l,   //index
                                       TestFile.length(),
                                       responseName,
                                       null,  //metadata
                                       responseURNs,
                                       null); //metadata
        byte[] ip={(byte)127, (byte)0, (byte)0, (byte)1};
        QueryReply reply=new QueryReply(qr.getGUID(), 
            (byte)6, 6666, ip, 0l, 
            new Response[] { response }, new byte[16],
            false, false, //needs push, is busy
            true, false,  //finished upload, measured speed
            false, false);//supports chat, is multicast response....
        //ManagedConnection stubConnection=new ManagedConnectionStub();
        router.handleQueryReply(reply, new ManagedConnection("1.2.3.4", 6346));

        //Make sure the downloader does the right thing with the response.
        Thread.sleep(1000);
        if (shouldDownload) {
            //a) Match: wait for download to start, then complete.
            while (downloader.getState()!=Downloader.COMPLETE) {            
			    if ( downloader.getState() != Downloader.CONNECTING &&
			         downloader.getState() != Downloader.WAITING_FOR_RESULTS &&
			         downloader.getState() != Downloader.HASHING &&
			         downloader.getState() != Downloader.DOWNLOADING )
                    assertEquals(Downloader.DOWNLOADING, downloader.getState());
                Thread.sleep(200);
            }
        } else {
            //b) No match: keep waiting for results
            assertEquals("downloader isn't waiting for results", 
                Downloader.WAITING_FOR_RESULTS, downloader.getState());
            downloader.stop();
        }
    }    

    
    /**
     * Tests RequeryDownloader, aka the "wishlist" downloader.  It must
     * initially send the right query and only accept the right results.  
     */
    public void testRequeryDownload() throws Exception {
        ManagedDownloader.TIME_BETWEEN_REQUERIES=5*1000; //5 seconds
        DownloadManager.TIME_BETWEEN_REQUERIES=5*1000;

        //Start a download for the given incomplete file.  Give the thread time
        //to start up, then make sure nothing has been sent initially.
        Downloader downloader=null;
        downloader=mgr.download("file name", 
                                null, 
                                GUID.makeGuid(),
                                null);
        Thread.sleep(200);
        assertEquals("nothing should have been sent to start", 
            0, router.broadcasts.size());

        //Now wait a few seconds and make sure a requery of right type was sent.
        Thread.sleep(6*1000);
        assertEquals("unexpected router.broadcasts size", 1, router.broadcasts.size());
        Object m=router.broadcasts.get(0);
        assertInstanceof("m not a queryrequest", QueryRequest.class, m);
        QueryRequest qr=(QueryRequest)m;
		// First query is not counted as requery
        //assertTrue(GUID.isLimeRequeryGUID(qr.getGUID()));
        assertEquals("unexpected query", "file name", qr.getQuery());
        assertNotNull("expected any type of urn", qr.getRequestedUrnTypes());
        assertEquals("only one (any) urn type expected",
            1, qr.getRequestedUrnTypes().size());
        assertNotNull("should have sent atleast an empty length urn set",
            qr.getQueryUrns() );
        assertEquals("wishlist has no URN",
            0, qr.getQueryUrns().size() );
        assertEquals("downloader should be waiting for results", 
            Downloader.WAITING_FOR_RESULTS, downloader.getState());

        //Send a mismatching response to the query, making sure it is ignored.
        //Give the downloader time to start up first.
        Response response=new Response(
            0l, TestFile.length(), "totally different.txt",
            null, null, null);
        byte[] ip={(byte)127, (byte)0, (byte)0, (byte)1};
        QueryReply reply=new QueryReply(qr.getGUID(), 
            (byte)6, 6666, ip, 0l, 
            new Response[] { response }, new byte[16],
            false, false, //needs push, is busy
            true, false,  //finished upload, measured speed
            false, false);//supports chat, is multicast response
        router.handleQueryReply(reply, new ManagedConnection("1.2.3.4", 6346));
        Thread.sleep(400);
        assertEquals("downloader should still be waiting for results",
            Downloader.WAITING_FOR_RESULTS, downloader.getState());

        //Send a good response to the query.
        response=new Response(0l,   //index
                              TestFile.length(),
                              "some file name.txt",
                              null,  //metadata
                              null,  //URNs
                              null); //metadata
        reply=new QueryReply(qr.getGUID(), 
            (byte)6, 6666, ip, 0l, 
            new Response[] { response }, new byte[16],
            false, false, //needs push, is busy
            true, false,  //finished upload, measured speed
            false, false);//supports chat, is multicast response
        router.handleQueryReply(reply, new ManagedConnection("1.2.3.4", 6346));

        //Make sure the downloader does the right thing with the response.
        Thread.sleep(400);
        while (downloader.getState()!=Downloader.COMPLETE &&
               downloader.getState()!=Downloader.CORRUPT_FILE ) {
			if ( downloader.getState() != Downloader.CONNECTING &&
			     downloader.getState() != Downloader.WAITING_FOR_RESULTS &&
			     downloader.getState() != Downloader.HASHING &&
			     downloader.getState() != Downloader.SAVING &&
			     downloader.getState() != Downloader.DOWNLOADING )
                assertEquals("downloader should only be downloading",
                    Downloader.DOWNLOADING, downloader.getState());
            Thread.sleep(200);
        }
        
        assertEquals("download should be complete",
            Downloader.COMPLETE, downloader.getState() );
    }
    
    /** Tests that requeries are sent fairly and at appropriate rate. */
    public void testRequeryScheduling() throws Exception {
        ManagedDownloader.TIME_BETWEEN_REQUERIES=200; //0.1 seconds
        DownloadManager.TIME_BETWEEN_REQUERIES=1000;   //1 second

        Downloader downloader1=null;
        Downloader downloader2=null;
		byte [] guid1 = GUID.makeGuid();
		byte [] guid2 = GUID.makeGuid();
        downloader1=mgr.download("xxxxx", null, guid1, null);
        downloader2=mgr.download("yyyyy", null, guid2, null);

        //Got right number of requeries?
		// Note that the first query will kick off immediately now
        List broadcasts=router.broadcasts;
        Thread.sleep(8000);
		downloader1.stop();
		downloader2.stop();
        assertEquals("unexpected # of broadcasts: ", 
            8, broadcasts.size(), 1); //should be 8, plus fudge factor
        //Are they balanced?  Check for approximate fairness.
        int xCount=0;
        int yCount=0;
        for (int i=0; i<broadcasts.size(); i++) {
            String qr=((QueryRequest)broadcasts.get(i)).getQuery();
            if (qr.equals("xxxxx"))
                xCount++;
            else if (qr.equals("yyyyy"))
                yCount++;
            else
                fail("Invalid query: "+qr);
        }
		// This test is looser than it use to be.  There is no delay on the 
		// first requery now so it is possible that xxxxx can get 2 extra 
		// queries in before yyyyy does.
        assertLessThanOrEquals("Unbalanced x/y count: "+xCount+"/"+yCount, 
                   2, Math.abs(xCount-yCount));
    }
}
