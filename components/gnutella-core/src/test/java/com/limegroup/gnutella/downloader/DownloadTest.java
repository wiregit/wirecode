package com.limegroup.gnutella.downloader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
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
import com.limegroup.gnutella.FileManager;
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
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PushRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
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
    private static final int PORT_1 = 6320;

    /**
     * Port for the second uploader.
     */
    private static final int PORT_2 = 6321;

    /**
     * Port for the third uploader.
     */
    private static final int PORT_3 = 6322;


    /**
     * Port for the fourth uploader.
     */
    private static final int PORT_4 = 6323;

    /**
     * Port for the fifth uploader.
     */
    private static final int PORT_5 = 6324;
    
    
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
    
    public static void globalSetUp() throws Exception {
        // raise the download-bytes-per-sec so stealing is easier
        DownloadSettings.MAX_DOWNLOAD_BYTES_PER_SEC.setValue(10);
		RouterService rs = new RouterService(callback);
        dm = rs.getDownloadManager();
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
    
    public void setUp() {
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
        
        try {
            Map m = (Map)PrivilegedAccessor.getValue(PushEndpoint.class,"GUID_PROXY_MAP");
            m.clear();
        }catch(Exception e){
            ErrorService.error(e);
        }
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
        LOG.debug("-Testing non-swarmed download...");
        
        RemoteFileDesc rfd=newRFD(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd};
        tGeneric(rfds);
    }
    
    public void testSimpleDownload11() throws Exception {
        LOG.debug("-Testing non-swarmed download...");
        
        RemoteFileDesc rfd=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd};
        tGeneric(rfds);
    }
    
    /**
     * tests http11 downloads and the gray area allocation.
     */
    public void testTHEXDownload11() throws Exception {
        LOG.debug("-Testing chunk allocation in a thex download...");
        
        
        final RemoteFileDesc rfd=newRFDWithURN(PORT_1, 100);
        final IncompleteFileManager ifm=dm.getIncompleteFileManager();
        RemoteFileDesc[] rfds = {rfd};
        
        HTTP11Listener grayVerifier = new HTTP11Listener() {
        	private int requestNo;
        	public void requestHandled(){}
        	public void thexRequestStarted() {}
            public void thexRequestHandled() {}
        	
        	// checks whether we request chunks at the proper offset, etc.
        	public void requestStarted(TestUploader uploader) {
        		
        		Interval i = null;
        		try {
        			IntervalSet leased = null;
        			File incomplete = null;
        			incomplete = ifm.getFile(rfd);
        			assertNotNull(incomplete);
        			VerifyingFile vf = ifm.getEntry(incomplete);
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
        		
        		switch(requestNo) {
        			case 0: 
        				// first request, we should have 0-99999 gray
        				assertEquals(0,i.low);
        				assertEquals(99999,i.high);
        				break;
        			case 1:
        				// on the second request we have 100K-256K
        				assertEquals(100000,i.low);
        				assertEquals(256*1024 -1,i.high);
        				break;
        			case 2:
        				// 256K-512K
        				assertEquals(256*1024,i.low);
        				assertEquals(512*1024 -1, i.high);
        				break;
        			case 3:
        				// 512K-768K
        				assertEquals(512*1024,i.low);
        				assertEquals(768*1024 -1, i.high);
        				break;
        			case 4:
        				// 768K-1,000,000
        				assertEquals(768*1024,i.low);
        				assertEquals(999999, i.high);
        				break;
        		}
        		requestNo++;
        		
        	}
        };
        
        uploader1.setHTTPListener(grayVerifier);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(true);
        
        TigerTreeCache.instance().purgeTree(rfd.getSHA1Urn());
        Downloader download=RouterService.download(rfds, Collections.EMPTY_LIST, false, null);
        
        waitForComplete(false);
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
    }
    
    public void testTHEXDownloadSwarm() throws Exception {
    	LOG.debug("-Testing the grey area allocation during swarm downloads");
    	
    	final RemoteFileDesc rfd=newRFDWithURN(PORT_1, 100);
    	final RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        final IncompleteFileManager ifm=dm.getIncompleteFileManager();
        RemoteFileDesc[] rfds = {rfd,rfd2};
        
        HTTP11Listener grayVerifier = new HTTP11Listener() {
        	private int requestNo;
        	private int thexStatus;
            private int thexRequestIndex = -1 ;
        	private Interval lastInterval;
        	public synchronized void requestHandled(){
        	    LOG.debug("handled request");
        	}
            
            public synchronized void thexRequestHandled() {
                assertGreaterThan(0,thexStatus);
                if (thexStatus == 1) {
                    thexStatus = 2;
                    thexRequestIndex = requestNo+1;
                    LOG.debug("ending thex request at request no "+requestNo);
                }
            }
            
        	public synchronized void thexRequestStarted() {
        	    LOG.debug("starting thex request at request no "+requestNo);
        	    thexStatus=1;
        	}
        	
        	// checks whether we request chunks at the proper offset, etc.
        	public synchronized void requestStarted(TestUploader uploader) {
        	    LOG.debug("started request "+uploader.start+"-"+uploader.stop);
        		
        		Interval i = new Interval (uploader.start,uploader.stop-1);
                if (requestNo == 0) {
                    assertEquals(0,thexStatus);
                    // first request, we should have 0-99999 gray
                    assertEquals(0,i.low);
                    assertEquals(99999,i.high);
                    LOG.debug("request 0 pass");
                }
                else if (requestNo == 1) {
                    
                    // if no thex yet, so 100K-200K        				
                    if (thexStatus == 0)  
                        assertEquals(199999,i.high);
                    
                    // else 100K-256K        				
                    if (thexStatus == 2 && thexRequestIndex == 1)
                        assertEquals(256*1024 -1,i.high);
                }
                else if (requestNo < 5){
                    assertEquals(lastInterval.high+1,i.low);
                    
                    if (thexStatus != 2) 
                        assertEquals(100000,i.high - i.low +1);
                     else if (thexRequestIndex >= requestNo) 
                        assertTrue((0 == (i.high+1) % (256*1024)) ||
                                i.high+1==TestFile.length());
                     
                }
                lastInterval = i;
        		requestNo++;
        		
        	}
        };
        
        uploader1.setHTTPListener(grayVerifier);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(true);
        uploader2.setHTTPListener(grayVerifier);
        uploader2.setSendThexTreeHeader(true);
        uploader2.setSendThexTree(true);
        
        Downloader download=null;

        download=RouterService.download(rfds, Collections.EMPTY_LIST, false, null);

        waitForComplete(false);
        //assertEquals(6,uploader1.getRequestsReceived());
        System.out.println("u1: "+uploader1.getRequestsReceived());
        System.out.println("u2: "+uploader2.getRequestsReceived());
        if (isComplete())
            LOG.debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        
        for (int i=0; i<rfds.length; i++) {
            File incomplete=ifm.getFile(rfds[i]);
            VerifyingFile vf=ifm.getEntry(incomplete);
            assertNull("verifying file should be null", vf);
        }
        
    }
    
    public void testSimplePushDownload() throws Exception {
        LOG.debug("-Testing non-swarmed push download");
        
        AlternateLocation pushLoc = AlternateLocation.create(
                guid.toHexString()+";127.0.0.1:"+PPORT_1,TestFile.hash());
       ((PushAltLoc)pushLoc).updateProxies(true);

        RemoteFileDesc rfd = newRFDPush(PPORT_1,1);
        
        assertTrue(rfd.needsPush());
        
        RemoteFileDesc [] rfds = {rfd};
        TestUploader uploader = new TestUploader("push uploader");
        PushAcceptor p = new PushAcceptor(PPORT_1,RouterService.getPort(),
                savedFile.getName(),uploader,guid);
        
        tGeneric(rfds);
    }
    
    public void testSimpleSwarm() throws Exception {
        LOG.debug("-Testing swarming from two sources...");
        
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
        LOG.debug("-Testing swarming from two sources, one push...");  
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        AlternateLocation pushLoc = AlternateLocation.create(
                guid.toHexString()+";127.0.0.1:"+PPORT_2,TestFile.hash());
       ((PushAltLoc)pushLoc).updateProxies(true);
        RemoteFileDesc rfd2 = pushLoc.createRemoteFileDesc(TestFile.length());
        
        uploader1.setRate(500);
        TestUploader uploader = new TestUploader("push uploader");
        final int FUDGE_FACTOR=500*1024;
        
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        PushAcceptor pa = 
            new PushAcceptor(PPORT_2,RouterService.getPort(),savedFile.getName(),uploader,guid);
        
        tGeneric(rfds);
        
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, 
                uploader1.fullRequestsUploaded());
        
        assertGreaterThan("pusher did all the work ",0,uploader1.fullRequestsUploaded());
    }

    public void testUnbalancedSwarm() throws Exception  {
        LOG.debug("-Testing swarming from two unbalanced sources...");
        
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
        LOG.debug("-Testing swarming from two sources (one broken)...");
        
        final int RATE=100;
        final int STOP_AFTER = TestFile.length()/4;       
        final int FUDGE_FACTOR=RATE*1024;  
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
        assertLessThan("u1 did all the work", TestFile.length()-STOP_AFTER+FUDGE_FACTOR, u1);
        assertEquals("u2 did all the work", STOP_AFTER, u2);
    }
    
    /**
     * tests a swarm from a 1.0 and 1.1 source - designed to test stealing.
     */
    public void testSwarmWithTheft() throws Exception {
        LOG.debug("-Testing swarming from two sources, one 1.0 and one 1.1");
        
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

    public void testAddDownload() throws Exception {
        LOG.debug("-Testing addDownload (increases swarming)...");
        
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

        waitForComplete(false);
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
        LOG.debug("-Testing download completion with stalling downloader...");
        
        //Throttle rate at 500KB/s to give opportunities for swarming.
        final int RATE=500;
        uploader1.setRate(0.1f);//stalling uploader
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

        LOG.debug("passed"+"\n");//file downloaded? passed
    }
    
    public void testAcceptableSpeedStallIsReplaced() throws Exception {
        LOG.debug("-Testing a download that is an acceptable speed but slower" +
                  " is replaced by another download that is faster");
        
        final int SLOW_RATE = 5;
        final int FAST_RATE = 100;
        uploader1.setRate(SLOW_RATE);
        uploader2.setRate(FAST_RATE);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);
        
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
        LOG.debug("-Testing that a download can handle an uploader giving low+higher ranges");
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
        
        waitForComplete(false);
        assertGreaterThanOrEquals(50,uploader5.fullRequestsUploaded());
        assertGreaterThanOrEquals(100000-50,uploader1.fullRequestsUploaded());
    }
    
    public void testUploaderLowLowerRange() throws Exception {
        LOG.debug("-Testing that a download can handle an uploader giving low+lower ranges");
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
        waitForComplete(false);
        
    }
    
    public void testUploaderHighHigherRange() throws Exception {
        LOG.debug("-Testing that a download can handle an uploader giving high+higher ranges");
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
        waitForComplete(false);
    }
    
    public void testUploaderHighLowerRange() throws Exception {
        LOG.debug("-Testing that a download can handle an uploader giving high+lower ranges");
        uploader1.setRate(25);
        uploader1.setHighChunkOffset(-10);
        uploader5.setRate(100); // control, to finish the test.
        RemoteFileDesc rfd5 = newRFDWithURN(PORT_5, 100);
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        ManagedDownloader md = (ManagedDownloader)
            RouterService.download(rfds,true,null);
        
        Thread.sleep(5000);
        // at this point we should stall since we'll never get our 50 bytes
        md.addDownloadForced(rfd5,false);
        
        waitForComplete(false);
        assertGreaterThanOrEquals(50,uploader5.fullRequestsUploaded());
        assertGreaterThanOrEquals(100000-50,uploader1.fullRequestsUploaded());
    }
       
    public void testReuseHostWithBadTree() throws Exception {
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
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(false);
        uploader1.setSendContentLength(false);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        TigerTreeCache.instance().purgeTree(TestFile.hash());
        
        // the tree will fail, but it'll pick up the content-length
        // and discard the rest of the bad data.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = TigerTreeCache.instance().getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(uploader1.thexWasRequested());
        assertEquals(2, uploader1.getConnections());
    }
    
    public void testGetsThex() throws Exception {
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
    
    public void testThexFixesDownload() throws Exception {
        LOG.debug("-Testing that thex can identify a corrupt download range" +
                  " and the downloader will automatically get it back.");
        final int RATE = 100;
        uploader1.setCorruption(true);
        uploader1.setCorruptBoundary(50);
        uploader1.stopAfter(256*1024);
        uploader1.setSendThexTreeHeader(true);
        uploader1.setSendThexTree(true);
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);

        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        
        tGeneric( new RemoteFileDesc[] { rfd1, rfd2 } );
        HashTree tree = TigerTreeCache.instance().getHashTree(TestFile.hash());
        assertNotNull(tree);
        assertEquals(TestFile.tree().getRootHash(), tree.getRootHash());
        assertFalse(callback.corruptChecked);
        
        // tried once or if twice then failed the second time.
        assertLessThanOrEquals(2, uploader1.getConnections());
        // tried once or twice.
        assertLessThanOrEquals(2, uploader2.getConnections());
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
        LOG.debug("-Testing file declared corrupt, when hash of "+
                         "downloaded file mismatches bucket hash" +
                         "stop when corrupt "+ deleteCorrupt+" ");
        String badSha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";

        final int RATE=100;
        uploader1.setRate(RATE);
        uploader1.setSendThexTreeHeader(getThex);
        uploader1.setSendThexTree(getThex);
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1,100, badSha1);
        Downloader download = null;
        
        URN badURN = URN.createSHA1Urn(badSha1);
        TigerTreeCache.instance().purgeTree(TestFile.hash());
        TigerTreeCache.instance().purgeTree(badURN);
        
        download = RouterService.download(new RemoteFileDesc[] {rfd1}, false, 
                                          null);
        // even though the download completed, we ignore the tree 'cause the
        // URNs didn't match.
        assertNull(TigerTreeCache.instance().getHashTree(TestFile.hash()));
        assertNull(TigerTreeCache.instance().getHashTree(badURN));

        waitForComplete(deleteCorrupt);
        assertTrue(callback.corruptChecked);
        assertEquals(getThex, uploader1.thexWasRequested());
        assertEquals(1, uploader1.getConnections());
    }

    public void testDownloaderAddsSmallFilesWithHead() throws Exception {  
        LOG.debug("-Testing AlternateLocation write...");
        Object[] params = new Object[2];
        params[0] = saveDir;
        params[1] = new File(".");
        //add current dir as a save directory so MD sends head request
        PrivilegedAccessor.invokeMethod
        (RouterService.getFileManager(),"updateDirectories",params);
        //make the .out extension shared so FM thinks its shared
        FileManager man = RouterService.getFileManager();
        Set exts = (Set) PrivilegedAccessor.getValue(man,"_extensions");
        exts.add("out");
        
        RemoteFileDesc rfd1=
                           newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1};

        tGeneric(rfds);

        //Prepare to check the alternate locations
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocation dAlt = AlternateLocation.create(rfd1);
           
        URN sha1 = rfd1.getSHA1Urn();
        URN uSHA1 = uploader1.getReportedSHA1();
        
        assertTrue("uploader didn't recieve alt", alt1.hasAlternateLocations());
        assertTrue("downloader didn't add itself to mesh", alt1.contains(dAlt));
        //in the head requester case the uploader will be sent all the locations
        //including itself. The download has no way of knowing it is sending the
        //uploader the same uploaders location
        assertEquals("wrong number of locs ",2, alt1.getAltLocsSize());
        assertNotNull("rfd1 sha1", sha1);
        assertNotNull("uploader1 sha1", uSHA1);
        assertEquals("SHA1 test failed", sha1, uSHA1);
        exts.remove("out");
    }

    public void testTwoAlternateLocations() throws Exception {  
        LOG.debug("-Testing Two AlternateLocations...");
        
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
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection alt2 = uploader2.getAlternateLocations();

        AlternateLocation al1 = AlternateLocation.create(rfd1);
        AlternateLocation al2 = AlternateLocation.create(rfd2);
        
        assertTrue("uploader didn't recieve alt", alt1.hasAlternateLocations());
        assertTrue("uploader didn't recieve alt", alt2.hasAlternateLocations());
        assertTrue("uploader got wrong alt", !alt1.contains(al1));
        assertEquals("incorrect number of locs ",1,alt1.getAltLocsSize());
        assertTrue("uploader got wrong alt", !alt2.contains(al2));
        assertEquals("incorrect number of locs ",1,alt2.getAltLocsSize());
    }

    public void testUploaderAlternateLocations() throws Exception {  
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        LOG.debug("-Testing swarming from two sources one based on alt...");
        
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

        uploader1.setAlternateLocations(ualt);

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
        LOG.debug("-Testing swarming from two sources one based on a push alt...");
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;
        uploader1.setRate(RATE);
        uploader1.stopAfter(800000);
        
        TestUploader pusher = new TestUploader("push uploader");
        pusher.setRate(RATE);
        
        AlternateLocation pushLoc = AlternateLocation.create(
                guid.toHexString()+";127.0.0.1:"+PPORT_1,TestFile.hash());
        ((PushAltLoc)pushLoc).updateProxies(true);
        
        AlternateLocationCollection alCol=AlternateLocationCollection.create(TestFile.hash());
        alCol.add(pushLoc);
        
        uploader1.setAlternateLocations(alCol);
        
        RemoteFileDesc rfd = newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        
        RemoteFileDesc []rfds = {rfd};
        
        PushAcceptor pa = new PushAcceptor(PPORT_1,RouterService.getPort(),
                savedFile.getName(),pusher,guid);
        
        tGeneric(rfds);
        
        assertGreaterThan("u1 didn't do enough work ",100*1024,uploader1.fullRequestsUploaded());
        assertGreaterThan("pusher didn't do enough work ",100*1024,pusher.fullRequestsUploaded());
    }
    
    /**
     * tests that a push uploader passes push loc and the new push loc receives
     * the first uploader as an altloc.
     */
    public void testPushUploaderPassesPushLoc() throws Exception {
        
        final int RATE=500;
        
        TestUploader first = new TestUploader("first pusher");
        first.setRate(RATE/2);
        first.stopAfter(700000);
        
        TestUploader second = new TestUploader("second pusher");
        second.setRate(RATE);
        second.stopAfter(500000);
        second.setInterestedInFalts(true);
        
        GUID guid2 = new GUID(GUID.makeGuid());
        
        AlternateLocation firstLoc = AlternateLocation.create(
                guid.toHexString()+";127.0.0.1:"+PPORT_1,TestFile.hash());
        ((PushAltLoc)firstLoc).updateProxies(true);
        
        AlternateLocation pushLoc = AlternateLocation.create(
                guid2.toHexString()+";127.0.0.1:"+PPORT_2,TestFile.hash());
        ((PushAltLoc)pushLoc).updateProxies(true);
        
        AlternateLocationCollection alCol=AlternateLocationCollection.create(TestFile.hash());
        alCol.add(pushLoc);
        
        first.setAlternateLocations(alCol);
        
        PushAcceptor pa = new PushAcceptor(PPORT_1,RouterService.getPort(),
                savedFile.getName(),first,guid);
        
        PushAcceptor pa2 = new PushAcceptor(PPORT_2,RouterService.getPort(),
                savedFile.getName(),second,guid2);
        
        RemoteFileDesc []rfd ={newRFDPush(PPORT_1,2)};
        
        tGeneric(rfd);
        
        
        assertGreaterThan("first pusher did no work",100000,first.fullRequestsUploaded());
        assertGreaterThan("second pusher did no work",100000,second.fullRequestsUploaded());
        
        assertEquals(1,second.getAlternateLocations().getAltLocsSize());
        
        assertTrue("interested uploader didn't get first loc",
                second.getAlternateLocations().contains(firstLoc));
        
        
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
        LOG.debug("-Testing push download creating a push location...");
        final int RATE=200;
        final int FUDGE_FACTOR=RATE*1536;
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
                guid.toHexString()+";127.0.0.1:"+PPORT_1,TestFile.hash());
        ((PushAltLoc)pushLoc).updateProxies(true);
        
        RemoteFileDesc pushRFD = newRFDPush(PPORT_1,2);
        
        assertFalse(pushRFD.supportsFWTransfer());
        assertTrue(pushRFD.needsPush());

        

        
        RemoteFileDesc openRFD1 = newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        RemoteFileDesc openRFD2 = newRFDWithURN(PORT_2,100,TestFile.hash().toString());
        
        RemoteFileDesc []now={pushRFD};
        RemoteFileDesc []later={openRFD1,openRFD2};
        
        PushAcceptor pa = new PushAcceptor(PPORT_1,RouterService.getPort(),
                savedFile.getName(),pusher,guid);

        
        tGeneric(now,later);
        

        assertGreaterThan("u1 did no work",100*1024,uploader1.fullRequestsUploaded());

        assertGreaterThan("u2 did no work",100*1024,uploader2.fullRequestsUploaded());
        assertLessThan("u2 did too much work",550*1024,uploader2.fullRequestsUploaded());

        assertGreaterThan("pusher did no work",100*1024,pusher.fullRequestsUploaded());
        
        
        AlternateLocationCollection alc = uploader1.getAlternateLocations();
        assertTrue("interested uploader did not get pushloc",alc.contains(pushLoc));
        
        
        alc=uploader2.getAlternateLocations();
        assertFalse("not interested uploader got pushloc",alc.contains(pushLoc));
        
        
        alc=pusher.getAlternateLocations();
        assertFalse("not interested uploader got pushloc",alc.contains(pushLoc));
        

    }
    
    /**
     * tests that a pushloc which we thought did not support FWT 
     * but actually does updates its status through the headers,
     * as well as that the set of push proxies is getting updated.
     */
    public void testPushLocUpdatesStatus() throws Exception {
        
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
        
        Set expectedProxies = new HashSet(pe.getProxies());
        
        PushAltLoc pushLocFWT = (PushAltLoc)AlternateLocation.create(
                guid.toHexString()+";5:4.3.2.1;127.0.0.1:"+PPORT_2,TestFile.hash());
        pushLocFWT.updateProxies(true);
        
        assertEquals(1,pushLocFWT.getPushAddress().getProxies().size());
        
        RemoteFileDesc openRFD = newRFDWithURN(PORT_1,100);
        
        RemoteFileDesc pushRFD2 = newRFDPush(PPORT_2,3);
        assertFalse(pushRFD2.supportsFWTransfer());
        assertTrue(pushRFD2.needsPush());
        
        PushAcceptor pa2 = new PushAcceptor(PPORT_2,RouterService.getPort(),
                savedFile.getName(),pusher2,guid);
        
        RemoteFileDesc [] now = {pushRFD2};
        RemoteFileDesc [] later = {openRFD};
        
        tGeneric(now,later);
        
        AlternateLocationCollection alc = uploader1.getAlternateLocations();
        assertEquals(1,alc.getAltLocsSize());
        
        PushAltLoc pushLoc = (PushAltLoc)alc.iterator().next();
        
        assertEquals(UDPConnection.VERSION,pushLoc.supportsFWTVersion());
        
        RemoteFileDesc readRFD = pushLoc.createRemoteFileDesc(1);
        assertTrue(readRFD.supportsFWTransfer());
        
        assertEquals(expectedProxies.size(),readRFD.getPushProxies().size());
        
        expectedProxies.retainAll(readRFD.getPushProxies());
        
        assertEquals(expectedProxies.size(),readRFD.getPushProxies().size());
        
    }
    
    /**
     * tests that bad push locs get removed
     */
    public void testBadPushLocGetsDemotedNotAdvertised() throws Exception {
        
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
        
        uploader1.setAlternateLocations(alc);
        
        //add the push loc directly to the _incomming collection,
        //otherwise it doesn't get demoted
        PrivilegedAccessor.setValue(uploader1,"incomingAltLocs",alc);
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1,100);
        RemoteFileDesc rfd2 = newRFDWithURN(PORT_2,100);
        
        RemoteFileDesc [] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);
        
        assertGreaterThan("u1 did no work",100*1024,uploader1.fullRequestsUploaded());
        assertGreaterThan("u2 did no work",100*1024,uploader2.fullRequestsUploaded());
        
        assertFalse("bad pushloc got advertised",
                uploader2.getAlternateLocations().contains(badPushLoc));
        
        AlternateLocationCollection newAlc = uploader1.getAlternateLocations();
        assertEquals(2,newAlc.getAltLocsSize());
        
        Iterator it = newAlc.iterator();
        AlternateLocation current = (AlternateLocation) it.next();
        
        if(! (current instanceof PushAltLoc))
            current = (AlternateLocation)it.next();
        
        assertTrue(current instanceof PushAltLoc);
        PushAltLoc pcurrent = (PushAltLoc)current;
        assertTrue(pcurrent.getPushAddress().getProxies().isEmpty());
        assertTrue(pcurrent.isDemoted());
        
    }
    
    public void testAlternateLocationsAreRemoved() throws Exception {  
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        LOG.debug("-Testing swarming from two sources one based on alt...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=50;
        final int STOP_AFTER = 1*TestFile.length()/10 - 1;          
        final int FUDGE_FACTOR=RATE*1024;  
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


        AlternateLocation al1 =	AlternateLocation.create(rfd1);
        AlternateLocation al2 =	AlternateLocation.create(rfd2);
        AlternateLocation al3 =	AlternateLocation.create(rfd3);
        ualt.add(al2);
        ualt.add(al3);

        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        int u3 = uploader3.fullRequestsUploaded();        
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tu3: "+u3+"\n");        
        LOG.debug("\tTotal: "+(u1+u2+u3)+"\n");
        //Now let's check that the uploaders got the correct AltLocs.
        //Uploader 1: Must have al3. al2 may either be demoted or removed
        //Uploader 1 got correct Alts?
        AlternateLocationCollection alts = uploader1.getAlternateLocations();
        assertTrue(alts.contains(al3));
        assertEquals("Extra alts in u1",1,alts.getAltLocsSize());
        Iterator iter = alts.iterator();
        while(iter.hasNext()) {
            AlternateLocation loc = (AlternateLocation)iter.next();
            if(loc.equals(al2))
                assertTrue("failed loc not demoted",loc.isDemoted());
        }
        //uploader 2 dies after it uploades 1 bytes less than a chunk, so it
        //does only one round of http handshakes. so it never receives alternate
        //locations, even though u1 and u3 are good locations
        alts = uploader2.getAlternateLocations();
        assertEquals("u2 did more than 1 handshake",0,alts.getAltLocsSize());
        alts = uploader3.getAlternateLocations();
        assertTrue(alts.contains(al1));
        iter = alts.iterator();
        while(iter.hasNext()) {
            AlternateLocation loc = (AlternateLocation)iter.next();
            if(loc.equals(al2))
                assertTrue("failed loc not demoted",loc.isDemoted());
        }
        //Test Downloader has correct alts: the downloader should have
        //2 or 3. If two they should be u1 and u3. If 3 u2 should be demoted 
        AlternateLocationCollection coll = (AlternateLocationCollection)
        PrivilegedAccessor.getValue(DOWNLOADER,"validAlts");
        assertTrue(coll.contains(al1));
        assertTrue(coll.contains(al3));
        iter = coll.iterator();
        while(iter.hasNext()) {
            AlternateLocation loc = (AlternateLocation)iter.next();
            if(loc.equals(al2))
                assertTrue("failed loc not demoted",loc.isDemoted());
        }
    }    

    public void testWeirdAlternateLocations() throws Exception {  
        LOG.debug("-Testing AlternateLocation write...");
        
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
        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);
        
        //Check to check the alternate locations
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();

        AlternateLocation agood = AlternateLocation.create(rfd1);

        assertEquals("uploader got bad alt locs",0,alt1.getAltLocsSize());
        AlternateLocationCollection coll = (AlternateLocationCollection)
        PrivilegedAccessor.getValue(DOWNLOADER,"validAlts");
        assertTrue(coll.contains(agood));
        assertFalse(coll.contains(HugeTestUtils.EQUAL_SHA1_LOCATIONS[0]));
        assertFalse(coll.contains(HugeTestUtils.EQUAL_SHA1_LOCATIONS[1]));
        assertFalse(coll.contains(HugeTestUtils.EQUAL_SHA1_LOCATIONS[2]));
    }

    public void testStealerInterruptedWithAlternate() throws Exception {
        LOG.debug("-Testing swarming of rfds ignoring alt ...");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE=200;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = 1*TestFile.length()/10;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader1.stopAfter(STOP_AFTER);
        uploader2.setRate(RATE);
        uploader3.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2,100,TestFile.hash().toString());
        RemoteFileDesc rfd3=newRFDWithURN(PORT_3,100,TestFile.hash().toString());
        RemoteFileDesc rfd4=newRFDWithURN(PORT_4,100,TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2,rfd3};

        //Prebuild an uploader alt in lieu of rdf4
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.create(rfd4.getSHA1Urn());

        AlternateLocation al4 = AlternateLocation.create(rfd4);
        ualt.add(al4);

        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        int u3 = uploader3.fullRequestsUploaded();
        int u4 = uploader4.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tu3: "+u3+"\n");
        LOG.debug("\tu4: "+u4+"\n");
        LOG.debug("\tTotal: "+(u1+u2+u3)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertEquals("u1 did too much work", STOP_AFTER, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        assertGreaterThan("u3 did no work", 0, u3);
        assertEquals("u4 was used", 0, u4);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testPartialSourceIsAddedAfterPortion() throws Exception {
        
        // change the minimum required bytes so it'll be added.
        PrivilegedAccessor.setValue(HTTPDownloader.class,
            "MIN_PARTIAL_FILE_BYTES", new Integer(1) );
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
            "_acceptedIncoming", Boolean.TRUE );
            
        LOG.debug("-Testing that downloader adds itself to the mesh");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
        
        AlternateLocationCollection u1Alt = uploader1.getAlternateLocations();
        AlternateLocationCollection u2Alt = uploader2.getAlternateLocations();
                    
        // neither uploader knows any alt locs.
        assertNull(u1Alt);
        assertNull(u2Alt);

        // the rate must be absurdly slow for the incomplete file.length()
        // check in HTTPDownloader to be updated.
        final int RATE=50;
        final int STOP_AFTER = TestFile.length()/3;
        final int FUDGE_FACTOR=RATE*1024;  
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
        
        //both uploaders should know that this downloader is an alt loc.
        u1Alt = uploader1.getAlternateLocations();
        u2Alt = uploader2.getAlternateLocations();
        assertNotNull(u1Alt);
        assertNotNull(u2Alt);

        AlternateLocation al = AlternateLocation.create(TestFile.hash());
        assertTrue( u1Alt.contains(al) );
        assertTrue( u2Alt.contains(al) );        

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertEquals("u1 did too much work", STOP_AFTER, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testAlternateLocationsFromPartialDoBootstrap() throws Exception {
        LOG.debug("-Testing a shared partial funnels alt locs to downloader");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
            
        final int RATE=200;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = TestFile.length()/10;
        final int FUDGE_FACTOR=RATE*1024;  
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
                    Thread.sleep(1000);
                    FileDesc fd = RouterService.getFileManager().
                        getFileDescForUrn(TestFile.hash());
                    assertInstanceof( IncompleteFileDesc.class, fd );
                    fd.add(
                        AlternateLocation.create(rfd2));
                    AlternateLocationCollection alcs =
                        AlternateLocationCollection.create(
                            TestFile.hash());
                    alcs.add(
                        AlternateLocation.create(rfd3));
                    fd.addAll(alcs);
                } catch(Throwable e) {
                    ErrorService.error(e);
                }
            }
       });
       locAdder.start();
        
        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        int u3 = uploader3.fullRequestsUploaded();
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
        LOG.debug("-Testing alt locs from partial bootstrap resumed download");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
            SpeedConstants.MODEM_SPEED_INT);
            
        final int RATE=200;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = TestFile.length()/10;
        final int FUDGE_FACTOR=RATE*1024;  
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
        AlternateLocationCollection alcs =
            AlternateLocationCollection.create(TestFile.hash());
        alcs.add(al2);
        alcs.add(al3);
        
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
        fd.add(al1);
        fd.addAll(alcs);
        
        tResume(incFile);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        int u3 = uploader3.fullRequestsUploaded();
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

    public void testUpdateWhiteWithFailingFirstUploader() throws Exception {
        LOG.debug("-Testing corruption of needed. \n");
        
        final int RATE=500;
        //The first uploader got a range of 0-100%. It will return busy, the
        //needed could get corrupted becasue of this. The second downloader
        //takes over, and it should get the whole file. If needed was corrupted
        //the file will not go into complete state rather it will go to corrupt
        //state
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader1.setBusy(true);
        uploader2.setRate(RATE/4);//slower downloader - guarantee second spot
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        tGeneric(rfds);        

        int u1 = uploader1.fullRequestsUploaded();
        int u2 = uploader2.fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        LOG.debug("passed \n");
    }

    public void testQueuedDownloader() throws Exception {
        LOG.debug("-Testing queued downloader. \n");
        
        uploader1.setQueue(true);
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        //the queued downloader will resend the query after sleeping,
        //and then it shold complete the download, because TestUploader
        //resets queue after sending 503
        tGeneric(rfds);
    }
    
    public void testBusyHostIsUsed() throws Exception {
        LOG.debug("-Testing a once-busy host is reused.");
        
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
        RemoteFileDesc[] later = {rfd2};
        
        // Interesting odd factoid about the test:
        // Whether or not RFD1 or RFD2 is tried first is a BIG DEAL.
        // This test is making sure that RFD1 is reused even though
        // RFD2 is actively downloading.  However, because ManagedDownloader
        // sets the RetryAfter time differently depending on if
        // someone is already downloading (and this test will fail
        // if it sets the time to be the longer 10 minute wait),
        // we must ensure that RFD1 is tried first, so the wait
        // is only set to 1 minute.
        
        tGeneric(rfds, later);
        
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
        LOG.debug("-Testing dloader gets alt from 503 uploader...");
        
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

        uploader1.setAlternateLocations(ualt);

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
        LOG.debug("-Testing download with initial alts");
        
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
        LOG.debug("-testing that a good source does not dislodge other good ones"+
              " when swarming at capacity");
       int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 200;
        final int FUDGE_FACTOR = RATE*1024;
        uploader1.setRate(RATE);
        uploader3.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1, rfd2};
        
        RemoteFileDesc rfd3 = newRFDWithURN(PORT_3, 100);
        
        ManagedDownloader downloader = null;        
        downloader=(ManagedDownloader)RouterService.download(rfds, false, null);
        Thread.sleep(2500);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",0, queued);

        //try to add a third
        downloader.addDownloadForced(rfd3, true);
        Thread.sleep(1000);
        
        //make sure we did not kill anybody
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not replaced ",0, queued);

        waitForComplete(false);
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
        LOG.debug("-testing that if max threads are queued or downloading, and a "+
              "good location comes along, the queued downloader is dislodged");
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 200;
        final int FUDGE_FACTOR = RATE*1024;
        uploader1.setRate(RATE);
        uploader3.setRate(RATE);
        uploader2.setRate(RATE);
        uploader2.setQueue(true);
        uploader2.unqueue = false; //never unqueue this uploader.
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100);
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1};//one good and one queued
        
        RemoteFileDesc rfd3 = newRFDWithURN(PORT_3, 100);
        
        ManagedDownloader downloader = null;
        
        downloader=(ManagedDownloader)RouterService.download(rfds, false, null);
        Thread.sleep(2000);
        downloader.addDownload(rfd2,false);
        Thread.sleep(2000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1, queued);

        downloader.addDownload(rfd3, true);
        Thread.sleep(400);
        
        //make sure we killed the queued
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not replaced ",0, queued);

        waitForComplete(false);
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
        LOG.debug("-testing that if max threads are queued or downloading, and a "+
              "queued downloader gets by a queued downloader only if the new "+
              "one has a better queue position");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 50;
        final int FUDGE_FACTOR = RATE*1024;
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
        Thread.sleep(2000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        int qPos=downloader.getQueuePosition();
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 3 which is worse, nothing should change
        downloader.addDownload(rfd3,true);
        Thread.sleep(2000);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = downloader.getQueuePosition();
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 4 which is better, we should drop uploader2
        downloader.addDownload(rfd4,true);
        Thread.sleep(2000);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = downloader.getQueuePosition();
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 4 not queued ",1,queued);
        assertEquals("incorrect queue pos ",1,qPos);        

        waitForComplete(false);
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
        LOG.debug("-testing that if queued downloaders advance we downloaders "+
              "register that they did, so that the choice of which downloader"+
              " to replace is made correctly");
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 50;
        final int FUDGE_FACTOR = RATE*1024;
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
        Thread.sleep(1500);
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
        Thread.sleep(1000);
        //now uploader 2 should have been removed.
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        int qPos = downloader.getQueuePosition();
        
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("queued uploader not dropped",1,queued);
        assertEquals("wrong uploader removed",1,qPos);

        waitForComplete(false);
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }
    
    /**
     * This test MUST BE LAST because it leaves a file around.
     * I suppose it could be cleaned up ... but oh well.
     * Easier to make it last.
     */
    public void testPartialDownloads() throws IOException {
        LOG.debug("-Testing partial downloads...");
        uploader1.setPartial(true);
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        Downloader downloader = null;
        try {
            downloader = RouterService.download(rfds,false,null);
        } catch (AlreadyDownloadingException adx) {
            assertTrue("downloader already downloading??",false);
        }
        waitForBusy(downloader);
        assertEquals("Downloader did not go to busy after getting ranges",
                     Downloader.WAITING_FOR_RETRY, downloader.getState());
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

        download=RouterService.download(rfds, alts, false, null);
        if(later != null) {
            Thread.sleep(100);
            for(int i = 0; i < later.length; i++)
                ((ManagedDownloader)download).addDownload(later[i], true);
        }

        waitForComplete(false);
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
    
    private static void tGenericCorrupt(RemoteFileDesc[] now)
      throws Exception {
        tGenericCorrupt(now, null);
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

        waitForComplete(false);
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
        Downloader download = null;
        
        download = RouterService.download(incFile);
        
        waitForComplete(false);
        if (isComplete())
            LOG.debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        IncompleteFileManager ifm=dm.getIncompleteFileManager();
        VerifyingFile vf = ifm.getEntry(incFile);
        assertNull("verifying file should be null", vf);
    }

    private static URL genericURL(String url) {
        URL    theURL = null;
        try {
            theURL = new URL(url);
        } catch( Exception e ) {
            fail("Generic URL creation failed", e);
        }  
        return theURL;
    }


    private static RemoteFileDesc newRFD(int port, int speed) {
        return new RemoteFileDesc("127.0.0.1", port,
                                  0, savedFile.getName(),
                                  TestFile.length(), new byte[16],
                                  speed, false, 4, false, null, null,
                                  false,false,"",0,null, -1);
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
                                  false, false,"",0,null, -1);
    }
    
    private static RemoteFileDesc newRFDPush(int port,int suffix) throws Exception{
        PushAltLoc al = (PushAltLoc)AlternateLocation.create(
                guid.toHexString()+";127.0.0.1:"+port,TestFile.hash());
        al.updateProxies(true);
        
        Set urns = new HashSet();
        urns.add(TestFile.hash());
        
        return new RemoteFileDesc("127.0.0."+suffix, 6346, 0, savedFile.getName(),
                TestFile.length(),100,false,1,false, 
                null,urns,false,
                true,"ALT",0,0,al.getPushAddress());
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
    
    private static void waitForComplete(boolean isCorrupt) {
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
        
        if ( isCorrupt )
            assertEquals("unexpected state", Downloader.CORRUPT_FILE, DOWNLOADER.getState());
        else
            assertEquals("unexpected state", Downloader.COMPLETE, DOWNLOADER.getState());
    }        
    
    private static void waitForBusy(Downloader downloader) {
        for(int i=0; i< 12; i++) { //wait 12 seconds
            if(downloader.getState() == Downloader.WAITING_FOR_RETRY)
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
        }
    }
    
    private static class PushAcceptor extends Thread{
        private final int _portC;
        private DatagramSocket sock;
        private final String _fileName;
        private final TestUploader _uploader;
        private final GUID _g;
        public boolean sentGIV;
        
        public PushAcceptor(int portL,int portC,String filename,TestUploader uploader,GUID g) {
            super("push acceptor "+portL+"->"+portC);
            
            _portC=portC;
            _fileName=filename;
            _uploader=uploader;
            _g=g;
            try {
                sock = new DatagramSocket(portL);
                //sock.connect(InetAddress.getLocalHost(),portC);
                sock.setSoTimeout(15000);
            }catch(IOException bad) {
                ErrorService.error(bad);
            }
            start();
        }
        
        public void run() {
            DatagramPacket p = new DatagramPacket(new byte[1024],1024);
            Message m = null;
            try {
                LOG.debug("listening for push request on "+sock.getLocalPort());
                sock.receive(p);
                ByteArrayInputStream bais = new ByteArrayInputStream(p.getData());            
                m = Message.read(bais);
                
                assertTrue(m instanceof PushRequest);
                
                LOG.debug("received a push request");
                
                Socket s = Sockets.connect("127.0.0.1",_portC,500);
                
                OutputStream os = s.getOutputStream();
                
                String GIV = "GIV 0:"+_g.toHexString()+"/"+_fileName+"\n\n";
                os.write(GIV.getBytes());
                
                os.flush();
                
                LOG.debug("wrote GIV");
                sentGIV=true;
                _uploader.setSocket(s);
                
            }catch(BadPacketException bad) {
                ErrorService.error(bad);
            }catch(IOException bad) {
                ErrorService.error(bad);
            }finally {
                sock.close();
            }
            

        }
    }
}
