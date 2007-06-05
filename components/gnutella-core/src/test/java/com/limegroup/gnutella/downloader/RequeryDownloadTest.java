package com.limegroup.gnutella.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.limewire.collection.Interval;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.ForMeReplyHandler;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.ManagedConnection;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.RouteTable;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * This test makes sure that LimeWire does the following things:
 * 1) Does NOT requery ever.
 * 2) Wakes up from the WAITING_FROM_RESULTS state when a new, valid query comes
 *    in.
 * 3) Wakes up from the GAVE_UP state when a new, valid query comes in.
 */
@SuppressWarnings( { "unchecked", "cast" } )
public class RequeryDownloadTest 
    extends LimeTestCase {

    /** The main test fixture.  Contains the incomplete file and hash below. */
    private DownloadManager _mgr; 
    /** Where to send and receive messages */
    private static TestMessageRouter _router;
    /** The simulated downloads.dat file.  Used only to build _mgr. */
    private File _snapshot;
    /** The name of the completed file. */
    private String _filename="some file.txt";
    /** The incomplete file to resume from. */
    private File _incompleteFile;    
    /** The hash of file when complete. */
    private URN _hash = TestFile.hash();
    /** The uploader */
    private TestUploader _uploader;
    /** The TestMessageRouter's queryRouteTable. */
    private RouteTable _queryRouteTable;
    /** The TestMessageRouter's FOR_ME_REPLY_HANDLER. */
    private final ReplyHandler _ourReplyHandler = ForMeReplyHandler.instance();
    
    private static final int PORT = 6939;

    static class TestMessageRouter extends MessageRouterStub {
        List /* of QueryMessage */ broadcasts=new LinkedList();
		public void sendDynamicQuery(QueryRequest query) {
            broadcasts.add(query);
            super.sendDynamicQuery(query); //add GUID to route table
        }
        
        public void clearBroadcasts() { 
            broadcasts.clear();
        }
    }

    //////////////////////////// Fixtures /////////////////////////

    public RequeryDownloadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RequeryDownloadTest.class);
    }
    
    public static void globalSetUp() throws Exception {
        setSettings();
        
        _router=new TestMessageRouter();
        new RouterService(new ActivityCallbackStub(), _router);  
        _router.initialize();
    }

    public void setUp() throws Exception {
        setSettings();
        RouterService.setListeningPort(ConnectionSettings.PORT.getValue());
        PrivilegedAccessor.setValue(
            RouterService.class, "manager", new ConnectionManagerStub());
        _queryRouteTable = 
            (RouteTable) PrivilegedAccessor.getValue(_router, 
                                                     "_queryRouteTable");

        createSnapshot();
        _mgr=RouterService.getDownloadManager();
        _mgr.initialize();
        _mgr.scheduleWaitingPump();
        boolean ok=_mgr.readSnapshot(_snapshot);
        assertTrue("Couldn't read snapshot file", ok);
        _uploader=new TestUploader("uploader 6666", 6666, false);
        _uploader.setRate(Integer.MAX_VALUE);
        RouterService.getDownloadManager().clearAllDownloads();
        
        new File( getSaveDirectory(), _filename).delete();
    }    
    
    private static void setSettings() {
        ManagedDownloader.NO_DELAY = true;
        ConnectionSettings.NUM_CONNECTIONS.setValue(0);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        ConnectionSettings.PORT.setValue(PORT);
    }

    /** Creates a downloads.dat file named SNAPSHOT with a faked up
     *  IncompleteFileManager in it.  All of this because we can't access
     *  DownloadManager.incompleteFileManager. */
    private void createSnapshot() throws Exception {
        try {
            //Make IncompleteFileManager with appropriate entries...
            IncompleteFileManager ifm=createIncompleteFile();
            //...and write it to downloads.dat.
            _snapshot = File.createTempFile(
                "ResumeByHashTest", ".dat"
            );
            ObjectOutputStream out = 
                new ObjectOutputStream(new FileOutputStream(_snapshot));
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
       urns.add(_hash);
       RemoteFileDesc rfd = new RemoteFileDesc("1.2.3.4", PORT, 13l,
                                               _filename, TestFile.length(),
                                               new byte[16], 56, false, 4, 
                                               true, null, urns,  false, 
                                               false,"",null, -1, false);

       //Create incompleteFile, write a few bytes
       _incompleteFile=ifm.getFile(rfd);
       try {
           _incompleteFile.delete();
           _incompleteFile.createNewFile();
           OutputStream out=new FileOutputStream(_incompleteFile);
           out.write((byte)TestFile.getByte(0));
           out.write((byte)TestFile.getByte(1));
           out.close();
       } catch (IOException e) { 
           fail("Couldn't create incomplete file", e);
       }

       //Record information in IncompleteFileManager.
       VerifyingFile vf=new VerifyingFile(TestFile.length());
       vf.addInterval(new Interval(0, 1));  //inclusive
       ifm.addEntry(_incompleteFile, vf);       
       return ifm;
    }
       
    public void tearDown() {
        _router.clearBroadcasts();
        _uploader.stopThread();
        if (_incompleteFile != null )
            _incompleteFile.delete();
        if (_snapshot != null)
           _snapshot.delete();
        new File(getSaveDirectory(), _filename).delete();           
    }


    /////////////////////////// Actual Tests /////////////////////////////

    /** Gets response with exact match, starts downloading. */
    public void testExactMatch() throws Exception {
        doTest(_filename, _hash, true);
    }

    /** Gets response with same hash, different name, starts downloading. */
    public void testHashMatch() throws Exception {
        doTest("different name.txt", _hash, true);
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
        // we need to seed the MessageRouter with a GUID that it will recognize
        byte[] guidToUse = GUID.makeGuid();
        _queryRouteTable.routeReply(guidToUse, _ourReplyHandler);

        //Start a download for the given incomplete file.  Give the thread time
        //to start up and send its requery.
        Downloader downloader = null;
        downloader = _mgr.download(_incompleteFile);
        assertTrue(downloader instanceof ResumeDownloader);
        assertEquals(Downloader.QUEUED,downloader.getState());
        
        int counts = 0;
		// Make sure that you are through the QUEUED state.
        while (downloader.getState() == Downloader.QUEUED) {
            Thread.sleep(100);
            if(counts++ > 50)
                fail("took too long, state: " + downloader.getState());
		}
        
        //give the downloading thread time to change states
        Thread.sleep(1000);
        
        assertEquals("downloader isn't waiting for results", 
            Downloader.WAITING_FOR_RESULTS, downloader.getState());

        // no need to do a dldr.resume() cuz ResumeDownloaders spawn the query
        // automatically

        //Check that we can get query of right type.
        //TODO: try resume without URN
        assertEquals("unexpected router.broadcasts size", 1, 
                     _router.broadcasts.size());
        Object m=_router.broadcasts.get(0);
        assertInstanceof("m should be a query request", QueryRequest.class, m);
        QueryRequest qr=(QueryRequest)m;
        // no more requeries
        assertTrue((GUID.isLimeGUID(qr.getGUID())) &&
                   !(GUID.isLimeRequeryGUID(qr.getGUID())));
        // since filename is the first thing ever submitted it should always
        // query for allFiles[0].getFileName()
        String qString =QueryUtils.createQueryString(_filename);
        assertEquals("should have queried for filename", qString, 
                     qr.getQuery());
        assertNotNull("should have some requested urn types", 
            qr.getRequestedUrnTypes() );
        assertEquals("unexpected amount of requested urn types",
            1, qr.getRequestedUrnTypes().size() );
        Set urns=qr.getQueryUrns();
        assertNotNull("urns shouldn't be null", urns);
        assertEquals("should only have NO urn", 0, urns.size());
        // not relevant anymore, we don't send URN queries
        // assertTrue("urns should contain the hash", urns.contains(_hash));

        //Send a response to the query.
        Set responseURNs = null;
        if (responseURN != null) {
            responseURNs = new HashSet(1);
            responseURNs.add(responseURN);
        }
        Response response = newResponse(0l, TestFile.length(),
                                        responseName, responseURNs);
        byte[] ip = {(byte)127, (byte)0, (byte)0, (byte)1};
        QueryReply reply = new QueryReply(guidToUse, 
            (byte)6, 6666, ip, 0l, 
            new Response[] { response }, new byte[16],
            false, false, //needs push, is busy
            true, false,  //finished upload, measured speed
            false, false);//supports chat, is multicast response....
        _router.handleQueryReply(reply, new ManagedConnection("1.2.3.4", PORT));

        //Make sure the downloader does the right thing with the response.
        Thread.sleep(2000);
        counts = 0;
        if (shouldDownload) {
            //a) Match: wait for download to start, then complete.
            while (downloader.getState()!=Downloader.COMPLETE) {            
			    if ( downloader.getState() != Downloader.CONNECTING &&
			         downloader.getState() != Downloader.HASHING &&
			         downloader.getState() != Downloader.SAVING )
                    assertEquals(Downloader.DOWNLOADING, downloader.getState());
                Thread.sleep(500);
                if(counts++ > 60)
                    fail("took too long, state: " + downloader.getState());
            }
        } 
        else {
            //b) No match: keep waiting for results
            assertEquals("downloader should wait for user", 
                Downloader.WAITING_FOR_RESULTS, downloader.getState());
            downloader.stop();
        }
    }
    
    /**
     * Utility method to create a new response.
     */
    private Response newResponse(long index, long size, String name, Set urns) 
					 throws Exception {
        Class gc = PrivilegedAccessor.getClass(Response.class, "GGEPContainer");
        return (Response)PrivilegedAccessor.invokeConstructor(
            Response.class, new Object[] {
                new Long(index), new Long(size), name,
                urns, null, null, null },
            new Class[] {
                Long.TYPE, Long.TYPE, String.class,
                Set.class, LimeXMLDocument.class,
                gc, byte[].class } );
    }
}
