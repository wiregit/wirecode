package com.limegroup.gnutella.downloader;

import java.io.*;
import com.sun.java.util.collections.*;
import java.net.URL;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.gui.*;
import com.limegroup.gnutella.settings.*;
//import com.limegroup.gnutella.gui.search.*;
import javax.swing.JOptionPane;
import junit.framework.*;

/**
 * Comprehensive test of downloads -- one of the most important tests in
 * LimeWire.
 */
public class DownloadTest extends com.limegroup.gnutella.util.BaseTestCase {

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
        CommonUtils.getResourceFile(filePath + "saved");
    
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
	
    
    public static void globalSetUp() {
		RouterService rs = new RouterService(callback);
        dm = rs.getDownloadManager();
        dm.initialize();
        
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
        debug("-Measuring time for download at rate "+rate+"... \n");
        uploader1.setRate(rate);
        long start1=System.currentTimeMillis();
        AlternateLocationCollection dcoll = 
			AlternateLocationCollection.createCollection(rfd.getSHA1Urn());

        HTTPDownloader downloader=new HTTPDownloader(rfd, savedFile,dcoll);
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

        AlternateLocationCollection dcol = 
			AlternateLocationCollection.createCollection(rfd.getSHA1Urn());
        downloader=new HTTPDownloader(rfd, savedFile,dcol);
        vf = new VerifyingFile(false);
        vf.open(savedFile,null);
        downloader.connectTCP(0);
        downloader.connectHTTP(0, TestFile.length(),true);
        downloader.doDownload(vf);

        long elapsed2=System.currentTimeMillis()-start2;
        debug("  No check="+elapsed2+", check="+elapsed1 +"\n");
    }
    
    /**
     * Tests a basic download that does not swarm.
     */
    public void testSimpleDownload() throws Exception {
        debug("-Testing non-swarmed download...");
        
        RemoteFileDesc rfd=newRFD(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd};
        tGeneric(rfds);
    }
    
    public void testSimpleSwarm() throws Exception {
        debug("-Testing swarming from two sources...");
        
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
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");
        
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }


    public void testUnbalancedSwarm() throws Exception  {
        debug("-Testing swarming from two unbalanced sources...");
        
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
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");

        assertLessThan("u1 did all the work", 9*TestFile.length()/10+FUDGE_FACTOR*10, u1);
        assertLessThan("u2 did all the work", TestFile.length()/10+FUDGE_FACTOR, u2);
    }


    public void testSwarmWithInterrupt() throws Exception {
        debug("-Testing swarming from two sources (one broken)...");
        
        final int RATE=500;
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
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()-STOP_AFTER+FUDGE_FACTOR, u1);
        assertEquals("u2 did all the work", STOP_AFTER, u2);
    }


    public void testStealerInterrupted() throws Exception {
        debug("-Testing unequal swarming with stealer dying...");
        
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
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work",
                    TestFile.length()-STOP_AFTER+2*FUDGE_FACTOR, u1);
        assertEquals("u2 did all the work", STOP_AFTER, u2);
    }



    public void testAddDownload() throws Exception {
        debug("-Testing addDownload (increases swarming)...");
        
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);

        Downloader download=null;

        //Start one location, wait a bit, then add another.
        download=dm.download(new RemoteFileDesc[] {rfd1}, false);
        ((ManagedDownloader)download).addDownload(rfd2,true);

        waitForComplete(false);
        if (isComplete())
            debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        //Make sure there weren't too many overlapping regions. Each upload
        //should do roughly half the work.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");

        assertLessThan("u1 did all the work", (TestFile.length()/2+FUDGE_FACTOR), u1);
        assertLessThan("u2 did all the work", (TestFile.length()/2+FUDGE_FACTOR), u2);
    }

    public void testStallingUploaderReplaced() throws Exception  {
        debug("-Testing download completion with stalling downloader...");
        
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
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 

        debug("passed"+"\n");//file downloaded? passed
    }
    
    public void testOverlapCheckGreyNoStopOnCorrupt() throws Exception {
        tOverlapCheckGrey(false);
    }
    
    public void testOverlapCheckGreyStopOnCorrupt() throws Exception {
        callback.delCorrupt = true;
        tOverlapCheckGrey(true);
    }

    private void tOverlapCheckGrey(boolean deleteCorrupt) throws Exception {
        debug("-Testing overlap checking from Grey area..." +
                         "stop when corrupt "+deleteCorrupt+" ");
                         
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader2.setRate(RATE/100);
        uploader2.setCorruption(true);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        
        Downloader download=null;
        //Start one location, wait a bit, then add another.
        download=dm.download(new RemoteFileDesc[] {rfd1,rfd2}, false);
        waitForComplete(deleteCorrupt);
        debug("passed"+"\n");//got here? Test passed
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
        debug("-Testing overlap checking from White area..."+
                         "stop when corrupt "+deleteCorrupt+" ");
                         
        final int RATE=500;
        uploader1.setCorruption(true);
        uploader1.stopAfter(TestFile.length()/8);//blinding fast
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(PORT_1, 100);
        RemoteFileDesc rfd2=newRFD(PORT_2, 100);
        
        Downloader download=null;

        //Start one location, wait a bit, then add another.
        download=dm.download(new RemoteFileDesc[] {rfd1,rfd2}, false);
        waitForComplete(deleteCorrupt);
        debug("passed"+"\n");//got here? Test passed
    }
    
    public void testMismatchedVerifyHashNoStopOnCorrupt() throws Exception {
        tMismatchedVerifyHash(false);
    }
    
    public void testMismatchedVerifyHashStopOnCorrupt() throws Exception {
        callback.delCorrupt = true;        
        tMismatchedVerifyHash(true);
    }

    private void tMismatchedVerifyHash(boolean deleteCorrupt) throws Exception {
        debug("-Testing file declared corrupt, when hash of "+
                         "downloaded file mismatches bucket hash" +
                         "stop when corrupt "+ deleteCorrupt+" ");
                         
        final int RATE=100;
        uploader1.setRate(RATE);
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1,100,
        "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
        Downloader download = null;
        
        download = dm.download(new RemoteFileDesc[] {rfd1}, false);        
        waitForComplete(deleteCorrupt);
        debug("passed"+"\n");//got here? Test passed
    }

    public void testSimpleAlternateLocations() throws Exception {  
        debug("-Testing AlternateLocation write...");
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1};

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection ashould = 
			AlternateLocationCollection.createCollection(rfd1.getSHA1Urn());

        ashould.addAlternateLocation(
                AlternateLocation.createAlternateLocation(rfd1.getUrl()));

        AlternateLocationCollection adiff = 
        ashould.diffAlternateLocationCollection(alt1); 

        URN sha1 = rfd1.getSHA1Urn();
        URN uSHA1 = uploader1.getReportedSHA1();
        
        assertTrue("uploader didn't recieve alt", alt1.hasAlternateLocations());
        assertTrue("uploader got wrong alt", !adiff.hasAlternateLocations());
        assertNotNull("rfd1 sha1", sha1);
        assertNotNull("uploader1 sha1", uSHA1);
        assertEquals("SHA1 test failed", sha1, uSHA1);
    }

    public void testTwoAlternateLocations() throws Exception {  
        debug("-Testing Two AlternateLocations...");
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection alt2 = uploader2.getAlternateLocations();
        AlternateLocationCollection ashould = 
			AlternateLocationCollection.createCollection(rfd1.getSHA1Urn());

        URL url1 = rfd1.getUrl();
        URL url2 = rfd2.getUrl();
        AlternateLocation al1 =
			AlternateLocation.createAlternateLocation(url1);
        AlternateLocation al2 =
			AlternateLocation.createAlternateLocation(url2);
        ashould.addAlternateLocation(al1);
        ashould.addAlternateLocation(al2);

        AlternateLocationCollection adiff = 
            ashould.diffAlternateLocationCollection(alt1); 
        AlternateLocationCollection adiff2 = 
            alt1.diffAlternateLocationCollection(ashould); 
        
        assertTrue("uploader didn't recieve alt", alt1.hasAlternateLocations());
        assertTrue("uploader didn't recieve alt", alt2.hasAlternateLocations());
        assertTrue("uploader got wrong alt", !adiff.hasAlternateLocations());
        assertTrue("uploader got wrong alt", !adiff2.hasAlternateLocations());
    }

    public void testUploaderAlternateLocations() throws Exception {  
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        debug("-Testing swarming from two sources one based on alt...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.createCollection(rfd2.getSHA1Urn());

        URL url2 = rfd2.getUrl();
        AlternateLocation al2 =
			AlternateLocation.createAlternateLocation(url2);
        ualt.addAlternateLocation(al2);

        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }
    
    public void testAlternateLocationsAreRemoved() throws Exception {  
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        debug("-Testing swarming from two sources one based on alt...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        final int STOP_AFTER = 1*TestFile.length()/10 - 1;          
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        uploader3.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        RemoteFileDesc rfd3=newRFDWithURN(PORT_3, 100, TestFile.hash().toString());        
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.createCollection(rfd2.getSHA1Urn());

        URL url2 = rfd2.getUrl();
        URL url3 = rfd3.getUrl();        
        AlternateLocation al2 =
			AlternateLocation.createAlternateLocation(url2);
        AlternateLocation al3 =
			AlternateLocation.createAlternateLocation(url3);
        ualt.addAlternateLocation(al2);
        ualt.addAlternateLocation(al3);

        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();        
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tu3: "+u3+"\n");        
        debug("\tTotal: "+(u1+u2+u3)+"\n");
        
        // This is the real test.
        // We expect 2 alternate locations, one for the original RFD
        // that we sent to DownloadManager, and one for the alternate
        // location that was succesful.  The altnerate location that
        // failed should be removed from the list.
        // The location is actually being removed in
        // ManagedDownloader.establishConnection(RemoteFileDesc).
        // This is because the download stalled, but we were able to recieve
        // info, and ManagedDownloader attempts to reconnect.  TestUploader
        // will refuse the connection, and ManagedDownloader will try to send
        // a push (which of course fails), and then it assumes that the
        // location has left the network, and removes it from the list.
        assertEquals("bad alt loc wasn't removed", 
            2, DOWNLOADER.getNumberOfAlternateLocations()
        );

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertGreaterThan("u1 did no work", 0, u1);
        assertEquals("u2 did more work than needed", STOP_AFTER, u2);
        assertGreaterThan("u3 did no work", 0, u3);
    }    

    public void testWeirdAlternateLocations() throws Exception {  
        debug("-Testing AlternateLocation write...");
        
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1};


        //Prebuild some uploader alts
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.createCollection(
                HugeTestUtils.EQUAL_SHA1_LOCATIONS[0].getSHA1Urn());

        ualt.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[0]);
		//AlternateLocation.createAlternateLocation(
		//genericURL("http://211.211.211.211:PORT_2/get/0/foobar.txt")));
        ualt.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[1]);
		//  AlternateLocation.createAlternateLocation(
		//      genericURL("http://211.211.211.211/get/0/foobar.txt")));
        ualt.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[2]);
		//  AlternateLocation.createAlternateLocation(
		//      genericURL("http://www.yahoo.com/foo/bar/foobar.txt")));
        ualt.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[3]);
		//  AlternateLocation.createAlternateLocation(
		//      genericURL("http://40000000.400.400.400/get/99999999999999999999999999999/foobar.txt")));

        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection ashould = 
			AlternateLocationCollection.createCollection(rfd1.getSHA1Urn());

        ashould.addAlternateLocation(
            AlternateLocation.createAlternateLocation(rfd1));

        //ashould.addAlternateLocationCollection(ualt);
        AlternateLocationCollection adiff = 
            ashould.diffAlternateLocationCollection(alt1); 

        AlternateLocationCollection adiff2 = 
            alt1.diffAlternateLocationCollection(ashould); 
        
        assertTrue("uploader didn't receive alt", alt1.hasAlternateLocations());
        assertTrue("uploader got extra alts", !adiff.hasAlternateLocations());
        assertTrue("uploader didn't get all alts", !adiff2.hasAlternateLocations());
    }

    public void testStealerInterruptedWithAlternate() throws Exception {
        debug("-Testing swarming of rfds ignoring alt ...");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.MODEM_SPEED_INT);
        final int RATE=200;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = 1*TestFile.length()/10;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader1.stopAfter(STOP_AFTER);
        uploader2.setRate(RATE);
        uploader3.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        RemoteFileDesc rfd3=newRFDWithURN(PORT_3, 100, TestFile.hash().toString());
        RemoteFileDesc rfd4=newRFDWithURN(PORT_4, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2,rfd3};

        //Prebuild an uploader alt in lieu of rdf4
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.createCollection(rfd4.getSHA1Urn());

        AlternateLocation al4 =
			AlternateLocation.createAlternateLocation(rfd4);
        ualt.addAlternateLocation(al4);

        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();
        int u4 = uploader4.amountUploaded();
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tu3: "+u3+"\n");
        debug("\tu4: "+u4+"\n");
        debug("\tTotal: "+(u1+u2+u3)+"\n");

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
            
        debug("-Testing that downloader adds itself to the mesh");
        
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
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");
        
        //both uploaders should know that this downloader is an alt loc.
        u1Alt = uploader1.getAlternateLocations();
        u2Alt = uploader2.getAlternateLocations();
        assertNotNull(u1Alt);
        assertNotNull(u2Alt);
        Collection u1Locs = u1Alt.values();
        Collection u2Locs = u2Alt.values();
        AlternateLocation al =
            AlternateLocation.createAlternateLocation(TestFile.hash());
        assertTrue( u1Locs.contains(al) );
        assertTrue( u2Locs.contains(al) );        

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertEquals("u1 did too much work", STOP_AFTER, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testPartialSourceNotAddedWithCorruption() throws Exception {
        
        // change the minimum required bytes so it'll be added.
        PrivilegedAccessor.setValue(HTTPDownloader.class,
            "MIN_PARTIAL_FILE_BYTES", new Integer(TestFile.length()/2) );
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
            "_acceptedIncoming", Boolean.TRUE );
            
        debug("-Testing that downloader does not add to mesh if corrupt");
        
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
        uploader1.setCorruption(true);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGenericCorrupt(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");
        
        //both uploaders should know that this downloader is an alt loc.
        u1Alt = uploader1.getAlternateLocations();
        u2Alt = uploader2.getAlternateLocations();
        assertNotNull(u1Alt);
        assertNotNull(u2Alt);
        Collection u1Locs = u1Alt.values();
        Collection u2Locs = u2Alt.values();
        AlternateLocation al =
            AlternateLocation.createAlternateLocation(TestFile.hash());
        assertTrue( !u1Locs.contains(al) );
        assertTrue( !u2Locs.contains(al) );        

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertEquals("u1 did too much work", STOP_AFTER, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testAlternateLocationsFromPartialDoBootstrap() throws Exception {
        debug("-Testing a shared partial funnels alt locs to downloader");
        
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
                    fd.addAlternateLocation(
                        AlternateLocation.createAlternateLocation(rfd2));
                    AlternateLocationCollection alcs =
                        AlternateLocationCollection.createCollection(
                            TestFile.hash());
                    alcs.addAlternateLocation(
                        AlternateLocation.createAlternateLocation(rfd3));
                    fd.addAlternateLocationCollection(alcs);
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
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tu3: "+u3+"\n");
        debug("\tTotal: "+(u1+u2+u3)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are started at different times.

        assertEquals("u1 did too much work", STOP_AFTER, u1);
        assertEquals("u2 did too much work", STOP_AFTER, u2);
        assertGreaterThan("u3 did no work", 0, u3);
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);
    }
    
    public void testResumeFromPartialWithAlternateLocations() throws Exception {
        debug("-Testing alt locs from partial bootstrap resumed download");
        
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
        AlternateLocation al1 = AlternateLocation.createAlternateLocation(rfd1);
        AlternateLocation al2 = AlternateLocation.createAlternateLocation(rfd2);
        AlternateLocation al3 = AlternateLocation.createAlternateLocation(rfd3);
        AlternateLocationCollection alcs =
            AlternateLocationCollection.createCollection(TestFile.hash());
        alcs.addAlternateLocation(al2);
        alcs.addAlternateLocation(al3);
        
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
        fd.addAlternateLocation(al1);
        fd.addAlternateLocationCollection(alcs);
        
        tResume(incFile);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tu3: "+u3+"\n");
        debug("\tTotal: "+(u1+u2+u3)+"\n");

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
        debug("-Testing Two Alternates but one with no sha1...");
        RemoteFileDesc rfd1=newRFDWithURN(PORT_1, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(PORT_2, 100); // No SHA1
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Prepare to check the alternate location - second won't be there
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection alt2 = uploader2.getAlternateLocations();
        AlternateLocationCollection ashould = 
			AlternateLocationCollection.createCollection(rfd1.getSHA1Urn());
        try {
            URL url1 = rfd1.getUrl();//  rfdURL(rfd1);
            AlternateLocation al1 =
				AlternateLocation.createAlternateLocation(url1);
            ashould.addAlternateLocation(al1);
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
        debug("-Testing corruption of needed. \n");
        
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
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");
        debug("passed \n");
    }

    public void testQueuedDownloader() throws Exception {
        debug("-Testing queued downloader. \n");
        
        uploader1.setQueue(true);
        RemoteFileDesc rfd1 = newRFD(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        //the queued downloader will resend the query after sleeping,
        //and then it shold complete the download, because TestUploader
        //resets queue after sending 503
        tGeneric(rfds);
    }

    /**
     * Test to make sure that we read the alternate locations from the
     * uploader response headers even if the response code is a 503,
     * try again later.
     */
    public void testAlternateLocationsExchangedWithBusy() throws Exception {
        //tests that a downloader reads alternate locations from the
        //uploader even if it receives a 503 from the uploader.
        debug("-Testing dloader gets alt from 503 uploader...");
        
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
			AlternateLocationCollection.createCollection(rfd1.getSHA1Urn());

        AlternateLocation al2 =
			AlternateLocation.createAlternateLocation(rfd2);
        ualt.addAlternateLocation(al2);

        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertEquals("u1 did too much work", 0, u1);
        assertLessThan("u2 did all the work", TestFile.length()+FUDGE_FACTOR, u2);
    }
    
    public void testPartialDownloads() throws IOException {
        debug("-Testing partial downloads...");
        uploader1.setPartial(true);
        RemoteFileDesc rfd1 = newRFDWithURN(PORT_1, 100);
        RemoteFileDesc[] rfds = {rfd1};
        Downloader downloader = null;
        try {
            downloader = dm.download(rfds,false);
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
    
    /**
     * Performs a generic download of the file specified in <tt>rfds</tt>.
     */
    private static void tGeneric(RemoteFileDesc[] rfds) throws Exception {
        Downloader download=null;

        download=dm.download(rfds, false);

        waitForComplete(false);
        if (isComplete())
            debug("pass"+"\n");
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

        download=dm.download(rfds, false);

        waitForComplete(false);
        if (isComplete())
            fail("should be corrupt");
        else
            debug("pass");
        
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
        
        download = dm.download(incFile);
        
        waitForComplete(false);
        if (isComplete())
            debug("pass"+"\n");
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
            debug("File too small by: " + (TestFile.length() - savedFile.length()) );
            return false;
        } else if ( savedFile.length() > TestFile.length() ) {
            debug("File too large by: " + (savedFile.length() - TestFile.length()) );
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
                    debug("Bad byte at "+i+"\n");
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

    private static final boolean DEBUG = false;
    
    static void debug(String message) {
        if(DEBUG) 
            System.out.print(message);
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






