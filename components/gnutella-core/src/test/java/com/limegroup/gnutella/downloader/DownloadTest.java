package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.altlocs.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.gui.*;
import com.limegroup.gnutella.settings.*;

import javax.swing.JOptionPane;

import java.io.*;
import java.net.URL;
import java.net.InetAddress;

import com.sun.java.util.collections.*;

import junit.framework.*;


import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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
	private static DownloadManager dm;// = new DownloadManager();
	private static final ActivityCallbackStub callback = new MyCallback();
	private static ManagedDownloader DOWNLOADER = null;
	private static Object COMPLETE_LOCK = new Object();
	private static boolean REMOVED = false;
	private static final long TWO_MINUTES = 1000 * 60 * 2;
	
    
    public static void globalSetUp() throws Exception {
		RouterService rs = new RouterService(callback);
        dm = rs.getDownloadManager();
        dm.initialize();
        RouterService.getAcceptor().setAddress(InetAddress.getLocalHost());
        
        //SimpleTimer timer = new SimpleTimer(true);
        Runnable click = new Runnable() {
            public void run() {
                dm.measureBandwidth();
            }
        };
        RouterService.schedule(click,0,SupernodeAssigner.TIMER_DELAY);
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

        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        // Don't wait for network connections for testing
        ManagedDownloader.NO_DELAY = true;
        
        uploader1=new TestUploader("PORT_1", PORT_1);
        uploader2=new TestUploader("PORT_2", PORT_2);
        uploader3=new TestUploader("PORT_3", PORT_3);
        uploader4=new TestUploader("PORT_4", PORT_4);
        
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
    }    

    public void tearDown() {
        
        uploader1.reset();
        uploader2.reset();
        uploader3.reset();
        uploader4.reset();
        
        uploader1.stopThread();
        uploader2.stopThread();
        uploader3.stopThread();
        uploader4.stopThread();

        deleteAllFiles();
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
    
    private static void tOverlapCheckSpeed(int rate) throws Exception {
        RemoteFileDesc rfd=newRFDWithURN(PORT_1, 100);
        LOG.debug("-Measuring time for download at rate "+rate+"... \n");
        uploader1.setRate(rate);
        long start1=System.currentTimeMillis();

        HTTPDownloader downloader=new HTTPDownloader(rfd, savedFile);
        VerifyingFile vf = new VerifyingFile(true);
        vf.open(savedFile,null);
        downloader.connectTCP(0);
        downloader.connectHTTP(0,TestFile.length(),true);
        downloader.doDownload(vf);

        long elapsed1=System.currentTimeMillis()-start1;
        
        RandomAccessFile raf = new RandomAccessFile(savedFile,"rw");
        raf.seek(300);
        raf.write(65);
        
        long start2=System.currentTimeMillis();

        downloader=new HTTPDownloader(rfd, savedFile);
        vf = new VerifyingFile(false);
        vf.open(savedFile,null);
        downloader.connectTCP(0);
        downloader.connectHTTP(0, TestFile.length(),true);
        downloader.doDownload(vf);

        long elapsed2=System.currentTimeMillis()-start2;
        LOG.debug("  No check="+elapsed2+", check="+elapsed1 +"\n");
    }
    
    /**
     * Tests a basic download that does not swarm.
     */
    public void testSimpleDownload() throws Exception {
        LOG.debug("-Testing non-swarmed download...");
        
        RemoteFileDesc rfd=newRFD(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd};
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
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);
        
        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }


    public void testUnbalancedSwarm() throws Exception  {
        LOG.debug("-Testing swarming from two unbalanced sources...");
        
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE/10);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.  uploader1
        //should transfer 2/3 of the file and uploader2 should transfer 1/3 of
        //the file.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
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
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()-STOP_AFTER+FUDGE_FACTOR, u1);
        assertEquals("u2 did all the work", STOP_AFTER, u2);
    }


    public void testStealerInterrupted() throws Exception {
        LOG.debug("-Testing unequal swarming with stealer dying...");
        
        final int RATE=500;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = 5*TestFile.length()/8;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE/10);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work",
                    TestFile.length()-STOP_AFTER+2*FUDGE_FACTOR, u1);
        assertEquals("u2 did all the work", STOP_AFTER, u2);
    }



    public void testAddDownload() throws Exception {
        LOG.debug("-Testing addDownload (increases swarming)...");
        
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);

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
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
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
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);


        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
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
        
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int c1 = uploader1.getConnections();
        int c2 = uploader2.getConnections();
        
        assertGreaterThan("u1 not used", 0, u1);
        assertGreaterThan("u2 not used", 0, u2);
        assertEquals("extra connection attempts", 1, c1);
        assertEquals("extra connection attempts", 1, c2);
        assertTrue("slower uploader not replaced",uploader1.killedByDownloader);
        assertFalse("faster uploader killed",uploader2.killedByDownloader);
    }   
    
    public void testOverlapCheckGreyNoStopOnCorrupt() throws Exception {
        tOverlapCheckGrey(false);
    }
    
    public void testOverlapCheckGreyStopOnCorrupt() throws Exception {
        callback.delCorrupt = true;
        tOverlapCheckGrey(true);
    }

    private void tOverlapCheckGrey(boolean deleteCorrupt) throws Exception {
        LOG.debug("-Testing overlap checking from Grey area..." +
                         "stop when corrupt "+deleteCorrupt+" ");
                         
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader2.setRate(RATE/100);
        uploader2.setCorruption(true);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        
        Downloader download=null;
        //Start one location, wait a bit, then add another.
        download=RouterService.download(new RemoteFileDesc[] {rfd1,rfd2}, false,
                                        null);
        waitForComplete(deleteCorrupt);
        LOG.debug("passed"+"\n");//got here? Test passed
        //TODO: check IncompleteFileManager, disk
    }

    public void testOverlapCheckWhiteNoStopOnCorrupt() throws Exception {
        tOverlapCheckWhite(false);
    }
    
    public void testOverlapCheckWhiteStopOnCorrupt() throws Exception {
        callback.delCorrupt = true;        
        tOverlapCheckWhite(true);
    }

    private void tOverlapCheckWhite(boolean deleteCorrupt) throws Exception {
        LOG.debug("-Testing overlap checking from White area..."+
                         "stop when corrupt "+deleteCorrupt+" ");
                         
        final int RATE=500;
        uploader1.setCorruption(true);
        uploader1.stopAfter(TestFile.length()/8);//blinding fast
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        
        Downloader download=null;

        //Start one location, wait a bit, then add another.
        download=RouterService.download(new RemoteFileDesc[] {rfd1,rfd2}, false,
                                        null);
        waitForComplete(deleteCorrupt);
        LOG.debug("passed"+"\n");//got here? Test passed
    }
    
    public void testMismatchedVerifyHashNoStopOnCorrupt() throws Exception {
        tMismatchedVerifyHash(false);
    }
    
    public void testMismatchedVerifyHashStopOnCorrupt() throws Exception {
        callback.delCorrupt = true;        
        tMismatchedVerifyHash(true);
    }

    private void tMismatchedVerifyHash(boolean deleteCorrupt) throws Exception {
        LOG.debug("-Testing file declared corrupt, when hash of "+
                         "downloaded file mismatches bucket hash" +
                         "stop when corrupt "+ deleteCorrupt+" ");
                         
        final int RATE=100;
        uploader1.setRate(RATE);
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1,100,
        "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
        Downloader download = null;
        
        download = RouterService.download(new RemoteFileDesc[] {rfd1}, false, 
                                          null);
        waitForComplete(deleteCorrupt);
        LOG.debug("passed"+"\n");//got here? Test passed
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
        AlternateLocation dAlt = AlternateLocation.create(rfd1.getUrl());
           
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

        AlternateLocation al1 = AlternateLocation.create(rfd1.getUrl());
        AlternateLocation al2 = AlternateLocation.create(rfd2.getUrl());
        
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

        URL url2 = rfd2.getUrl();
        AlternateLocation al2 =
			AlternateLocation.create(url2);
        ualt.add(al2);

        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
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

        URL url2 = rfd2.getUrl();
        URL url3 = rfd3.getUrl();        
        AlternateLocation al1 =	AlternateLocation.create(rfd1);
        AlternateLocation al2 =	AlternateLocation.create(url2);
        AlternateLocation al3 =	AlternateLocation.create(url3);
        ualt.add(al2);
        ualt.add(al3);

        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();        
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
                assertTrue("failed loc not demoted",loc.getDemoted());
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
                assertTrue("failed loc not demoted",loc.getDemoted());
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
                assertTrue("failed loc not demoted",loc.getDemoted());
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
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();
        int u4 = uploader4.amountUploaded();
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
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
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
    
    public void testPartialSourceNotAddedWithCorruption() throws Exception {
        
        // change the minimum required bytes so it'll be added.
        PrivilegedAccessor.setValue(HTTPDownloader.class,
            "MIN_PARTIAL_FILE_BYTES", new Integer((TestFile.length()/3)*2) );
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
            "_acceptedIncoming", Boolean.TRUE );
            
        LOG.debug("-Testing that downloader does not add to mesh if corrupt");
        
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
        final int RATE=25;
        final int STOP_AFTER = TestFile.length()/3;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader1.stopAfter(STOP_AFTER);
        uploader1.setCorruption(true);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1,100,TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2,100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGenericCorrupt(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //both uploaders should know that this downloader is an alt loc.
        u1Alt = uploader1.getAlternateLocations(); //should have u2
        u2Alt = uploader2.getAlternateLocations(); //should have u1 but demoted
        assertNotNull(u1Alt);  
        assertNotNull(u2Alt);
        AlternateLocation al = AlternateLocation.create(TestFile.hash());
        assertTrue( !u1Alt.contains(al) );
        assertTrue( !u2Alt.contains(al) );
        
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
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();
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
        ifm.addEntry(incFile, new VerifyingFile(true));
        
        // Get the IncompleteFileDesc and add these alt locs to it.
        FileDesc fd =
            RouterService.getFileManager().getFileDescForUrn(TestFile.hash());
        assertNotNull(fd);
        assertInstanceof(IncompleteFileDesc.class, fd);
        fd.add(al1);
        fd.addAll(alcs);
        
        tResume(incFile);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();
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

	// no longer in use, as alternate locations should always have SHA1s
	/*
    private static void tTwoAlternatesButOneWithNoSHA1() {  
        LOG.debug("-Testing Two Alternates but one with no sha1...");
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100); // No SHA1
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Prepare to check the alternate location - second won't be there
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection alt2 = uploader2.getAlternateLocations();
        AlternateLocationCollection ashould = 
			AlternateLocationCollection.create(rfd1.getSHA1Urn());
        try {
            URL url1 = rfd1.getUrl();//  rfdURL(rfd1);
            AlternateLocation al1 =
				AlternateLocation.create(url1);
            ashould.add(al1);
        } catch (Exception e) {
            check(false, "Couldn't setup test");
        }
        AlternateLocationCollection adiff = 
			ashould.diffAlternateLocationCollection(alt1); 
        AlternateLocationCollection adiff2 = 
			alt1.diffAlternateLocationCollection(ashould); 
        
        check(alt1.hasAlternateLocations(), "uploader1 didn't receive alt");
        check(alt2.hasAlternateLocations(), "uploader2 didn't receive alt");
        check(!adiff.hasAlternateLocations(), "uploader got wrong alt");
        check(!adiff2.hasAlternateLocations(), "uploader got wrong alt");
		}
	*/

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

        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        LOG.debug("passed \n");
    }

    public void testQueuedDownloader() throws Exception {
        LOG.debug("-Testing queued downloader. \n");
        
        uploader1.setQueue(true);
        RemoteFileDesc rfd1 = newRFD(PORT_1, 100);
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
        
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        
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
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
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
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc[] rfds1 = {rfd1};
        List rfds2 = new LinkedList();
        rfds2.add(rfd2);
        
        tGeneric(rfds1, rfds2);
        
        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
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
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        RemoteFileDesc rfd3 = newRFD(PORT_3, 100);
        
        ManagedDownloader downloader = null;        
        downloader=(ManagedDownloader)RouterService.download(rfds, false, null);
        Thread.sleep(1000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",0, queued);

        //try to add a third
        downloader.addDownload(rfd3, true);
        Thread.sleep(1000);
        
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
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();

        assertLessThan("u1 did too much", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did too much", TestFile.length()/2+FUDGE_FACTOR, u2);
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
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        RemoteFileDesc rfd3 = newRFD(PORT_3, 100);
        
        ManagedDownloader downloader = null;
        
        downloader=(ManagedDownloader)RouterService.download(rfds, false, null);
        Thread.sleep(1000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1, queued);

        downloader.addDownload(rfd3, true);
        Thread.sleep(1000);
        
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
        
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();
        
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

        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc rfd3=newRFD(PORT_3, 100);
        RemoteFileDesc rfd4=newRFD(PORT_4, 100);
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        ManagedDownloader downloader = null;
        downloader = (ManagedDownloader)RouterService.download(rfds, false, null);
        Thread.sleep(1000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        int qPos = Integer.parseInt
        ((String)PrivilegedAccessor.getValue(downloader,"queuePosition"));
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 3 which is worse, nothing should change
        downloader.addDownload(rfd3,true);
        Thread.sleep(1000);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = Integer.parseInt
        ((String)PrivilegedAccessor.getValue(downloader,"queuePosition"));
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 4 which is better, we should drop uploader2
        downloader.addDownload(rfd4,true);
        Thread.sleep(1000);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = Integer.parseInt
        ((String)PrivilegedAccessor.getValue(downloader,"queuePosition"));
        
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
        
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        RemoteFileDesc rfd3=newRFD(PORT_3, 100);
        
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        ManagedDownloader downloader = null;
        downloader = (ManagedDownloader)RouterService.download(rfds,false,null);
        Thread.sleep(1000);
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
        int qPos = Integer.parseInt
        ((String)PrivilegedAccessor.getValue(downloader,"queuePosition"));
        
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
        tGeneric(now, later, DataUtils.EMPTY_LIST);
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
    
    /**
     * Performs a generic download of the file specified in <tt>rfds</tt>.
     */
    private static void tGenericCorrupt(RemoteFileDesc[] rfds) throws Exception {
        Downloader download=null;

        download=RouterService.download(rfds, false, null);

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

	/*
    private static URL rfdURL(RemoteFileDesc rfd) {
        String rfdStr;
        URL    rfdURL = null;
        rfdStr = "http://"+rfd.getHost()+":"+
        rfd.getPort()+"/get/"+String.valueOf(rfd.getIndex())+
        "/"+rfd.getFileName();
        try {
            rfdURL = new URL(rfdStr);
        } catch( Exception e ) {
            check(false, "URL creation failed");
        }  
        return rfdURL;
    }
	*/

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
                                  false,false,"",0,null);
    }

	private static RemoteFileDesc newRFDWithURN(int port, int speed) {
		return newRFDWithURN(port, speed, null);
	}

    private static RemoteFileDesc newRFDWithURN(int port, int speed, 
                                                String urn) {
        com.sun.java.util.collections.Set set = 
			new com.sun.java.util.collections.HashSet();
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
                                  false, false,"",0,null);
    }

    /** Returns true if the complete file exists and is complete */
    private static boolean isComplete() {
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
                COMPLETE_LOCK.wait(TWO_MINUTES);
            } catch (InterruptedException e) {
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
                assertTrue("downloader unexpecteted interrupted",false);
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
}
