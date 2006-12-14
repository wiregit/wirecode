package com.limegroup.gnutella.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.HugeTestUtils;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.SupernodeAssigner;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.auth.ContentResponseData.Authorization;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.messages.vendor.HeadPing;
import com.limegroup.gnutella.messages.vendor.HeadPong;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.tigertree.TigerTreeCache;
import com.limegroup.gnutella.udpconnect.UDPConnection;
import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IntervalSet;
import com.limegroup.gnutella.util.IpPortSet;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import com.limegroup.gnutella.util.Sockets;

/**
 * Comprehensive test of downloads -- one of the most important tests in
 * LimeWire.
 */
public class DownloadTest extends BaseTestCase {
    
    private static final Log LOG = LogFactory.getLog(DownloadTest.class);
    
    /**
     * Port for the first uploader.
     */
    private static final int PORT_1 = 6321;

    /**
     * Port for the second uploader.
     */
    private static final int PORT_2 = 6322;

    /**
     * Port for the third uploader.
     */
    private static final int PORT_3 = 6323;


    /**
     * Port for the fourth uploader.
     */
    private static final int PORT_4 = 6324;

    /**
     * Port for the fifth uploader.
     */
    private static final int PORT_5 = 6325;
    
    
    /**
     * ports for the various push proxies
     */
    private static final int PPORT_1 = 10001;
    private static final int PPORT_2 = 10002;
    private static final int PPORT_3 = 10003;
    
    private static final GUID guid = new GUID(GUID.makeGuid());
        

    private static final String filePath =
        "com/limegroup/gnutella/downloader/DownloadTestData/";
    
    private static File dataDir = 
        CommonUtils.getResourceFile(filePath);
    private static File saveDir = 
        (CommonUtils.getResourceFile(filePath + "saved")).getAbsoluteFile();
    
    // a random name for the saved file
    private static final String savedFileName = "DownloadTester2834343.out";
    private static File savedFile;
    
    private static TestUploader uploader1;
    private static TestUploader uploader2;
    private static TestUploader uploader3;
    private static TestUploader uploader4;
    private static TestUploader uploader5;
	private static DownloadManager dm;// = new DownloadManager();
	private static final ActivityCallbackStub callback = new MyCallback();
	private static ManagedDownloader DOWNLOADER = null;
	private static Object COMPLETE_LOCK = new Object();
	private static boolean REMOVED = false;
    
    // default to waiting for 2 defaults.
    private final static long DOWNLOAD_WAIT_TIME = 1000 * 60 * 4;	
    
    private static boolean saveAltLocs = false;
    private static Set validAlts = null;
    private static Set invalidAlts = null;
    
    private InetSocketAddress addr;
    
    public static void globalSetUp() throws Exception {
        // raise the download-bytes-per-sec so stealing is easier
        DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.setValue(10);
		RouterService rs = new RouterService(callback);
        dm = RouterService.getDownloadManager();
        dm.initialize();

        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
                "_acceptedIncoming",new Boolean(true));
        assertTrue(RouterService.acceptedIncomingConnection());
        
        ConnectionManagerStub cmStub = new ConnectionManagerStub() {
            public boolean isConnected() {
                return true;
            }
        };
        
        PrivilegedAccessor.setValue(RouterService.class,"manager",cmStub);
        
        RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());
        
        assertTrue(RouterService.isConnected());
        
        //SimpleTimer timer = new SimpleTimer(true);
        Runnable click = new Runnable() {
            public void run() {
                dm.measureBandwidth();
            }
        };
        RouterService.schedule(click,0,SupernodeAssigner.TIMER_DELAY);
        rs.start();
    } 
    
    public DownloadTest(String name) {
        super(name);
    }

    public static Test suite() { 
        return buildTestSuite(DownloadTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() throws Exception {
        DOWNLOADER = null;
        
        dm.clearAllDownloads();

        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        // Don't wait for network connections for testing
        ManagedDownloader.NO_DELAY = true;
        
        uploader1=new TestUploader("PORT_1", PORT_1);
        uploader2=new TestUploader("PORT_2", PORT_2);
        uploader3=new TestUploader("PORT_3", PORT_3);
        uploader4=new TestUploader("PORT_4", PORT_4);
        uploader5=new TestUploader("PORT_5", PORT_5);
        
        deleteAllFiles();
        
        dataDir.mkdirs();
        saveDir.mkdirs();
        
        try {
            SharingSettings.setSaveDirectory(saveDir);        
        } catch(IOException e) {
            fail( "cannot set save directory.", e);
        }
        
        //Pick random name for file.
        savedFile = new File( saveDir, savedFileName );
        savedFile.delete();
        ConnectionSettings.CONNECTION_SPEED.setValue(1000);
        
        callback.delCorrupt = false;
        callback.corruptChecked = false;
        TigerTreeCache.instance().purgeTree(TestFile.hash());
        
        InetAddress address = InetAddress.getByName("64.61.25.171");
    	addr = new InetSocketAddress(address, 10000);
    }    

    public void tearDown() {

        uploader1.reset();
        uploader2.reset();
        uploader3.reset();
        uploader4.reset();
        uploader5.reset();
        
        uploader1.stopThread();
        uploader2.stopThread();
        uploader3.stopThread();
        uploader4.stopThread();
        uploader5.stopThread();

        deleteAllFiles();
        RouterService.getAltlocManager().purge();
        
        try {
            Map m = (Map)PrivilegedAccessor.getValue(PushEndpoint.class,"GUID_PROXY_MAP");
            m.clear();
        }catch(Exception e){
            ErrorService.error(e);
        }
        // get rid of any pushlocs in the map
        System.runFinalization();
        System.gc();
        
        FileDesc []shared = RouterService.getFileManager().getAllSharedFileDescriptors();
        for (int i = 0; i < shared.length; i++) 
            RouterService.getFileManager().removeFileIfShared(shared[i].getFile());
        
        saveAltLocs = false;
        validAlts = null;
        invalidAlts = null;
    }
    
    private static void deleteAllFiles() {
        if ( !dataDir.exists() ) return;
        
        File[] files = dataDir.listFiles();
        for(int i=0; i< files.length; i++) {
            if(files[i].isDirectory()) {
                if(files[i].getName().equalsIgnoreCase("incomplete"))
                    deleteDirectory(files[i]);
                else if(files[i].getName().equals(saveDir.getName()) )
                    deleteDirectory(files[i]);
            }
        }
        dataDir.delete();
    }
    
    private static void  deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        for(int i=0; i< files.length; i++) 
            files[i].delete();
        dir.delete();
    }

        /*
    public void testLegacy() {
        String args[] = {};
        

        tOverlapCheckSpeed(5);
        cleanup();
        tOverlapCheckSpeed(25);
        cleanup();
        tOverlapCheckSpeed(125);
        cleanup();
    }
            */
    
    
    ////////////////////////// Test Cases //////////////////////////

    
    /**
     * Tests a basic download that does not swarm.
     */
    public void testSimpleDownload10() throws Exception {
        LOG.info("-Testing non-swarmed download...");
        
        RemoteFileDesc rfd=newRFD(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd};
        tGeneric(rfds);
    }
    
    public void testSimpleDownload11() throws Exception {
        LOG.info("-Testing non-swarmed download...");
        
        RemoteFileDesc rfd=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd};
        tGeneric(rfds);
    }
    
    /**
     * tests http11 downloads and the gray area allocation.
     */
    public void testTHEXDownload11() throws Exception {
        LOG.info("-Testing chunk allocation in a thex download...");
        
        
        final RemoteFileDesc rfd=newRFDWithURN(PORT_1, 100);
        final IncompleteFileManager ifm=dm.getIncompleteFileManager();
        RemoteFileDesc[] rfds = {rfd};
        
        HTTP11Listener grayVerifier = new HTTP11Listener() {
            //private int requestNo;
            //public void requestHandled(){}
            //public void thexRequestStarted() {}

            /** The only lease that is DEFAULT_CHUNK_SIZE large */
       	    private Interval firstLease = null;
            
            public void requestHandled(){}
            public void thexRequestStarted() {}
            public void thexRequestHandled() {}
            
       	    // checks whether we request chunks at the proper offset, etc.
            public void requestStarted(TestUploader uploader) {
                long fileSize = 0;
                
       	        Interval i = null;
                try {
       	            IntervalSet leased = null;
       	            File incomplete = null;
                    incomplete = ifm.getFile(rfd);
                    assertNotNull(incomplete);
                    VerifyingFile vf = ifm.getEntry(incomplete);
                    fileSize = ((Integer)PrivilegedAccessor.getValue(vf, "completedSize")
                                  ).longValue();
                    assertNotNull(vf);
            	    leased = (IntervalSet)
                                    PrivilegedAccessor.getValue(vf,"leasedBlocks");
            	    assertNotNull(leased);
                    List l = leased.getAllIntervalsAsList();
                    assertEquals(1,l.size());
                    i = (Interval)l.get(0);
                } catch (Exception bad) {
                  fail(bad);
                }
        		
                if (firstLease == null) {
                    // first request, we should have the chunk aligned to
                    // a DEFAULT_CHUNK_SIZE boundary
                    assertEquals("First chunk has improperly aligned low byte.",
                            0, i.low % VerifyingFile.DEFAULT_CHUNK_SIZE);
                    if (i.high != fileSize-1 &&
                            i.high % VerifyingFile.DEFAULT_CHUNK_SIZE != 
                                VerifyingFile.DEFAULT_CHUNK_SIZE-1) {
                        assertTrue("First chunk has improperly aligned high byte.",
                                false);
                    }
                    firstLease = i;
                } else {
                    // on all other requests, we have 256k blocks
                    // Check that the low byte is aligned    
                    if (i.low % (256 * 1024) != 0 &&
                            i.low != firstLease.high + 1) {
                        assertTrue("Un-aligned low byte on chunk that is "+
                                "not adjascent to the DEFAULT_CHUNK_SIZE chunk.",
                                false);
                    }
                    // Check that the high byte is aligned    
                    if (i.high % (256 * 1024) != 256*1024-1 &&
                            i.high != firstLease.low - 1 &&
                            i.high != fileSize-1) {
                        assertTrue("Un-aligned high byte on chunk that is "+
                                "not adjascent to the DEFAULT_CHUNK_SIZE chunk "+
                                "and is not the last chunk of the file",
                                false);
                    }
                } // close of if-else
            } // close of method
        }; // close of inner class
        
        uploader1.setHTTPListener(grayVerifier);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(true);
        
        TigerTreeCache.instance().purgeTree(rfd.getSHA1Urn());
        RouterService.download(rfds, Collections.EMPTY_LIST, null, false);
        
        waitForComplete();
        assertEquals(6,uploader1.getRequestsReceived());
        if (isComplete())
            LOG.debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        
        for (int i=0; i<rfds.length; i++) {
            File incomplete=ifm.getFile(rfds[i]);
            VerifyingFile vf=ifm.getEntry(incomplete);
            assertNull("verifying file should be null", vf);
        }
        
        assertEquals(1, uploader1.getConnections());
    }
    
    public void testSimplePushDownload() throws Exception {
        LOG.info("-Testing non-swarmed push download");
        
        AlternateLocation pushLoc = AlternateLocation.create(
                guid.toHexString()+";127.0.0.1:"+PPORT_1,TestFile.hash());
       ((PushAltLoc)pushLoc).updateProxies(true);

        RemoteFileDesc rfd = newRFDPush(PPORT_1,1);
        
        assertTrue(rfd.needsPush());
        
        RemoteFileDesc [] rfds = {rfd};
        TestUploader uploader = new TestUploader("push uploader");
        new UDPAcceptor(PPORT_1,RouterService.getPort(), savedFile.getName(),uploader,guid);
        
        tGeneric(rfds);
    }
    
    public void testSimpleSwarm() throws Exception {
        LOG.info("-Testing swarming from two sources...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);
        
        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }

    public void testSimpleSwarmPush() throws Exception {
        LOG.info("-Testing swarming from two sources, one push...");  
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        AlternateLocation pushLoc = AlternateLocation.create(
                guid.toHexString()+";127.0.0.1:"+PPORT_2,TestFile.hash());
       ((PushAltLoc)pushLoc).updateProxies(true);
        RemoteFileDesc rfd2 = pushLoc.createRemoteFileDesc(TestFile.length());
        
        
        TestUploader uploader = new TestUploader("push uploader");
        uploader.setRate(100);
        uploader1.setRate(100);
        
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        new UDPAcceptor(PPORT_2,RouterService.getPort(),savedFile.getName(),uploader,guid);
        
        tGeneric(rfds);
        
        assertLessThan("u1 did all the work", TestFile.length(), 
                uploader1.fullRequestsUploaded());
        
        assertGreaterThan("pusher did all the work ",0,uploader1.fullRequestsUploaded());
    }

    public void testUnbalancedSwarm() throws Exception  {
        LOG.info("-Testing swarming from two unbalanced sources...");
        
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE/10);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        assertLessThan("u1 did all the work", 9*TestFile.length()/10+FUDGE_FACTOR*10, u1);
        assertLessThan("u2 did all the work", TestFile.length()/10+FUDGE_FACTOR, u2);
    }


    public void testSwarmWithInterrupt() throws Exception {
        LOG.info("-Testing swarming from two sources (one broken)...");
        
        final int RATE=100;
        final int STOP_AFTER = TestFile.length()/4;
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);

        // Download first from rfd2 so we get its stall
        // and then add in rfd1.
        tGeneric(new RemoteFileDesc[] { rfd2 },
                 new RemoteFileDesc[] { rfd1 });

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThanOrEquals("u2 did too much work", STOP_AFTER, u2);
        assertGreaterThan(0,u2);
    }
    
    /**
     * tests a swarm from a 1.0 and 1.1 source - designed to test stealing.
     */
    public void testSwarmWithTheft() throws Exception {
        LOG.info("-Testing swarming from two sources, one 1.0 and one 1.1");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);
        
        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }
    
    /**
     * tests a generic swarm from a lot of sources with thex.  Meant to be run repetitevely
     * to find weird scheduling issues
     */
    public void testBigSwarm() throws Exception {
        LOG.info(" Testing swarming from many sources");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.T3_SPEED_INT);
        final int RATE = 20; // slow to allow swarming
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1,100);
        RemoteFileDesc rfd2 = newRFDWithURN(PORT_2,100);
        RemoteFileDesc rfd3 = newRFDWithURN(PORT_3,100);
        RemoteFileDesc rfd4 = newRFDWithURN(PORT_4,100);
        RemoteFileDesc rfd5 = newRFDWithURN(PORT_5,100);
        RemoteFileDesc pushRFD1 = newRFDPush(PPORT_1,1);
        RemoteFileDesc pushRFD2 = newRFDPush(PPORT_2,2);
        RemoteFileDesc pushRFD3 = newRFDPush(PPORT_3,3);
        
        TestUploader first = new TestUploader("first pusher");
        TestUploader second = new TestUploader("second pusher");
        TestUploader third = new TestUploader("third pusher");
        
        new UDPAcceptor(PPORT_1,RouterService.getPort(),savedFile.getName(),first,guid);
        new UDPAcceptor(PPORT_2,RouterService.getPort(),savedFile.getName(),second,guid);
        new UDPAcceptor(PPORT_3,RouterService.getPort(),savedFile.getName(),third,guid);
        
        
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        uploader3.setRate(RATE);
        uploader4.setRate(RATE);
        uploader5.setRate(RATE);
        first.setRate(RATE);
        second.setRate(RATE);
        third.setRate(RATE);
        
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(true);
        
        RemoteFileDesc []rfds = new RemoteFileDesc[] {rfd1,rfd2,rfd3,rfd4,rfd5,pushRFD1,pushRFD2,pushRFD3};
        
        tGeneric(rfds);
        
        // no assesrtions really - just test completion and observe behavior in logs
        
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
        
    }

    public void testAddDownload() throws Exception {
        LOG.info("-Testing addDownload (increases swarming)...");
        
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);

        Downloader download=null;

        //Start one location, wait a bit, then add another.
        download=RouterService.download(new RemoteFileDesc[] {rfd1}, false, null);
        ((ManagedDownloader)download).addDownload(rfd2,true);

        waitForComplete();
        if (isComplete())
            LOG.debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        //Make sure there weren't too many overlapping regions. Each upload
        //should do roughly half the work.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        assertLessThan("u1 did all the work", (TestFile.length()/2+FUDGE_FACTOR), u1);
        assertLessThan("u2 did all the work", (TestFile.length()/2+FUDGE_FACTOR), u2);
    }

    public void testStallingUploaderReplaced() throws Exception  {
        LOG.info("-Testing download completion with stalling downloader...");
        
        //Throttle rate at 100KB/s to give opportunities for swarming.
        final int RATE=100;
        uploader1.setRate(0.1f);//stalling uploader
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1};

        ManagedDownloader downloader = (ManagedDownloader) 
            RouterService.download(rfds,Collections.EMPTY_LIST, null, false);
        
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+1000);
        
        downloader.addDownload(rfd2,false);

        waitForComplete();

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        
        LOG.debug("passed"+"\n");//file downloaded? passed
    }
    
    
    public void testStallingHeaderUploader() throws Exception  {
        LOG.info("-Testing download completion with stalling downloader...");
        
        //Throttle rate at 100KB/s to give opportunities for swarming.
        final int RATE=300;
        uploader1.setStallHeaders(true); //stalling uploader
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1};

        ManagedDownloader downloader = (ManagedDownloader) 
            RouterService.download(rfds,Collections.EMPTY_LIST, null, false);
        
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()/2);
        
        downloader.addDownload(rfd2,false);

        waitForComplete();

         
        // the stalled uploader should not have uploaded anything
        assertEquals(0,uploader1.getAmountUploaded());
        
        LOG.debug("passed"+"\n");//file downloaded? passed
    }
    
    public void testAcceptableSpeedStallIsReplaced() throws Exception {
        LOG.info("-Testing a download that is an acceptable speed but slower" +
                  " is replaced by another download that is faster");
        
        final int SLOW_RATE = 5;
        final int FAST_RATE = 50;
        uploader1.setRate(SLOW_RATE);
        uploader2.setRate(FAST_RATE);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);
        
        Thread.sleep(8000);
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        int c1 = uploader1.getConnections();
        int c2 = uploader2.getConnections();
        
        assertEquals("u1 served full request", 0, u1);
        assertGreaterThan("u1 didn't upload anything",0,uploader1.getAmountUploaded());
        assertGreaterThan("u2 not used", 0, u2);
        assertEquals("extra connection attempts", 1, c1);
        assertEquals("extra connection attempts", 1, c2);
        assertTrue("slower uploader not replaced",uploader1.killedByDownloader);
        assertFalse("faster uploader killed",uploader2.killedByDownloader);
    }
    
    public void testUploaderLowHigherRange()  throws Exception {
        LOG.info("-Testing that a download can handle an uploader giving low+higher ranges");
        uploader1.setRate(25);
        uploader1.setLowChunkOffset(50);
        uploader5.setRate(100); // control, to finish the test.
        RemoteFileDesc rfd5 = newRFDWithURN(PORT_5, 100);
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        ManagedDownloader md = (ManagedDownloader)
            RouterService.download(rfds,true,null);
        
        Thread.sleep(5000);
        // at this point we should stall since we'll never get our 50 bytes
        md.addDownloadForced(rfd5,false);
        
        waitForComplete();
        assertGreaterThanOrEquals(50,uploader5.fullRequestsUploaded());
        assertGreaterThanOrEquals(100000-50,uploader1.fullRequestsUploaded());
    }
    
    public void testUploaderLowLowerRange() throws Exception {
        LOG.info("-Testing that a download can handle an uploader giving low+lower ranges");
        uploader1.setRate(25);
        uploader1.setLowChunkOffset(-10);
        uploader5.setRate(100); // control, to finish the test.
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd5 = newRFDWithURN(PORT_5, 100);
        
        RemoteFileDesc[] rfds = {rfd1};
        ManagedDownloader md = (ManagedDownloader)
            RouterService.download(rfds,true,null);
        
        Thread.sleep(5000);
        md.addDownloadForced(rfd5,false);
        // the first downloader should have failed after downloading a complete chunk
        
        assertLessThan(100001,uploader1.fullRequestsUploaded());
        waitForComplete();
        
    }
    
    public void testUploaderHighHigherRange() throws Exception {
        LOG.info("-Testing that a download can handle an uploader giving high+higher ranges");
        uploader1.setRate(25);
        uploader1.setHighChunkOffset(50);
        uploader5.setRate(100); // control, to finish the test.
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd5 = newRFDWithURN(PORT_5, 100);
        
        RemoteFileDesc[] rfds = {rfd1};
        ManagedDownloader md = (ManagedDownloader)
            RouterService.download(rfds,true,null);
        
        Thread.sleep(5000);
        md.addDownloadForced(rfd5,false);
        
        // the first downloader should have failed without downloading a complete chunk
        
        assertEquals(0,uploader1.fullRequestsUploaded());
        waitForComplete();
    }
    
    public void testUploaderHighLowerRange() throws Exception {
        LOG.info("-Testing that a download can handle an uploader giving high+lower ranges");
        uploader1.setRate(25);
        uploader1.setHighChunkOffset(-10);
        uploader5.setRate(100); // control, to finish the test.
        RemoteFileDesc rfd5 = newRFDWithURN(PORT_5, 100);
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        ManagedDownloader md = (ManagedDownloader)
            RouterService.download(rfds,true,null);
        
        Thread.sleep(5000);
        // at this point we should stall since we'll never get our 10 bytes
        md.addDownloadForced(rfd5,false);
        
        waitForComplete();
        assertGreaterThanOrEquals(50,uploader5.fullRequestsUploaded());
        assertGreaterThanOrEquals(100000-50,uploader1.fullRequestsUploaded());
    }
       
    public void testReuseHostWithBadTree() throws Exception {
        LOG.info("-Testing that a host with a bad tree will be used");
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(false);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        TigerTreeCache.instance().purgeTree(TestFile.hash());
        
        // the tree will fail, but it'll pick up the content-length
        // and discard the rest of the bad data.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = TigerTreeCache.instance().getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(uploader1.thexWasRequested());
        assertEquals(1, uploader1.getConnections());        
    }
    
    public void testReuseHostWithBadTreeAndNoContentLength() throws Exception {
        LOG.info("-Testing that a host with a bad tree will be used");
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(false);
        uploader1.setSendContentLength(false);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        TigerTreeCache.instance().purgeTree(TestFile.hash());
        
        // should pass after a bit because it retries the host
        // who gave it the bad length.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = TigerTreeCache.instance().getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(uploader1.thexWasRequested());
        assertEquals(2, uploader1.getConnections());
    }
    
    public void testGetsThex() throws Exception {
        LOG.info("test that a host gets thex");
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        TigerTreeCache.instance().purgeTree(TestFile.hash());
        
        // it will fail the first time, then re-use the host after
        // a little waiting and not request thex.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = TigerTreeCache.instance().getHashTree(TestFile.hash());
        assertNotNull(tree);
        assertEquals(TestFile.tree().getRootHash(), tree.getRootHash());
        
        assertTrue(uploader1.thexWasRequested());
        assertEquals(1, uploader1.getConnections());        
    }
    
    public void testQueuedOnThexContinues() throws Exception {
        LOG.info("test that queued on thex continues");
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setQueueOnThex(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        
        // it will fail the first time, then re-use the host after
        // a little waiting and not request thex.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = TigerTreeCache.instance().getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(uploader1.thexWasRequested());
        assertEquals(1, uploader1.getConnections());
    }
    
    public void testBadHeaderOnThexContinues() throws Exception {
        LOG.info("test bad header on thex continues");
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setUseBadThexResponseHeader(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        TigerTreeCache.instance().purgeTree(TestFile.hash());
        
        // it will fail the first time, then re-use the host after
        // a little waiting and not request thex.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = TigerTreeCache.instance().getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(uploader1.thexWasRequested());
        assertEquals(1, uploader1.getConnections());
    }    
    
    public void testKeepCorrupt() throws Exception {
        LOG.info("-Testing that if the user chooses to keep a corrupt download the download" +
                "will eventually finish");
        final int RATE = 100;
        uploader1.setCorruption(true);
        uploader1.setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(true);
        uploader1.setRate(RATE);
        
        uploader2.setCorruption(true);
        uploader2.setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        uploader2.setRate(RATE);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        
        RouterService.download(new RemoteFileDesc[]{rfd1, rfd2}, Collections.EMPTY_LIST, null, false);
        waitForComplete();

        
        HashTree tree = TigerTreeCache.instance().getHashTree(TestFile.hash());
        assertNull(tree);
        assertTrue(callback.corruptChecked);
        
        // tried once or if twice then failed the second time.
        assertLessThanOrEquals(2, uploader1.getConnections());
        // tried once or twice.
        assertLessThanOrEquals(2, uploader2.getConnections());
        
        assertGreaterThanOrEquals(TestFile.length(), 
                uploader1.getAmountUploaded()+uploader2.getAmountUploaded());
    }
    
    public void testDiscardCorrupt() throws Exception {
        LOG.info("-Testing that if the user chooses to discard a corrupt download it will terminate" +
                "immediately");
        
        final int RATE = 100;
        callback.delCorrupt = true;
        callback.corruptChecked = false;
        uploader1.setCorruption(true);
        uploader1.setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(true);
        uploader1.setRate(RATE);
        
        uploader2.setCorruption(true);
        uploader2.setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        uploader2.setRate(RATE);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        
        tGenericCorrupt( new RemoteFileDesc[] { rfd1}, new RemoteFileDesc[] {rfd2} );
        HashTree tree = TigerTreeCache.instance().getHashTree(TestFile.hash());
        assertNull(tree);
        assertTrue(callback.corruptChecked);
        
        // tried once or if twice then failed the second time.
        assertLessThanOrEquals(2, uploader1.getConnections());
        // tried once or twice.
        assertLessThanOrEquals(2, uploader2.getConnections());
        
        assertGreaterThanOrEquals(TestFile.length(), 
                uploader1.getAmountUploaded()+uploader2.getAmountUploaded());
    }
    
    
    public void testMismatchedVerifyHashNoStopOnCorrupt() throws Exception {
        tMismatchedVerifyHash(false, false);
    }
    
    public void testMismatchedVerifyHashStopOnCorrupt() throws Exception {
        callback.delCorrupt = true;        
        tMismatchedVerifyHash(true, false);
    }
    
    public void testMismatchedVerifyHashWithThexNoStopOnCorrupt()
      throws Exception {
        tMismatchedVerifyHash(false, true);
    }
    
    public void testMismatchedVerifyHashWithThexStopOnCorrupt() throws Exception{
        callback.delCorrupt = true;
        tMismatchedVerifyHash(true, true);
    }

    // note that this test ONLY works because the TestUploader does NOT SEND
    // a Content-Urn header.  if it did, the download would immediately fail
    // when reading the header. 
    private void tMismatchedVerifyHash(boolean deleteCorrupt, boolean getThex )
      throws Exception {
        LOG.info("-Testing file declared corrupt, when hash of "+
                         "downloaded file mismatches bucket hash" +
                         "stop when corrupt "+ deleteCorrupt+" ");
        String badSha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";

        final int RATE=100;
        uploader1.setRate(RATE);
        uploader1.setSendThexTreeHeader(getThex);
        uploader1.setSendThexTree(getThex);
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1,100, badSha1);
        
        URN badURN = URN.createSHA1Urn(badSha1);
        TigerTreeCache.instance().purgeTree(TestFile.hash());
        TigerTreeCache.instance().purgeTree(badURN);
        
        RouterService.download(new RemoteFileDesc[] {rfd1}, false, null);
        // even though the download completed, we ignore the tree 'cause the
        // URNs didn't match.
        assertNull(TigerTreeCache.instance().getHashTree(TestFile.hash()));
        assertNull(TigerTreeCache.instance().getHashTree(badURN));

        waitForComplete(deleteCorrupt);
        assertTrue(callback.corruptChecked);
        assertEquals(getThex, uploader1.thexWasRequested());
        assertEquals(1, uploader1.getConnections());
    }

    public void testTwoAlternateLocations() throws Exception {  
        LOG.info("-Testing Two AlternateLocations...");
        
        final int RATE = 50;
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        
        RemoteFileDesc rfd1=
                         newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=
                         newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        List alt1 = uploader1.incomingGoodAltLocs;
        List alt2 = uploader2.incomingGoodAltLocs;

        AlternateLocation al1 = AlternateLocation.create(rfd1);
        AlternateLocation al2 = AlternateLocation.create(rfd2);
        
        assertTrue("uploader didn't recieve alt", !alt1.isEmpty());
        assertTrue("uploader didn't recieve alt", !alt2.isEmpty());
        assertTrue("uploader got wrong alt", !alt1.contains(al1));
        assertEquals("incorrect number of locs ",1,alt1.size());
        assertTrue("uploader got wrong alt", !alt2.contains(al2));
        assertEquals("incorrect number of locs ",1,alt2.size());
    }

    public void testUploaderAlternateLocations() throws Exception {  
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        LOG.info("-Testing swarming from two sources one based on alt...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=
                          newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        RemoteFileDesc rfd2=
                          newRFDWithURN(PORT_2,100,TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection ualt = 
            AlternateLocationCollection.create(rfd2.getSHA1Urn());

        
        AlternateLocation al2 =
            AlternateLocation.create(rfd2);
        ualt.add(al2);

        uploader1.setGoodAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }
    /**
     * tests that an uploader will pass a push loc which will be included in the swarm
     */
    public void testUploaderPassesPushLoc() throws Exception {
        LOG.info("-Testing swarming from two sources one based on a push alt...");
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader1.stopAfter(800000);
        
        TestUploader pusher = new TestUploader("push uploader");
        pusher.setRate(RATE);
        
        AlternateLocation pushLoc = AlternateLocation.create(
                guid.toHexString()+";127.0.0.1:"+PPORT_1,TestFile.hash());
        
        AlternateLocationCollection alCol=AlternateLocationCollection.create(TestFile.hash());
        alCol.add(pushLoc);
        
        uploader1.setGoodAlternateLocations(alCol);
        
        RemoteFileDesc rfd = newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        
        RemoteFileDesc []rfds = {rfd};
        
        new UDPAcceptor(PPORT_1,RouterService.getPort(), savedFile.getName(),pusher,guid);
        
        tGeneric(rfds);
        
        assertGreaterThan("u1 didn't do enough work ",100*1024,uploader1.fullRequestsUploaded());
        assertGreaterThan("pusher didn't do enough work ",100*1024,pusher.fullRequestsUploaded());
    }
    
    /**
     * tests that a push uploader passes push loc and the new push loc receives
     * the first uploader as an altloc.
     */
    public void testPushUploaderPassesPushLoc() throws Exception {
        LOG.info("Test push uploader passes push loc");
        final int RATE=500;
        
        TestUploader first = new TestUploader("first pusher");
        first.setRate(RATE/3);
        first.stopAfter(700000);
        
        TestUploader second = new TestUploader("second pusher");
        second.setRate(RATE);
        second.stopAfter(700000);
        second.setInterestedInFalts(true);
        
        GUID guid2 = new GUID(GUID.makeGuid());
        
        AlternateLocation firstLoc = AlternateLocation.create(
                guid.toHexString()+";127.0.0.2:"+PPORT_1,TestFile.hash());
        
        AlternateLocation pushLoc = AlternateLocation.create(
                guid2.toHexString()+";127.0.0.2:"+PPORT_2,TestFile.hash());
        
        AlternateLocationCollection alCol=AlternateLocationCollection.create(TestFile.hash());
        alCol.add(pushLoc);
        
        first.setGoodAlternateLocations(alCol);
        
        new UDPAcceptor(PPORT_1,RouterService.getPort(), savedFile.getName(),first,guid);
        new UDPAcceptor(PPORT_2,RouterService.getPort(), savedFile.getName(),second,guid2);
        
        RemoteFileDesc []rfd ={newRFDPush(PPORT_1,1,2)};
        
        tGeneric(rfd);
        
        
        assertGreaterThan("first pusher did no work",100000,first.fullRequestsUploaded());
        assertGreaterThan("second pusher did no work",100000,second.fullRequestsUploaded());
        
        assertEquals(1,second.incomingGoodAltLocs.size());
        
        assertTrue("interested uploader didn't get first loc",
                second.incomingGoodAltLocs.contains(firstLoc));
        
        
    }
    
    /**
     * tests that a download from a push location becomes an alternate location.
     * 
     * It creates a push uploader from which we must create a PushLoc.  
     * After a while, two open uploaders join the swarm  -one which is interested 
     * in receiving push locs and one which isn't.  The interested one should
     * receive the push loc, the other one should not.
     */
    public void testPusherBecomesPushLocAndSentToInterested() throws Exception {
        LOG.info("-Testing push download creating a push location...");
        final int RATE=200;
        uploader1.setRate(RATE);
        uploader1.setInterestedInFalts(true);
        uploader1.stopAfter(600000);
        uploader2.setRate(RATE);
        uploader2.setInterestedInFalts(false);
        uploader2.stopAfter(300000);
        
        TestUploader pusher = new TestUploader("push uploader");
        pusher.setRate(RATE);
        pusher.stopAfter(200000);
        
        
        AlternateLocation pushLoc = AlternateLocation.create(
                guid.toHexString()+";127.0.0.2:"+PPORT_1,TestFile.hash());
        
        RemoteFileDesc pushRFD = newRFDPush(PPORT_1,1,2);
        
        assertFalse(pushRFD.supportsFWTransfer());
        assertTrue(pushRFD.needsPush());

        RemoteFileDesc openRFD1 = newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        RemoteFileDesc openRFD2 = newRFDWithURN(PORT_2,100,TestFile.hash().toString());
        
        RemoteFileDesc []now={pushRFD};
        HashSet later=new HashSet();
        later.add(openRFD1);
        later.add(openRFD2);
        
        new UDPAcceptor(PPORT_1,RouterService.getPort(), savedFile.getName(),pusher,guid);

        
        ManagedDownloader download=
            (ManagedDownloader)RouterService.download(now, Collections.EMPTY_LIST, null, false);
        Thread.sleep(1000);
        download.addDownload(later,false);

        waitForComplete();
        

        assertGreaterThan("u1 did no work",100000,uploader1.getAmountUploaded());

        assertGreaterThan("u2 did no work",100000,uploader2.getAmountUploaded());
        assertLessThan("u2 did too much work",550*1024,uploader2.getAmountUploaded());

        assertGreaterThan("pusher did no work",100*1024,pusher.getAmountUploaded());
        
        
        List alc = uploader1.incomingGoodAltLocs;
        assertTrue("interested uploader did not get pushloc",alc.contains(pushLoc));
        
        
        alc=uploader2.incomingGoodAltLocs;
        assertFalse("not interested uploader got pushloc",alc.contains(pushLoc));
        
        
        alc=pusher.incomingGoodAltLocs;
        assertFalse("not interested uploader got pushloc",alc.contains(pushLoc));
        

    }
    
    /**
     * tests that a pushloc which we thought did not support FWT 
     * but actually does updates its status through the headers,
     * as well as that the set of push proxies is getting updated.
     */
    public void testPushLocUpdatesStatus() throws Exception {
        LOG.info("testing that a push loc updates its status");
        final int RATE=100;
        
        UDPService.instance().setReceiveSolicited(true);
        uploader1.setRate(RATE);
        uploader1.stopAfter(900000);
        uploader1.setInterestedInFalts(true);
        
        TestUploader pusher2 = new TestUploader("firewalled pusher");
        pusher2.setRate(RATE);
        pusher2.stopAfter(200000);
        pusher2.setFirewalled(true);
        pusher2.setProxiesString("1.2.3.4:5,6.7.8.9:10");
        pusher2.setInterestedInFalts(true);
        
        // create a set of the expected proxies and keep a ref to it
        PushEndpoint pe = new PushEndpoint(guid.toHexString()+";1.2.3.4:5;6.7.8.9:10");
        
        Set expectedProxies = new IpPortSet();
        expectedProxies.addAll(pe.getProxies());
        
        PushAltLoc pushLocFWT = (PushAltLoc)AlternateLocation.create(
                guid.toHexString()+";5:4.3.2.1;127.0.0.2:"+PPORT_2,TestFile.hash());
        pushLocFWT.updateProxies(true);
        
        assertEquals(1,pushLocFWT.getPushAddress().getProxies().size());
        
        RemoteFileDesc openRFD = newRFDWithURN(PORT_1,100);
        
        RemoteFileDesc pushRFD2 = newRFDPush(PPORT_2, 1, 2);
        assertFalse(pushRFD2.supportsFWTransfer());
        assertTrue(pushRFD2.needsPush());
        
        new UDPAcceptor(PPORT_2,RouterService.getPort(), savedFile.getName(),pusher2,guid);
        
        RemoteFileDesc [] now = {pushRFD2};
        
        ManagedDownloader download=
            (ManagedDownloader)RouterService.download(now, Collections.EMPTY_LIST, null, false);
        Thread.sleep(2000);
        download.addDownload(openRFD,false);
        waitForComplete();
        
        List alc = uploader1.incomingGoodAltLocs;
        assertEquals(1,alc.size());
        
        PushAltLoc pushLoc = (PushAltLoc)alc.iterator().next();
        
        assertEquals(UDPConnection.VERSION,pushLoc.supportsFWTVersion());
        
        RemoteFileDesc readRFD = pushLoc.createRemoteFileDesc(1);
        assertTrue(readRFD.supportsFWTransfer());
        
        assertEquals(expectedProxies.size(),readRFD.getPushProxies().size());
        
        assertTrue(expectedProxies.containsAll(readRFD.getPushProxies()));
        
    }
    
    /**
     * tests that bad push locs get removed
     */
    public void testBadPushLocGetsDemotedNotAdvertised() throws Exception {
        LOG.info("test that bad push loc gets demoted and not advertised");
        // this test needs to go slowly so that the push attempt may time out
        final int RATE=15;
        
        uploader1.setInterestedInFalts(true);
        uploader2.setInterestedInFalts(true);
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        uploader1.stopAfter(550000);
        uploader2.stopAfter(550000);
        
        AlternateLocation badPushLoc=AlternateLocation.create(
                guid.toHexString()+";1.2.3.4:5",TestFile.hash());
        ((PushAltLoc)badPushLoc).updateProxies(true);
        
        AlternateLocationCollection alc = 
            AlternateLocationCollection.create(TestFile.hash());
        
        alc.add(badPushLoc);
        
        uploader1.setGoodAlternateLocations(alc);
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1,100);
        RemoteFileDesc rfd2 = newRFDWithURN(PORT_2,100);
        
        RemoteFileDesc [] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);
        
        assertGreaterThan("u1 did no work",100*1024,uploader1.fullRequestsUploaded());
        assertGreaterThan("u2 did no work",100*1024,uploader2.fullRequestsUploaded());
        
        assertFalse("bad pushloc got advertised",
                uploader2.incomingGoodAltLocs.contains(badPushLoc));
        assertEquals(1,uploader1.incomingGoodAltLocs.size());
        assertTrue(uploader1.incomingGoodAltLocs.contains(AlternateLocation.create(rfd2)));
        
        assertEquals(1,uploader1.incomingBadAltLocs.size());
        AlternateLocation current = (AlternateLocation)uploader1.incomingBadAltLocs.get(0);
        
        assertTrue(current instanceof PushAltLoc);
        PushAltLoc pcurrent = (PushAltLoc)current;
        assertTrue(pcurrent.getPushAddress().getProxies().isEmpty());
        assertTrue(pcurrent.isDemoted());
        
    }
    
    public void testAlternateLocationsAreRemoved() throws Exception {  
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        LOG.info("-Testing swarming from two sources one based on alt...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=5;
        // Make sure uploader2 will never complete an upload
        final int STOP_AFTER = 0;
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        uploader3.setRate(RATE);
        RemoteFileDesc rfd1=
                        newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=
                        newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        RemoteFileDesc rfd3=
                        newRFDWithURN(PORT_3, 100, TestFile.hash().toString());
        
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection ualt = 
                         AlternateLocationCollection.create(rfd2.getSHA1Urn());


        AlternateLocation al1 = AlternateLocation.create(rfd1);
        AlternateLocation al2 = AlternateLocation.create(rfd2);
        AlternateLocation al3 = AlternateLocation.create(rfd3);
        ualt.add(al2);
        ualt.add(al3);

        uploader1.setGoodAlternateLocations(ualt);
        
        saveAltLocs = true;
        tGeneric(rfds);

        //Now let's check that the uploaders got the correct AltLocs.
        //Uploader 1: Must have al3. 
        //Uploader 1 got correct Alts?
        List alts = uploader1.incomingGoodAltLocs;
        assertTrue(alts.contains(al3));
        
        // al2 should have been sent to uploader1 as NAlt header
        assertTrue(uploader1.incomingBadAltLocs.contains(al2));

        // uploader3 should contain only al1
        alts = uploader3.incomingGoodAltLocs;
        assertTrue(alts.contains(al1));
        assertFalse(alts.contains(al2));
        
        // Test Downloader has correct alts: the downloader should have 
        // 2 or 3. If two they should be u1 and u3. If 3 u2 should be demoted
        assertTrue(validAlts.contains(al1)); 
        assertTrue(validAlts.contains(al3)); 
        Iterator iter = validAlts.iterator(); 
        while(iter.hasNext()) { 
            AlternateLocation loc = (AlternateLocation)iter.next(); 
            if(loc.equals(al2)) 
                assertTrue("failed loc not demoted",loc.isDemoted()); 
        }
        
        // ManagedDownloader clears validAlts and invalidAlts after completion
        assertEquals(Downloader.COMPLETE, DOWNLOADER.getState());
        assertTrue(((Set)PrivilegedAccessor.getValue(DOWNLOADER, "validAlts")).isEmpty());
        assertTrue(((Set)PrivilegedAccessor.getValue(DOWNLOADER, "invalidAlts")).isEmpty());
    }    

    public void testWeirdAlternateLocations() throws Exception {  
        LOG.info("-Testing AlternateLocation weird...");
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1};
        
        
        //Prebuild some uploader alts
        AlternateLocationCollection ualt = 
            AlternateLocationCollection.create(
                HugeTestUtils.EQUAL_SHA1_LOCATIONS[0].getSHA1Urn());

        ualt.add(HugeTestUtils.EQUAL_SHA1_LOCATIONS[0]);
        ualt.add(HugeTestUtils.EQUAL_SHA1_LOCATIONS[1]);
        ualt.add(HugeTestUtils.EQUAL_SHA1_LOCATIONS[2]);
        ualt.add(HugeTestUtils.EQUAL_SHA1_LOCATIONS[3]);
        uploader1.setGoodAlternateLocations(ualt);

        saveAltLocs = true;
        tGeneric(rfds);
        
        //Check to check the alternate locations
        List alt1 = uploader1.incomingGoodAltLocs;
        assertEquals("uploader got bad alt locs",0,alt1.size());
        
        AlternateLocation agood = AlternateLocation.create(rfd1);
        assertTrue(validAlts.contains(agood)); 
        assertFalse(validAlts.contains(HugeTestUtils.EQUAL_SHA1_LOCATIONS[0])); 
        assertFalse(validAlts.contains(HugeTestUtils.EQUAL_SHA1_LOCATIONS[1])); 
        assertFalse(validAlts.contains(HugeTestUtils.EQUAL_SHA1_LOCATIONS[2]));
        
        // ManagedDownloader clears validAlts and invalidAlts after completion
        assertEquals(Downloader.COMPLETE, DOWNLOADER.getState());
        assertTrue(((Set)PrivilegedAccessor.getValue(DOWNLOADER, "validAlts")).isEmpty());
        assertTrue(((Set)PrivilegedAccessor.getValue(DOWNLOADER, "invalidAlts")).isEmpty());
    }

    public void testAddSelfToMeshWithTree() throws Exception {
        
        // change the minimum required bytes so it'll be added.
        PrivilegedAccessor.setValue(HTTPDownloader.class,
            "MIN_PARTIAL_FILE_BYTES", new Integer(1) );
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
            "_acceptedIncoming", Boolean.TRUE );
            
        LOG.info("-Testing that downloader adds itself to the mesh if it has a tree");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
        
        List u1Alt = uploader1.incomingGoodAltLocs;
        List u2Alt = uploader2.incomingGoodAltLocs;
                    
        // neither uploader knows any alt locs.
        assertTrue(u1Alt.isEmpty());
        assertTrue(u2Alt.isEmpty());

        // the rate must be absurdly slow for the incomplete file.length()
        // check in HTTPDownloader to be updated.
        final int RATE=50;
        final int STOP_AFTER = (TestFile.length()/2)+1;
        uploader1.setRate(RATE);
        uploader1.stopAfter(STOP_AFTER);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(true);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2,100,TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //both uploaders should know that this downloader is an alt loc.
        u1Alt = uploader1.incomingGoodAltLocs;
        u2Alt = uploader2.incomingGoodAltLocs;
        assertFalse(u1Alt.isEmpty());
        assertFalse(u2Alt.isEmpty());

        AlternateLocation al = AlternateLocation.create(TestFile.hash());
        assertTrue(u1Alt.toString()+" should contain "+al, u1Alt.contains(al) );
        assertTrue(u2Alt.toString()+" should contain "+al,  u2Alt.contains(al) );        

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertLessThanOrEquals("u1 did too much work", STOP_AFTER, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testNotAddSelfToMeshIfNoTree() throws Exception {
        // change the minimum required bytes so it'll be added.
        PrivilegedAccessor.setValue(HTTPDownloader.class,
            "MIN_PARTIAL_FILE_BYTES", new Integer(1) );
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
            "_acceptedIncoming", Boolean.TRUE );
            
        LOG.info("-Testing that downloader does not add itself to the mesh if it has no tree");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
        
        List u1Alt = uploader1.incomingGoodAltLocs;
        List u2Alt = uploader2.incomingGoodAltLocs;
                    
        // neither uploader knows any alt locs.
        assertTrue(u1Alt.isEmpty());
        assertTrue(u2Alt.isEmpty());

        // the rate must be absurdly slow for the incomplete file.length()
        // check in HTTPDownloader to be updated.
        final int RATE=50;
        final int STOP_AFTER = TestFile.length()/2;
        uploader1.setRate(RATE);
        uploader1.stopAfter(STOP_AFTER);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2,100,TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //both uploaders should know that the other uploader is an alt loc.
        u1Alt = uploader1.incomingGoodAltLocs;
        u2Alt = uploader2.incomingGoodAltLocs;
        assertEquals(1,u1Alt.size());
        assertEquals(1,u2Alt.size());
        assertTrue(u1Alt.contains(AlternateLocation.create(rfd2)));
        assertTrue(u2Alt.contains(AlternateLocation.create(rfd1)));

        // but should not know about me.
        AlternateLocation al = AlternateLocation.create(TestFile.hash());
        assertFalse( u1Alt.contains(al) );
        assertFalse( u2Alt.contains(al) );        

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertLessThanOrEquals("u1 did too much work", STOP_AFTER, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testPartialAddsAltsActiveDownload() throws Exception {
        altBootstrapTest(false);
    }
    
    public void testPartialBootstrapsInactiveDownload() throws Exception {
        // this is different from testResumePartialWithAlternateLocations where the
        // download is resumed manuall
        altBootstrapTest(true);
    }
    
    private void altBootstrapTest(final boolean complete) throws Exception {
        LOG.info("-Testing a shared partial funnels alt locs to downloader");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
            
        final int RATE=200;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = TestFile.length()/10;
        uploader1.setRate(RATE);
        uploader1.stopAfter(STOP_AFTER);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        uploader3.setRate(RATE);
        final RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        final RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        final RemoteFileDesc rfd3=newRFDWithURN(PORT_3, 100, TestFile.hash().toString());
        
        //Start with only RFD1.
        RemoteFileDesc[] rfds = {rfd1};
        
        // Add RFD2 and 3 to the IncompleteFileDesc, make sure we use them.
        Thread locAdder = new Thread( new Runnable() {
            public void run() {
                try {
                    Thread.sleep(complete ? 4000 : 1500);
                    FileDesc fd = RouterService.getFileManager().
                        getFileDescForUrn(TestFile.hash());
                    assertTrue(fd instanceof IncompleteFileDesc);
                    RouterService.getAltlocManager().add(
                        AlternateLocation.create(rfd2),this);
                    RouterService.getAltlocManager().add(
                        AlternateLocation.create(rfd3),this);
                } catch(Throwable e) {
                    ErrorService.error(e);
                }
            }
       });
       locAdder.start();
        
        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.getAmountUploaded();
        int u2 = uploader2.getAmountUploaded();
        int u3 = uploader3.getAmountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tu3: "+u3+"\n");
        LOG.debug("\tTotal: "+(u1+u2+u3)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertEquals("u1 did too much work", STOP_AFTER, u1);
        assertEquals("u2 did too much work", STOP_AFTER, u2);
        assertGreaterThan("u3 did no work", 0, u3);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testResumeFromPartialWithAlternateLocations() throws Exception {
        LOG.info("-Testing alt locs from partial bootstrap resumed download");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
            
        final int RATE=200;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = TestFile.length()/10;
        uploader1.setRate(RATE);
        uploader1.stopAfter(STOP_AFTER);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        uploader3.setRate(RATE);
        final RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        final RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        final RemoteFileDesc rfd3=newRFDWithURN(PORT_3, 100, TestFile.hash().toString());
        AlternateLocation al1 = AlternateLocation.create(rfd1);
        AlternateLocation al2 = AlternateLocation.create(rfd2);
        AlternateLocation al3 = AlternateLocation.create(rfd3);
        
        IncompleteFileManager ifm = dm.getIncompleteFileManager();
        // put the hash for this into IFM.
        File incFile = ifm.getFile(rfd1);
        incFile.createNewFile();
        // add the entry, so it's added to FileManager.
        ifm.addEntry(incFile, new VerifyingFile(TestFile.length()));
        
        // Get the IncompleteFileDesc and add these alt locs to it.
        FileDesc fd =
            RouterService.getFileManager().getFileDescForUrn(TestFile.hash());
        assertNotNull(fd);
        assertInstanceof(IncompleteFileDesc.class, fd);
        RouterService.getAltlocManager().add(al1, null);
        RouterService.getAltlocManager().add(al2, null);
        RouterService.getAltlocManager().add(al3, null);
        
        tResume(incFile);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.getAmountUploaded();
        int u2 = uploader2.getAmountUploaded();
        int u3 = uploader3.getAmountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tu3: "+u3+"\n");
        LOG.debug("\tTotal: "+(u1+u2+u3)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertEquals("u1 did wrong work", STOP_AFTER, u1);
        assertEquals("u2 did wrong work", STOP_AFTER, u2);
        assertGreaterThan("u3 did no work", 0, u3);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    } 

    public void testQueuedDownloader() throws Exception {
        LOG.info("-Testing queued downloader. \n");
        
        uploader1.setQueue(true);
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        //the queued downloader will resend the query after sleeping,
        //and then it shold complete the download, because TestUploader
        //resets queue after sending 503
        tGeneric(rfds);
    }
    
    public void testBusyHostIsUsed() throws Exception {
        LOG.info("-Testing a once-busy host is reused.");
        
        //Throttle rate to give opportunities for swarming.
        final int SLOW_RATE=5;
        final int FAST_RATE=100;
        uploader1.setBusy(true);
        uploader1.setTimesBusy(1);
        uploader1.setRate(FAST_RATE);
        uploader2.setRate(SLOW_RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1}; // see note below about why only rfd1
        
        // Interesting odd factoid about the test:
        // Whether or not RFD1 or RFD2 is tried first is a BIG DEAL.
        // This test is making sure that RFD1 is reused even though
        // RFD2 is actively downloading.  However, because ManagedDownloader
        // sets the RetryAfter time differently depending on if
        // someone is already downloading (and this test will fail
        // if it sets the time to be the longer 10 minute wait),
        // we must ensure that RFD1 is tried first, so the wait
        // is only set to 1 minute.
        
        ManagedDownloader download= (ManagedDownloader) RouterService.download(rfds, Collections.EMPTY_LIST, null, false);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+1000);
        download.addDownload(rfd2,true);
        
        waitForComplete();
        int u1 = uploader1.getAmountUploaded();
        int u2 = uploader2.getAmountUploaded();
        
        LOG.debug("u1: " + u1);
        LOG.debug("u2: " + u2);
        
        assertGreaterThan("u1 did no work", 0, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        
        // This should ideally be an equals ( not >= ) but timing
        // conditions can cause assignGrey to fail too early,
        // causing more connection attempts.
        assertGreaterThanOrEquals("wrong connection attempts",
            2, uploader1.getConnections());
        assertEquals("wrong connection attempts",
            1, uploader2.getConnections());
    }

    /**
     * Test to make sure that we read the alternate locations from the
     * uploader response headers even if the response code is a 503,
     * try again later.
     */
    public void testAlternateLocationsExchangedWithBusy() throws Exception {
        //tests that a downloader reads alternate locations from the
        //uploader even if it receives a 503 from the uploader.
        LOG.info("-Testing dloader gets alt from 503 uploader...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setBusy(true);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.create(rfd1.getSHA1Urn());

        AlternateLocation al2 =
			AlternateLocation.create(rfd2);
        ualt.add(al2);

        uploader1.setGoodAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertEquals("u1 did too much work", 0, u1);
        assertLessThan("u2 did all the work", TestFile.length()+FUDGE_FACTOR, u2);
    }
    
    public void testSimpleDownloadWithInitialAlts() throws Exception {
        LOG.info("-Testing download with initial alts");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;

        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds1 = {rfd1};
        List rfds2 = new LinkedList();
        rfds2.add(rfd2);
        
        tGeneric(rfds1, rfds2);
        
        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }    
    

    /**
     * Tests that if the downloader has two sources, adding a third does not
     * cause it to drop either of the others -- important to test since we have
     * added logic that tries to knock off queued download and replace with good
     * downloaders
     */
    public void testFullSwarmDownloadsNotDropped() throws Exception {
        LOG.info("-testing that a good source does not dislodge other good ones"+
              " when swarming at capacity");
       int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 30;
        uploader1.setRate(RATE);
        uploader3.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1, rfd2};
        
        RemoteFileDesc rfd3 = newRFDWithURN(PORT_3, 100);
        
        ManagedDownloader downloader = null;        
        downloader=(ManagedDownloader)RouterService.download(rfds, false, null);
        Thread.sleep(2 * DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        int swarm = downloader.getActiveWorkers().size();
        int queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",0, queued);

        //try to add a third
        downloader.addDownloadForced(rfd3, true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        
        //make sure we did not kill anybody
        swarm = downloader.getActiveWorkers().size();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not replaced ",0, queued);

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");
        int u1 = uploader1.getAmountUploaded();
        int u2 = uploader2.getAmountUploaded();
        int u3 = uploader3.getAmountUploaded();

        // we only care that the 3rd downloader doesn't download anything -
        // how the other two downloaders split the file between themselves 
        // doesn't matter.
        assertGreaterThan("u1 did not do any work",0,u1);
        assertGreaterThan("u2 did not do any work",0,u2);
        assertGreaterThanOrEquals("u3 did some work",TestFile.length(),u1+u2);
        assertEquals("u3 replaced a good downloader",0,u3);

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }

    /**
     * Tests that an uploader offering the file, replaces a queued uploader
     * when even at swarm capacity
     */
    public void testDownloadAtCapacityReplaceQueued() throws Exception {
        LOG.info("-testing that if max threads are queued or downloading, and a "+
              "good location comes along, the queued downloader is dislodged");
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 30;
        uploader1.setRate(RATE);
        uploader3.setRate(RATE);
        uploader2.setRate(RATE);
        uploader2.setQueue(true);
        uploader2.unqueue = false; //never unqueue this uploader.
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};//one good and one queued
        
        RemoteFileDesc rfd3 = newRFDWithURN(PORT_3, 100);
        
        ManagedDownloader downloader = null;
        
        downloader=(ManagedDownloader)RouterService.download(rfds, false, null);
        //Thread.sleep(1000);
        //downloader.addDownloadForced(rfd2,false);
        Thread.sleep(2 * DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        LOG.debug("about to check swarming");
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1, queued);

        downloader.addDownload(rfd3, true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        
        //make sure we killed the queued
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not replaced ",0, queued);

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");
        
        int u1 = uploader1.getAmountUploaded();
        int u2 = uploader2.getAmountUploaded();
        int u3 = uploader3.getAmountUploaded();
        
        assertEquals("queued uploader uploaded",0,u2);
        assertGreaterThan("u3 not given a chance to run", 0, u3);
        assertLessThan("u1 did all the work",TestFile.length(),u1);  
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }

    /**
     * Tests that when we have max download threads, and there is a queued
     * downloader, it does not get replaced by another queued downloader with a
     * worse position, but does get replaced by a queued downloader that has a
     * better position
     */
    public void testDownloadAtCapacityGetsBetterQueued() throws Exception {
        LOG.info("-testing that if max threads are queued or downloading, and a "+
              "queued downloader gets by a queued downloader only if the new "+
              "one has a better queue position");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 50;
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        uploader2.setQueue(true);
        uploader2.unqueue = false; //never unqueue this uploader.
        uploader2.queuePos=3;

        uploader3.setRate(RATE);
        uploader3.setQueue(true);
        uploader3.unqueue = false; //never unqueue this uploader.
        uploader3.queuePos=5;

        uploader4.setRate(RATE);
        uploader4.setQueue(true);
        uploader4.unqueue = false; //never unqueue this uploader.
        uploader4.queuePos=1;

        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc rfd3=newRFDWithURN(PORT_3, 100);
        RemoteFileDesc rfd4=newRFDWithURN(PORT_4, 100);
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        ManagedDownloader downloader = null;
        downloader = (ManagedDownloader)RouterService.download(rfds, false, null);
        Thread.sleep(2 * DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        int qPos=downloader.getQueuePosition();
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 3 which is worse, nothing should change
        downloader.addDownload(rfd3,true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1500);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = downloader.getQueuePosition();
        LOG.debug("queued workers: "+downloader.getQueuedWorkers());
        LOG.debug("active workers: "+downloader.getActiveWorkers());
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 4 which is better, we should drop uploader2
        downloader.addDownload(rfd4,true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1500);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = downloader.getQueuePosition();
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 4 not queued ",1,queued);
        assertEquals("incorrect queue pos ",1,qPos);        

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }

    /**
     *  Tests that queued downloads advance on the downloader, this is important
     *  because we use the queue position to decide which downloader to get rid
     *  off when a good uploader shows up
     */
    public void testQueueAdvancementWorks() throws Exception {
        LOG.info("-testing that if queued downloaders advance we downloaders "+
              "register that they did, so that the choice of which downloader"+
              " to replace is made correctly");
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 50;
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        uploader3.setRate(RATE);

        uploader1.setQueue(true);
        uploader1.unqueue = false; //never unqueue this uploader.
        uploader1.queuePos=5;//the worse one
        uploader2.setQueue(true);
        uploader2.unqueue = false; //never unqueue this uploader.
        uploader2.queuePos = 3;//the better one
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc rfd3=newRFDWithURN(PORT_3, 100);
        
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        ManagedDownloader downloader = null;
        downloader = (ManagedDownloader)RouterService.download(rfds,false,null);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()*2 + 1000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("both uploaders should be queued",2,queued);
        
        uploader1.queuePos = 1;//make uploader1 become better
        //wait for the downloader to make the next requests to uploaders.
        Thread.sleep(uploader1.MIN_POLL+2000);

        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("both uploaders should still be queued",2,queued);
        
        downloader.addDownload(rfd3,true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        //now uploader 2 should have been removed.
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        int qPos = downloader.getQueuePosition();
        
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("queued uploader not dropped",1,queued);
        assertEquals("wrong uploader removed",1,qPos);

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }
    
    public void testPartialDownloads() throws IOException {
        LOG.info("-Testing partial downloads...");
        uploader1.setPartial(true);
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        Downloader downloader = RouterService.download(rfds,false,null);
        waitForBusy(downloader);
        assertEquals("Downloader did not go to busy after getting ranges",
                     Downloader.BUSY, downloader.getState());
    }
        
    /**
     * tests that when we receive a headpong claiming that it doesn't have the file,
     * we send out an NAlt for that source
     */
    public void testHeadPongNAlts() throws Exception {
        
        uploader1.setRate(100);
        uploader2.setRate(100);
        
        int sleep = DownloadSettings.WORKER_INTERVAL.getValue();
        
        // make sure we use the ping ranker
        PrivilegedAccessor.setValue(RouterService.getUdpService(),"_acceptedSolicitedIncoming", 
                Boolean.TRUE);
        assertTrue(RouterService.canReceiveSolicited());
        assertTrue(SourceRanker.getAppropriateRanker() instanceof PingRanker);
        
       // create one source that will actually download and another one to which a headping should be sent 
       RemoteFileDesc rfd = newRFDWithURN(PORT_1,100);
       RemoteFileDesc noFile = newRFDWithURN(PORT_2,100);
       
       AlternateLocation toBeDemoted = AlternateLocation.create(noFile);
       
       // create a listener for the headping
       UDPAcceptor l = new UDPAcceptor(PORT_2);
       
       ManagedDownloader download= (ManagedDownloader) 
           RouterService.download(new RemoteFileDesc[]{rfd}, Collections.EMPTY_LIST, null, false);
       LOG.debug("started download");
       
       // after a while clear the ranker and add the second host.
       Thread.sleep((int)(sleep * 1.5));
       SourceRanker.getAppropriateRanker().stop();
       SourceRanker.getAppropriateRanker().setMeshHandler(download);
       download.addDownload(noFile,false);
       
       LOG.debug("waiting for download to complete");
       waitForComplete();
       
       // the first downloader should have received an NAlt
       assertTrue(uploader1.incomingBadAltLocs.contains(toBeDemoted));
       // the first uploader should have uploaded the whole file
       assertGreaterThan(0,uploader1.getConnections());
       assertEquals(TestFile.length(),uploader1.fullRequestsUploaded());
       
       // the second downloader should not be contacted
       assertEquals(0,uploader2.getConnections());
       assertEquals(0,uploader2.getAmountUploaded());
       // only one ping should have been sent to the second uploader
       assertEquals(1,l.pings);
       
       l.interrupt();
    }
    
    /** Tests what happens if the content authority says no. 
     * LEAVE AS LAST -- (it does weird things otherwise) */
    public void testContentInvalid() throws Exception {
        LOG.info("-Testing partial downloads...");
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        uploader1.setRate(50);
        
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);        
        RouterService.download(rfds,false,null);
        Thread.sleep(1000);
        synchronized(COMPLETE_LOCK) {
        	RouterService.getMessageRouter().handleUDPMessage(new ContentResponse(TestFile.hash(), Authorization.UNAUTHORIZED, "False"), addr);
        	waitForInvalid();       
        }
    }

    /*
    private static void tGUI() {
        final int RATE=500;
        uploader1.setCorruption(true);
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);

        //Bring up application.  Make sure disconnected.
        debug("Bringing up GUI..."+"\n");
        com.limegroup.gnutella.gui.Main.main(new String[0]);
        RouterService router=GUIMediator.instance().getRouter();
        router.disconnect();
        
        //Do dummy search
        byte[] guid=GUIMediator.instance().triggerSearch(file.getName());
        assertNotNull("Search didn't happen", guid);

        //Add normal dummy result
        ActivityCallback callback=router.getCallback();        
        byte[] localhost={(byte)127, (byte)0, (byte)0, (byte)1};
        Response[] responses=new Response[1];
        responses[0]=new Response(0l, file.length(), file.getName());
        QueryReply qr=new QueryReply(guid, (byte)5, PORT_1,
                                     localhost, Integer.MAX_VALUE,
                                     responses, new byte[16]);
        responses=new Response[1];
        responses[0]=new Response(0l, file.length(), file.getName());
        qr=new QueryReply(guid, (byte)5, PORT_2,
                          localhost, Integer.MAX_VALUE,
                          responses, new byte[16]);

		
        //callback.handleQueryReply(qr);
		RouterService.getSearchResultHandler().handleQueryReply(qr);
    }
    */


    ////////////////////////// Helping Code ///////////////////////////
    private static void tGeneric(RemoteFileDesc[] rfds) throws Exception {
        tGeneric(rfds, new RemoteFileDesc[0]);
    }
    
    private static void tGeneric(RemoteFileDesc[] rfds, List alts)
      throws Exception {
        tGeneric(rfds, null, alts);
    }
    
    private static void tGeneric(RemoteFileDesc[] now, RemoteFileDesc[] later)
      throws Exception {
        tGeneric(now, later, Collections.EMPTY_LIST);
    }
    
    /**
     * Performs a generic download of the file specified in <tt>rfds</tt>.
     */
    private static void tGeneric(RemoteFileDesc[] rfds, RemoteFileDesc[] later, 
      List alts) throws Exception {
        Downloader download=null;

        download=RouterService.download(rfds, alts, null, false);
        if(later != null) {
            Thread.sleep(100);
            for(int i = 0; i < later.length; i++)
                ((ManagedDownloader)download).addDownload(later[i], true);
        }

        waitForComplete();
        if (isComplete())
            LOG.debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        IncompleteFileManager ifm=dm.getIncompleteFileManager();
        for (int i=0; i<rfds.length; i++) {
            File incomplete=ifm.getFile(rfds[i]);
            VerifyingFile vf=ifm.getEntry(incomplete);
            assertNull("verifying file should be null", vf);
        }
    }
    
    /**
     * Performs a generic download of the file specified in <tt>rfds</tt>.
     */
    private static void tGenericCorrupt(RemoteFileDesc[] rfds,
                                        RemoteFileDesc[] later)
                                        throws Exception {
        Downloader download=null;

        download=RouterService.download(rfds, false, null);
        if(later != null) {
            Thread.sleep(100);
            for(int i = 0; i < later.length; i++)
                ((ManagedDownloader)download).addDownload(later[i], true);
        }

        waitForCorrupt();
        if (isComplete())
            fail("should be corrupt");
        else
            LOG.debug("pass");
        
        IncompleteFileManager ifm=dm.getIncompleteFileManager();
        for (int i=0; i<rfds.length; i++) {
            File incomplete=ifm.getFile(rfds[i]);
            VerifyingFile vf=ifm.getEntry(incomplete);
            assertNull("verifying file should be null", vf);
        }
    }
    
    /**
     * Performs a generic resume download test.
     */
    private static void tResume(File incFile) throws Exception {
         RouterService.download(incFile);
        
        waitForComplete();
        if (isComplete())
            LOG.debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        IncompleteFileManager ifm=dm.getIncompleteFileManager();
        VerifyingFile vf = ifm.getEntry(incFile);
        assertNull("verifying file should be null", vf);
    }

    private static RemoteFileDesc newRFD(int port, int speed) {
        return new RemoteFileDesc("127.0.0.1", port,
                                  0, savedFile.getName(),
                                  TestFile.length(), new byte[16],
                                  speed, false, 4, false, null, null,
                                  false,false,"",null, -1);
    }

	private static RemoteFileDesc newRFDWithURN(int port, int speed) {
		return newRFDWithURN(port, speed, null);
	}

    private static RemoteFileDesc newRFDWithURN(int port, int speed, 
                                                String urn) {
        Set set = new HashSet();
        try {
            // for convenience, don't require that they pass the urn.
            // assume a null one is the TestFile's hash.
            if (urn == null)
                set.add(TestFile.hash());
            else
                set.add(URN.createSHA1Urn(urn));
        } catch(Exception e) {
            fail("SHA1 not created for: "+savedFile, e);
        }
        return new RemoteFileDesc("127.0.0.1", port,
                                  0, savedFile.getName(),
                                  TestFile.length(), new byte[16],
                                  speed, false, 4, false, null, set,
                                  false, false,"",null, -1);
    }
    
    private static RemoteFileDesc newRFDPush(int port,int suffix) throws Exception{
        return newRFDPush(port, suffix, 1);
    }
    
    private static RemoteFileDesc newRFDPush(int port, int rfdSuffix, int proxySuffix) throws Exception{    
        PushAltLoc al = (PushAltLoc)AlternateLocation.create(
                guid.toHexString()+";127.0.0." + proxySuffix +":"+port,TestFile.hash());
        al.updateProxies(true);
        
        Set urns = new HashSet();
        urns.add(TestFile.hash());
        
        return new RemoteFileDesc("127.0.0."+rfdSuffix, 6346, 0, savedFile.getName(),
                TestFile.length(),100,false,1,false, 
                null,urns,false,
                true,"ALT",0,al.getPushAddress());
    }

    /** Returns true if the complete file exists and is complete */
    private static boolean isComplete() {LOG.debug("file is "+savedFile.getPath());
        if ( savedFile.length() < TestFile.length() ) {
            LOG.debug("File too small by: " + (TestFile.length() - savedFile.length()) );
            return false;
        } else if ( savedFile.length() > TestFile.length() ) {
            LOG.debug("File too large by: " + (savedFile.length() - TestFile.length()) );
            return false;
        }
        FileInputStream stream=null;
        try {
            stream = new FileInputStream(savedFile);
            for (int i=0 ; ; i++) {
                int c=stream.read();
                if (c==-1)//eof
                    break;
                if ((byte)c!=TestFile.getByte(i)) {
                    LOG.debug("Bad byte at "+i+"\n");
                    return false;
                }
            }
        } catch (IOException ioe) {
            //ioe.printStackTrace();
            return false;
        } finally {
            if (stream!=null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return true;
    }
    
    private static final int CORRUPT = 1;
    private static final int COMPLETE = 2;
    private static final int INVALID = 3;
    
    private static void waitForComplete(boolean corrupt) {
        waitForCompleteImpl(corrupt? CORRUPT : COMPLETE);
    }
    
    private static void waitForCorrupt() {
        waitForCompleteImpl(CORRUPT);       
    }
    
    private static void waitForInvalid() {
        waitForCompleteImpl(INVALID);
    }
    
    private static void waitForComplete() {
        waitForCompleteImpl(COMPLETE);
    }
    
    private static void waitForCompleteImpl(int state) {
        synchronized(COMPLETE_LOCK) {
            try {
                REMOVED = false;
                LOG.debug("starting wait");
                COMPLETE_LOCK.wait(DOWNLOAD_WAIT_TIME);
                LOG.debug("finished waiting");
            } catch (InterruptedException e) {
                LOG.debug("interrupted",e);
                //good.
            }
        }
        
        if( !REMOVED ) {
            dm.remove(DOWNLOADER, false);
            fail("download did not finish, last state was: " +
                 DOWNLOADER.getState());
        }
        
        if ( state == CORRUPT )
            assertEquals("unexpected state", Downloader.CORRUPT_FILE, DOWNLOADER.getState());
        else if(state == INVALID)
            assertEquals("unexpected state", Downloader.INVALID, DOWNLOADER.getState());
        else if(state == COMPLETE)
            assertEquals("unexpected state", Downloader.COMPLETE, DOWNLOADER.getState());
        else
            fail("bad expectation: " + state);
    }        
    
    private static void waitForBusy(Downloader downloader) {
        for(int i=0; i< 12; i++) { //wait 12 seconds
            if(downloader.getState() == Downloader.BUSY)
                return;
            try {
                Thread.sleep(1000);// try again after a second
            } catch(InterruptedException e) {
                fail("downloader unexpecteted interrupted", e);
                return;
            }
        }
        return;
    }
    
    private static final class MyCallback extends ActivityCallbackStub {
        
        public void addDownload(Downloader d) {
            DOWNLOADER = (ManagedDownloader)d;
        }
        public void removeDownload(Downloader d) {
            synchronized(COMPLETE_LOCK) {
                REMOVED = true;
                COMPLETE_LOCK.notify();
            }
            
            if (saveAltLocs) {
                try {
                    validAlts = new HashSet();
                    validAlts.addAll((Set)PrivilegedAccessor.getValue(d, "validAlts"));
                    
                    invalidAlts = new HashSet();
                    invalidAlts.addAll((Set)PrivilegedAccessor.getValue(d, "invalidAlts"));
                } catch (Exception err) {
                    throw new RuntimeException(err);
                }
            }
        }
    }
    
    private static class UDPAcceptor extends ManagedThread {
        private int _portC;
        private DatagramSocket sock;
        private String _fileName;
        private TestUploader _uploader;
        private GUID _g;
        public boolean sentGIV;
        private boolean noFile;
        public int pings;
        private String startedInTest;

        public UDPAcceptor(int port) {
            startedInTest = "began in test: " + _currentTestName;
            noFile = true;
            try {
                sock = new DatagramSocket(port);
                // sock.connect(InetAddress.getLocalHost(),portC);
                sock.setSoTimeout(15000);
            } catch (IOException bad) {
                ErrorService.error(bad, startedInTest);
            }
            start();
        }
        
        public UDPAcceptor(int portL,int portC,String filename,TestUploader uploader,GUID g) {
            super("push acceptor " + portL + "->" + portC);

            _portC = portC;
            _fileName = filename;
            _uploader = uploader;
            _g = g;
            try {
                sock = new DatagramSocket(portL);
                // sock.connect(InetAddress.getLocalHost(),portC);
                sock.setSoTimeout(15000);
            } catch (IOException bad) {
                ErrorService.error(bad, startedInTest);
            }
            setPriority(Thread.MAX_PRIORITY);
            start();
        }

        public void managedRun() {
            DatagramPacket p = new DatagramPacket(new byte[1024],1024);
            Message m = null;
            try {
                LOG.debug("listening for push request on "+sock.getLocalPort());
                while(true) {
                    sock.receive(p);
                    ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());            
                    m = MessageFactory.read(bais);
                    LOG.debug("received "+m.getClass()+ " no file? "+noFile);
                    if (noFile) {
                        if (m instanceof HeadPing) 
                            handleNoFile(p.getSocketAddress(),new GUID(m.getGUID()));
                        continue;
                    }
                    else if (m instanceof HeadPing)
                        continue;
                    else
                        break;
                }
                
                assertTrue(m instanceof PushRequest);
                
                LOG.debug("received a push request");
                
                Socket s = Sockets.connect("127.0.0.1",_portC,500);
                
                OutputStream os = s.getOutputStream();
                
                String GIV = "GIV 0:"+_g.toHexString()+"/"+_fileName+"\n\n";
                os.write(GIV.getBytes());
                
                os.flush();
                
                LOG.debug("wrote GIV");
                sentGIV = true;
                _uploader.setSocket(s);

            } catch (BadPacketException bad) {
                ErrorService.error(bad, startedInTest);
            } catch (IOException bad) {
                ErrorService.error(bad, startedInTest);
            } finally {
                sock.close();
            }
        }
        
        private void handleNoFile(SocketAddress from,GUID g) {
            HeadPing ping = new HeadPing(g,HugeTestUtils.SHA1,0);
            HeadPong pong = new HeadPong(ping);
            assertFalse(pong.hasFile());
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                pong.write(baos);
                DatagramPacket pack = 
                    new DatagramPacket(baos.toByteArray(),baos.toByteArray().length,from);
                sock.send(pack);
                pings++;
            } catch (IOException e) {
                ErrorService.error(e, startedInTest);
            }
            
            LOG.debug("sent a NoFile headPong to "+from);
        }
    }
}
