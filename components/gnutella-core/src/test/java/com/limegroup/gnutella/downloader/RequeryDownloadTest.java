package com.limegroup.gnutella.downloader;

import com.sun.java.util.collections.*;
import java.io.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.stubs.*;
import junit.framework.*;

/**
 * Tests that LimeWire queries by hash properly and starts resuming when
 * appropriate results come in.  This is an integration test covering code in
 * DownloadManager, ManagedDownloader, and ResumeDownloader.  Uses real
 * downloaders, a stubbed-out MessageRouter (no real messaging connections), and
 * test uploaders.
 */
public class RequeryDownloadTest extends TestCase {
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
        public void broadcastQueryRequest(QueryRequest queryRequest) {
            broadcasts.add(queryRequest);
            super.broadcastQueryRequest(queryRequest); //add GUID to route table
        }

        protected void handleQueryReplyForMe(
                QueryReply queryReply, ManagedConnection receivingConnection) {
            //Copied from StandardMessageRouter.
            mgr.handleQueryReply(queryReply);
        }
    }


    //////////////////////////// Fixtures /////////////////////////

    public RequeryDownloadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(RequeryDownloadTest.class);
        //return new RequeryDownloadTest("testRequeryDownload");
    }

    public void setUp() {
        (new File(filename)).delete();
        SettingsManager.instance().setKeepAlive(0);
        SettingsManager.instance().setConnectOnStartup(false);
        try {
            SettingsManager.instance().setSaveDirectory(new File("."));
        } catch (IOException e) {
            fail("Couldn't create saved directory");
        }
        createSnapshot();
        mgr=new DownloadManager();
        router=new TestMessageRouter();
        mgr.initialize(new ActivityCallbackStub(),
                       router,
                       new AcceptorStub(),
                       new FileManagerStub());
        router.initialize(null, 
                          new ConnectionManagerStub(),
                          null,   //HostCatcher
                          null);  //UploadManager
        boolean ok=mgr.readSnapshot(snapshot);
        assertTrue("Couldn't read snapshot file", ok);
        uploader=new TestUploader("uploader 6666", 6666);
        uploader.setRate(Integer.MAX_VALUE);
    }    

    /** Creates a downloads.dat file named SNAPSHOT with a faked up
     *  IncompleteFileManager in it.  All of this because we can't access
     *  DownloadManager.incompleteFileManager. */
    private void createSnapshot() {
        try {
            //Make IncompleteFileManager with appropriate entries...
            IncompleteFileManager ifm=createIncompleteFile();
            //...and write it to downloads.dat.
            snapshot=File.createTempFile("ResumeByHashTest", ".dat");
            ObjectOutputStream out=new ObjectOutputStream(
                new FileOutputStream(snapshot));
            out.writeObject(new ArrayList());   //downloads
            out.writeObject(ifm);
            out.close();
        } catch (IOException e) {
            fail("Couldn't create temp file");
        }
    }

    /** Creates the incomplete file and returns an IncompleteFileManager with
     *  info for that file. */
    public IncompleteFileManager createIncompleteFile() {
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
           fail("Couldn't create incomplete file");
       }

       //Record information in IncompleteFileManager.
       VerifyingFile vf=new VerifyingFile(false);
       vf.addInterval(new Interval(0, 1));  //inclusive
       ifm.addEntry(incompleteFile, vf);       
       return ifm;
    }
       
    public void tearDown() {
        uploader.stopThread();
        incompleteFile.delete();
        if (snapshot!=null)
           snapshot.delete();
    }


    /////////////////////////// Actual Tests /////////////////////////////

    /** Gets response with exact match, starts downloading. */
    public void testExactMatch() {
        doTest(filename, hash, true);
    }

    /** Gets response with same hash, different name, starts downloading. */
    public void testHashMatch() {
        doTest("different name.txt", hash, true);
    }

    /** Gets a response that doesn't match--can't download. */
    public void testNoMatch() {
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
                        boolean shouldDownload) {        
        //Start a download for the given incomplete file.  Give the thread time
        //to start up and send its requery.
        Downloader downloader=null;
        try {
            downloader=mgr.startResumeDownload(incompleteFile);
        } catch (AlreadyDownloadingException e) {
            fail("Already downloading.");
        } catch (CantResumeException e) {
            fail("Invalid incomplete file.");
        }                                
        try { Thread.sleep(200); } catch (InterruptedException e) { }        
        assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());

        //Check that we can get query of right type.
        //TODO: try resume without URN
        assertEquals(1, router.broadcasts.size());
        Object m=router.broadcasts.get(0);
        assertTrue(m instanceof QueryRequest);
        QueryRequest qr=(QueryRequest)m;
        assertTrue(GUID.isLimeRequeryGUID(qr.getGUID()));
        assertEquals(filename, qr.getQuery());
        assertTrue(qr.getRequestedUrnTypes()==null
                   || qr.getRequestedUrnTypes().size()==0);
        Set urns=qr.getQueryUrns();
        assertTrue(urns!=null);
        assertEquals(1, urns.size());
        assertTrue(urns.contains(hash));

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
            false);       //supports chat
        ManagedConnection stubConnection=new ManagedConnectionStub();
        router.handleQueryReply(reply, stubConnection);

        //Make sure the downloader does the right thing with the response.
        try { Thread.sleep(500); } catch (InterruptedException e) { }
        if (shouldDownload) {
            //a) Match: wait for download to start, then complete.
            while (downloader.getState()!=Downloader.COMPLETE) {            
                assertEquals(Downloader.DOWNLOADING, downloader.getState());
                try { Thread.sleep(200); } catch (InterruptedException e) { }
            }
        } else {
            //b) No match: keep waiting for results
            assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
            downloader.stop();
        }
    }    

    
    /**
     * Tests RequeryDownloader, aka the "wishlist" downloader.  It must
     * initially send the right query and only accept the right results.  
     */
    public void testRequeryDownload() {
        ManagedDownloader.TIME_BETWEEN_REQUERIES=5*1000; //5 seconds
        DownloadManager.TIME_BETWEEN_REQUERIES=5*1000;

        //Start a download for the given incomplete file.  Give the thread time
        //to start up, then make sure nothing has been sent initially.
        Downloader downloader=null;
        try {
            downloader=mgr.startRequeryDownload("file name", 
                                                null, 
                                                GUID.makeGuid(),
                                                null);
        } catch (AlreadyDownloadingException e) {
            fail("Already downloading.");
        }
        try { Thread.sleep(200); } catch (InterruptedException e) { }  
        assertEquals(0, router.broadcasts.size());

        //Now wait a few seconds and make sure a requery of right type was sent.
        try { Thread.sleep(6*1000); } catch (InterruptedException e) { }
        assertEquals(1, router.broadcasts.size());
        Object m=router.broadcasts.get(0);
        assertTrue(m instanceof QueryRequest);
        QueryRequest qr=(QueryRequest)m;
        assertTrue(GUID.isLimeRequeryGUID(qr.getGUID()));
        assertEquals("file name", qr.getQuery());
        assertTrue(qr.getRequestedUrnTypes()==null
                   || qr.getRequestedUrnTypes().size()==0);
        assertTrue(qr.getQueryUrns()==null
                   || qr.getQueryUrns().size()==0);
        assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());

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
            false);       //supports chat
        ManagedConnection stubConnection=new ManagedConnectionStub();
        router.handleQueryReply(reply, stubConnection);
        try { Thread.sleep(400); } catch (InterruptedException e) { }
        assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());

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
            false);       //supports chat
        router.handleQueryReply(reply, stubConnection);

        //Make sure the downloader does the right thing with the response.
        try { Thread.sleep(500); } catch (InterruptedException e) { }
        while (downloader.getState()!=Downloader.COMPLETE) {            
            assertEquals(Downloader.DOWNLOADING, downloader.getState());
            try { Thread.sleep(200); } catch (InterruptedException e) { }
        }
    }    
}
