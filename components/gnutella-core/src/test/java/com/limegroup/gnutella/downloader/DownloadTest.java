package com.limegroup.gnutella.downloader;

import java.io.*;
import java.util.*;
import java.net.URL;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.gui.*;
import com.limegroup.gnutella.settings.*;
import com.limegroup.gnutella.gui.search.*;
import javax.swing.JOptionPane;
import junit.framework.*;

/**
 * Comprehensive test of downloads -- one of the most important tests in
 * LimeWire.
 */
public class DownloadTest extends TestCase {
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
	private static final ActivityCallbackStub callback = new ActivityCallbackStub();
	
	/**
	 * The list of tests to run.
	 * If it is empty, or if a test's number is in here, it will run.
	 * Otherwise it will generate a failure (just to let us know it isn't running).
	 */
	private static final int[] testsToRun = { };
	private static final int SIMPLE_DOWNLOAD = 1;
	private static final int SIMPLE_SWARM = 2;
	private static final int UNBALANCED_SWARM = 3;
	private static final int SWARM_WITH_INTERRUPT = 4;
	private static final int STEALER_INTERRUPTED = 5;
	private static final int ADD_DOWNLOAD = 6;
	private static final int STALLING_UPLOADER_REPLACED = 7;
	private static final int TWO_ALTERNATE_LOCATIONS = 8;
	private static final int UPLOADER_ALTERNATE_LOCATIONS = 9;
	private static final int WIERD_ALTERNATE_LOCATIONS = 10;
	private static final int STEALER_INTERRUPTED_WITH_ALTERNATE = 11;
	private static final int UPDATE_WHITE_WITH_FAILING_FIRST_UPLOADER = 12;
	private static final int QUEUED_DOWNLOADER = 13;
	private static final int ALTERNATE_LOCATIONS_EXCHANGED_WITH_BUSY = 14;
	private static final int MISMATCHED_VERIFY_HASH = 17;
	private static final int OVERLAP_CHECK_WHITE = 18;
	private static final int OVERLAP_CHECK_GREY = 19;
	private static final int SIMPLE_ALTERNATE_LOCATIONS = 20;

    static { 
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        // Don't wait for network connections for testing
        ManagedDownloader.NO_DELAY = true;
        
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
        return new TestSuite(DownloadTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void setUp() {   
        uploader1=new TestUploader("6346", 6346);
        uploader2=new TestUploader("6347", 6347);
        uploader3=new TestUploader("6348", 6348);
        uploader4=new TestUploader("6349", 6349);
        
        deleteAllFiles();
        
        dataDir.mkdirs();
        saveDir.mkdirs();
        
        try {
            SettingsManager.instance().setSaveDirectory(saveDir);        
        } catch(IOException e) {
            e.printStackTrace();
            fail( "cannot set save directory.");
        }
        
        //Pick random name for file.
        savedFile = new File( saveDir, savedFileName );
        savedFile.delete();
        SettingsManager.instance().setConnectionSpeed(1000);
        
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

    public void testLegacy() {
        String args[] = {};
        
        /*
        tOverlapCheckSpeed(5);
        cleanup();
        tOverlapCheckSpeed(25);
        cleanup();
        tOverlapCheckSpeed(125);
        cleanup(); */
    }
    
    /**
     * Tests to see if this 'i' is one of the testsToRun
     */
    private static void shouldRun(int myTest) {
        if ( testsToRun.length == 0 ) return;
       
        for(int i = 0; i < testsToRun.length; i++) {
            if ( testsToRun[i] == myTest )
                return;
        }
        
        fail("ignoring test.");
    }
    
    
    ////////////////////////// Test Cases //////////////////////////
    
    private static void tOverlapCheckSpeed(int rate) {
        RemoteFileDesc rfd=newRFDWithURN(6346, 100);
        debug("-Measuring time for download at rate "+rate+"... \n");
        uploader1.setRate(rate);
        long start1=System.currentTimeMillis();
        AlternateLocationCollection dcoll = 
			AlternateLocationCollection.createCollection(rfd.getSHA1Urn());
        try {
            HTTPDownloader downloader=new HTTPDownloader(rfd, savedFile,dcoll);
            VerifyingFile vf = new VerifyingFile(true);
            vf.open(savedFile,null);
            downloader.connectTCP(0);
            downloader.connectHTTP(0,TestFile.length(),true);
            downloader.doDownload(vf,false);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected exception: "+e);
        } 
        long elapsed1=System.currentTimeMillis()-start1;
        
        try{
            RandomAccessFile raf = new RandomAccessFile(savedFile,"rw");
            raf.seek(300);
            raf.write(65);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected exception: "+e);
        }
        
        long start2=System.currentTimeMillis();
        try {
            AlternateLocationCollection dcol = 
				AlternateLocationCollection.createCollection(rfd.getSHA1Urn());
            HTTPDownloader downloader=new HTTPDownloader(rfd, savedFile,dcol);
            VerifyingFile vf = new VerifyingFile(false);
            vf.open(savedFile,null);
            downloader.connectTCP(0);
            downloader.connectHTTP(0, TestFile.length(),true);
            downloader.doDownload(vf, false);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Unexpected exception: "+e);
        } 
        long elapsed2=System.currentTimeMillis()-start2;
        debug("  No check="+elapsed2+", check="+elapsed1 +"\n");
    }
    
    /**
     * Tests a basic download that does not swarm.
     */
    public void testSimpleDownload() {
        shouldRun(SIMPLE_DOWNLOAD);
        debug("-Testing non-swarmed download...");
        
        RemoteFileDesc rfd=newRFD(6346, 100);
        RemoteFileDesc[] rfds = {rfd};
        tGeneric(rfds);
    }
    
    public void testSimpleSwarm() {
        shouldRun(SIMPLE_SWARM);
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
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
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
        check(u1<TestFile.length()/2+FUDGE_FACTOR, "u1 did all the work");
        check(u2<TestFile.length()/2+FUDGE_FACTOR, "u2 did all the work");
    }


    public void testUnbalancedSwarm() {
        shouldRun(UNBALANCED_SWARM);
        debug("-Testing swarming from two unbalanced sources...");
        
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE/10);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
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

        check(u1<9*TestFile.length()/10+FUDGE_FACTOR*10, "u1 did all the work");
        check(u2<TestFile.length()/10+FUDGE_FACTOR, "u2 did all the work");
    }


    public void testSwarmWithInterrupt() {
        shouldRun(SWARM_WITH_INTERRUPT);
        debug("-Testing swarming from two sources (one broken)...");
        
        final int RATE=500;
        final int STOP_AFTER = TestFile.length()/4;       
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
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
        assertTrue("u1 did all the work", u1<TestFile.length()-STOP_AFTER+FUDGE_FACTOR);
        assertEquals("u2 did all the work", STOP_AFTER, u2);
    }


    public void testStealerInterrupted() {
        shouldRun(STEALER_INTERRUPTED);
        debug("-Testing unequal swarming with stealer dying...");
        
        final int RATE=500;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = 5*TestFile.length()/8;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE/10);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
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
        check(u1<TestFile.length()-STOP_AFTER+2*FUDGE_FACTOR,
              "u1 did all the work");
        check(u2==STOP_AFTER, "u2 did all the work");
    }



    public void testAddDownload() {
        shouldRun(ADD_DOWNLOAD);
        debug("-Testing addDownload (increases swarming)...");
        
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);

        Downloader download=null;
        try {
            //Start one location, wait a bit, then add another.
            download=dm.download(new RemoteFileDesc[] {rfd1}, false);
            ((ManagedDownloader)download).addDownload(rfd2);
        } catch (FileExistsException e) {
            e.printStackTrace();
            fail("FAILED: already exists");
        } catch (AlreadyDownloadingException e) {
            e.printStackTrace();
            fail("FAILED: already downloading");
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
            fail("FAILED: file not found (huh?)");
        }
        waitForComplete(download);
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

        check(u1<(TestFile.length()/2+(FUDGE_FACTOR)), "u1 did all the work");
        check(u2<(TestFile.length()/2+(FUDGE_FACTOR)), "u2 did all the work");
    }

    public void testStallingUploaderReplaced() {
        shouldRun(STALLING_UPLOADER_REPLACED);
        debug("-Testing download completion with stalling downloader...");
        
        //Throttle rate at 500KB/s to give opportunities for swarming.
        final int RATE=500;
        uploader1.setRate(0);//stalling uploader
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
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
    
    public void testOverlapCheckGreyNoStopOnCorrupt() {
        tOverlapCheckGrey(false);
    }
    
    public void testOverlapCheckGreyStopOnCorrupt() {
        callback.delCorrupt = true;
        tOverlapCheckGrey(true);
    }

    private void tOverlapCheckGrey(boolean deleteCorrupt) {
        shouldRun(OVERLAP_CHECK_GREY);
        debug("-Testing overlap checking from Grey area..." +
                         "stop when corrupt "+deleteCorrupt+" ");
                         
        final int RATE=500;
        uploader1.setRate(RATE);
        uploader2.setRate(RATE/100);
        uploader2.setCorruption(true);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
        
        Downloader download=null;
        try {
            //Start one location, wait a bit, then add another.
            download=dm.download(new RemoteFileDesc[] {rfd1,rfd2}, false);
        } catch (FileExistsException e) {
            e.printStackTrace();
            fail("FAILED: already exists");
        } catch (AlreadyDownloadingException e) {
            e.printStackTrace();
            fail("FAILED: already downloading");
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
            fail("FAILED: file not found (huh?)");
        }
        if(deleteCorrupt)
            waitForCorrupt(download);
        else
            waitForComplete(download);
        debug("passed"+"\n");//got here? Test passed
        //TODO: check IncompleteFileManager, disk
    }

    public void testOverlapCheckWhiteNoStopOnCorrupt() {
        tOverlapCheckWhite(false);
    }
    
    public void testOverlapCheckWhiteStopOnCorrupt() {
        callback.delCorrupt = true;        
        tOverlapCheckWhite(true);
    }

    private void tOverlapCheckWhite(boolean deleteCorrupt) {
        shouldRun(OVERLAP_CHECK_WHITE);
        debug("-Testing overlap checking from White area..."+
                         "stop when corrupt "+deleteCorrupt+" ");
                         
        final int RATE=500;
        uploader1.setCorruption(true);
        uploader1.stopAfter(TestFile.length()/8);//blinding fast
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
        
        Downloader download=null;
        try {
            //Start one location, wait a bit, then add another.
            download=dm.download(new RemoteFileDesc[] {rfd1,rfd2}, false);
        } catch (FileExistsException e) {
            e.printStackTrace();
            fail("FAILED: already exists");
        } catch (AlreadyDownloadingException e) {
            e.printStackTrace();
            fail("FAILED: already downloading");
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
            fail("FAILED: file not found (huh?)");
        }
        if(deleteCorrupt)
            waitForCorrupt(download);
        else
            waitForComplete(download);
        debug("passed"+"\n");//got here? Test passed
    }
    
    public void testMismatchedVerifyHashNoStopOnCorrupt() {
        tMismatchedVerifyHash(false);
    }
    
    public void testMismatchedVerifyHashStopOnCorrupt() {
        callback.delCorrupt = true;        
        tMismatchedVerifyHash(true);
    }

    private void tMismatchedVerifyHash(boolean deleteCorrupt) {
        shouldRun(MISMATCHED_VERIFY_HASH);
        debug("-Testing file declared corrupt, when hash of "+
                         "downloaded file mismatches bucket hash" +
                         "stop when corrupt "+ deleteCorrupt+" ");
                         
        final int RATE=100;
        uploader1.setRate(RATE);
        RemoteFileDesc rfd1 = newRFDWithURN(6346,100,
        "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB");
        Downloader download = null;
        try {
            download = dm.download(new RemoteFileDesc[] {rfd1}, false);        
        } catch (FileExistsException e) {
            e.printStackTrace();
            fail("FAILED: already exists");
        } catch (AlreadyDownloadingException e) {
            e.printStackTrace();
            fail("FAILED: already downloading");
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
            fail("FAILED: file not found (huh?)");
        }
        if(deleteCorrupt)
            waitForCorrupt(download);
        else
            waitForComplete(download);
        debug("passed"+"\n");//got here? Test passed
    }

    public void testSimpleAlternateLocations() {  
        shouldRun(SIMPLE_ALTERNATE_LOCATIONS);
        debug("-Testing AlternateLocation write...");
        
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1};

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection ashould = 
			AlternateLocationCollection.createCollection(rfd1.getSHA1Urn());
        try {
            ashould.addAlternateLocation(
                AlternateLocation.createAlternateLocation(rfd1.getUrl()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Couldn't setup test");
        }
        AlternateLocationCollection adiff = 
        ashould.diffAlternateLocationCollection(alt1); 

        URN sha1 = rfd1.getSHA1Urn();
        URN uSHA1 = uploader1.getReportedSHA1();
        boolean sha1Matches = 
        (sha1 != null && uSHA1 != null && sha1.equals(uSHA1));
        
        check(alt1.hasAlternateLocations(), "uploader didn't receive alt");
        check(!adiff.hasAlternateLocations(), "uploader got wrong alt");
        check(sha1Matches, "SHA1 test failed");
    }

    public void testTwoAlternateLocations() {  
        shouldRun(TWO_ALTERNATE_LOCATIONS);
        debug("-Testing Two AlternateLocations...");
        
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(6347, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection alt2 = uploader2.getAlternateLocations();
        AlternateLocationCollection ashould = 
			AlternateLocationCollection.createCollection(rfd1.getSHA1Urn());
        try {
            URL url1 = rfd1.getUrl();
            URL url2 = rfd2.getUrl();
            AlternateLocation al1 =
				AlternateLocation.createAlternateLocation(url1);
            AlternateLocation al2 =
				AlternateLocation.createAlternateLocation(url2);
            ashould.addAlternateLocation(al1);
            ashould.addAlternateLocation(al2);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Couldn't setup test");
        }
        AlternateLocationCollection adiff = 
            ashould.diffAlternateLocationCollection(alt1); 
        AlternateLocationCollection adiff2 = 
            alt1.diffAlternateLocationCollection(ashould); 
        
        check(alt1.hasAlternateLocations(), "uploader didn't receive alt");
        check(alt2.hasAlternateLocations(), "uploader didn't receive alt");
        check(!adiff.hasAlternateLocations(), "uploader got wrong alt");
        check(!adiff2.hasAlternateLocations(), "uploader got wrong alt");
    }

    public void testUploaderAlternateLocations() {  
        // This is a modification of simple swarming based on alternate location
        // for the second swarm
        shouldRun(UPLOADER_ALTERNATE_LOCATIONS);
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
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(6347, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.createCollection(rfd2.getSHA1Urn());
        try {
            URL url2 = rfd2.getUrl();
            AlternateLocation al2 =
				AlternateLocation.createAlternateLocation(url2);
            ualt.addAlternateLocation(al2);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Couldn't setup test");
        }
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
        check(u1<TestFile.length()/2+FUDGE_FACTOR, "u1 did all the work");
        check(u2<TestFile.length()/2+FUDGE_FACTOR, "u2 did all the work");
    }

    public void testWeirdAlternateLocations() {  
        shouldRun(WIERD_ALTERNATE_LOCATIONS);
        debug("-Testing AlternateLocation write...");
        
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1};


        //Prebuild some uploader alts
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.createCollection(
                HugeTestUtils.EQUAL_SHA1_LOCATIONS[0].getSHA1Urn());
        try {
            ualt.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[0]);
			//AlternateLocation.createAlternateLocation(
			//genericURL("http://211.211.211.211:6347/get/0/foobar.txt")));
            ualt.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[1]);
			//  AlternateLocation.createAlternateLocation(
			//      genericURL("http://211.211.211.211/get/0/foobar.txt")));
            ualt.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[2]);
			//  AlternateLocation.createAlternateLocation(
			//      genericURL("http://www.yahoo.com/foo/bar/foobar.txt")));
            ualt.addAlternateLocation(HugeTestUtils.EQUAL_SHA1_LOCATIONS[3]);
			//  AlternateLocation.createAlternateLocation(
			//      genericURL("http://40000000.400.400.400/get/99999999999999999999999999999/foobar.txt")));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Couldn't setup test");
        }
        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection ashould = 
			AlternateLocationCollection.createCollection(rfd1.getSHA1Urn());
        try {
            ashould.addAlternateLocation(
                AlternateLocation.createAlternateLocation(rfd1));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Couldn't setup test");
        }
        //ashould.addAlternateLocationCollection(ualt);
        AlternateLocationCollection adiff = 
            ashould.diffAlternateLocationCollection(alt1); 

        AlternateLocationCollection adiff2 = 
            alt1.diffAlternateLocationCollection(ashould); 
        
        assertTrue("uploader didn't receive alt", alt1.hasAlternateLocations());
        assertTrue("uploader got extra alts", !adiff.hasAlternateLocations());
        assertTrue("uploader didn't get all alts", !adiff2.hasAlternateLocations());
    }

    public void testStealerInterruptedWithAlternate() {
        shouldRun(STEALER_INTERRUPTED_WITH_ALTERNATE);
        debug("-Testing swarming of rfds ignoring alt ...");
        
        int capacity=SettingsManager.instance().getConnectionSpeed();
        SettingsManager.instance().setConnectionSpeed(
                                                      SpeedConstants.MODEM_SPEED_INT);
        final int RATE=200;
        //second half of file + 1/8 of the file
        final int STOP_AFTER = 1*TestFile.length()/10;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader1.stopAfter(STOP_AFTER);
        uploader2.setRate(RATE);
        uploader3.setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(6347, 100, TestFile.hash().toString());
        RemoteFileDesc rfd3=newRFDWithURN(6348, 100, TestFile.hash().toString());
        RemoteFileDesc rfd4=newRFDWithURN(6349, 100, TestFile.hash().toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2,rfd3};

        //Prebuild an uploader alt in lieu of rdf4
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.createCollection(rfd4.getSHA1Urn());
        try {
            AlternateLocation al4 =
				AlternateLocation.createAlternateLocation(rfd4);
            ualt.addAlternateLocation(al4);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Couldn't setup test");
        }
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
        assertTrue("u2 did no work", u2>0);
        assertTrue("u3 did no work", u3>0);
        assertEquals("u4 was used", 0, u4);
        SettingsManager.instance().setConnectionSpeed(capacity);
    }

	// no longer in use, as alternate locations should always have SHA1s
	/*
    private static void tTwoAlternatesButOneWithNoSHA1() {  
        debug("-Testing Two Alternates but one with no sha1...");
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, TestFile.hash().toString());
        RemoteFileDesc rfd2=newRFDWithURN(6347, 100); // No SHA1
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

    public void testUpdateWhiteWithFailingFirstUploader() {
        shouldRun(UPDATE_WHITE_WITH_FAILING_FIRST_UPLOADER);
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
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        tGeneric(rfds);        

        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        debug("\tu1: "+u1+"\n");
        debug("\tu2: "+u2+"\n");
        debug("\tTotal: "+(u1+u2)+"\n");
        debug("passed \n");
    }

    public void testQueuedDownloader() {
        shouldRun(QUEUED_DOWNLOADER);
        debug("-Testing queued downloader. \n");
        
        uploader1.setQueue(true);
        RemoteFileDesc rfd1 = newRFD(6346, 100);
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
    public void testAlternateLocationsExchangedWithBusy() {
        //tests that a downloader reads alternate locations from the
        //uploader even if it receives a 503 from the uploader.
        shouldRun(ALTERNATE_LOCATIONS_EXCHANGED_WITH_BUSY);
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
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100);
        RemoteFileDesc rfd2=newRFDWithURN(6347, 100);
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection ualt = 
			AlternateLocationCollection.createCollection(rfd1.getSHA1Urn());
        try {
            AlternateLocation al2 =
				AlternateLocation.createAlternateLocation(rfd2);
            ualt.addAlternateLocation(al2);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Couldn't setup test");
        }
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
        assertTrue("u2 did all the work", u2<TestFile.length()+FUDGE_FACTOR);
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
        QueryReply qr=new QueryReply(guid, (byte)5, 6346,
                                     localhost, Integer.MAX_VALUE,
                                     responses, new byte[16]);
        responses=new Response[1];
        responses[0]=new Response(0l, file.length(), file.getName());
        qr=new QueryReply(guid, (byte)5, 6347,
                          localhost, Integer.MAX_VALUE,
                          responses, new byte[16]);
        callback.handleQueryReply(qr);
    }
    */


    ////////////////////////// Helping Code ///////////////////////////
    
    /**
     * Performs a generic download of the file specified in <tt>rfds</tt>.
     */
    private static void tGeneric(RemoteFileDesc[] rfds) {
        Downloader download=null;
        try {
            download=dm.download(rfds, false);
        } catch (FileExistsException e) {
            e.printStackTrace();
            fail("FAILED: already exists");
        } catch (AlreadyDownloadingException e) {
            e.printStackTrace();
            fail("FAILED: already downloading");
            return;
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
            fail("FAILED: file not found (huh?)");
        }
        waitForComplete(download);
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
            e.printStackTrace();
            fail("Generic URL creation failed");
        }  
        return theURL;
    }


    private static RemoteFileDesc newRFD(int port, int speed) {
        return new RemoteFileDesc("127.0.0.1", port,
                                  0, savedFile.getName(),
                                  TestFile.length(), new byte[16],
                                  speed, false, 4, false, null, null);
    }

	private static RemoteFileDesc newRFDWithURN(int port, int speed) {
		return newRFDWithURN(port, speed, null);
	}

    private static RemoteFileDesc newRFDWithURN(int port, int speed, 
                                                String urn) {
        com.sun.java.util.collections.Set set = 
			new com.sun.java.util.collections.HashSet();
        try {
            if (urn == null)
                set.add(URN.createSHA1Urn("urn:sha1:MTSUIEFABDVUDXZMJEBQWNI6RVYHTNIJ"));
            else
                set.add(URN.createSHA1Urn(urn));
        } catch(Exception e) {
            e.printStackTrace();
            fail("SHA1 not created for: "+savedFile);
        }
        return new RemoteFileDesc("127.0.0.1", port,
                                  0, savedFile.getName(),
                                  TestFile.length(), new byte[16],
                                  speed, false, 4, false, null, set);
    }

    /** Waits for the given download to complete. */
    private static void waitForComplete(Downloader d) {
        //Current implementation: polling (ugh)
        while (d.getState()!=Downloader.COMPLETE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        }
    }


    private static void waitForCorrupt(Downloader d) {
        //Current implementation: polling (ugh)
        while (d.getState()!=Downloader.CORRUPT_FILE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
        }
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
            ioe.printStackTrace();
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

    private static void check(boolean ok, String message) {
        assertTrue(message,ok);
    }

    private static final boolean DEBUG = false;
    
    static void debug(String message) {
        if(DEBUG) 
            System.out.print(message);
    }

    /** Cleans up the complete file */
    private static void cleanup() {
        uploader1.reset();
        uploader2.reset();
        uploader3.reset();
        uploader4.reset();
        boolean deleted=savedFile.delete();
        if (!deleted)
            debug("WARNING: couldn't delete "+savedFile+"\n");        
    }
}






