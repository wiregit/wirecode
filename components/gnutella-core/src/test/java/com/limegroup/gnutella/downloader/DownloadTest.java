package com.limegroup.gnutella.downloader;

import java.io.*;
import java.util.*;
import java.net.URL;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.gui.*;
import com.limegroup.gnutella.gui.search.*;
import javax.swing.JOptionPane;
import junit.framework.*;


public class DownloadTest extends TestCase {
    static File directory=new File(".");
    static File file;
    static TestUploader uploader1=new TestUploader("6346", 6346);
    static TestUploader uploader2=new TestUploader("6347", 6347);
    static TestUploader uploader3=new TestUploader("6348", 6348);
    static TestUploader uploader4=new TestUploader("6349", 6349);
	static final DownloadManager dm = new DownloadManager();
	static final ActivityCallbackStub callback = new ActivityCallbackStub();	

    static URN testHash = null;
    static File testFile = null;
    
    public DownloadTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DownloadTest.class);
    }

    public void tearDown() {
        //1. Kill all the upload threads.
        uploader1.stopThread();
        uploader2.stopThread();
        uploader3.stopThread();
        uploader4.stopThread();
        //2. Delete the incomplete directory and files in it
        File[] files = directory.listFiles();
        for(int i=0; i< files.length; i++) {
            if(files[i].isDirectory())
                deleteIncompleteDirectory(files[i]);
            else if(files[i].getName().equals("DownloadTester2834343.out"))
                files[i].delete();
        }
    }
    
    static void  deleteIncompleteDirectory(File dir) {
        if(!dir.getName().equalsIgnoreCase("incomplete"))
            return;
        File[] files = dir.listFiles();
        for(int i=0; i< files.length; i++) 
            files[i].delete();
        //now delete the directory
        dir.delete();
    }

    public void testLegacy() {
        String args[] = {};
        try {
            testFile = new File("test.txt");
            RandomAccessFile raf = new RandomAccessFile(testFile,"rw");
            for(int i=0; i<TestFile.length(); i++)
                raf.writeByte(TestFile.getByte(i));
            raf.close();
            testHash = URN.createSHA1Urn(testFile);
        } catch (IOException iox) {
            debug("unable to get hash of test file...exiting \n");
            System.exit(1);
        } catch (InterruptedException ignored){}

        try {
            //Pick random name for file.  Delete if already exists from previous
            //run.
            file=new File("DownloadTester2834343.out");
            file.delete();
            SettingsManager.instance().setSaveDirectory(directory);
            SettingsManager.instance().setConnectionSpeed(1000);
        } catch (IOException e) {
            debug("Couldn't create temp file. \n");
            System.exit(1);
        }
		RouterService rs = RouterService.instance();
        //RouterService rs=new RouterService(null, null, null, null);
        //dm.initialize(callback, new MessageRouterStub(), 
		//            null, new FileManagerStub());
        //dm.postGuiInit(rs);
        
        SimpleTimer timer = new SimpleTimer(true);
        Runnable click = new Runnable() {
            public void run() {
                dm.measureBandwidth();
            }
        };
        timer.schedule(click,0,SupernodeAssigner.TIMER_DELAY);
        
//          tOverlapCheckSpeed(5);
//          cleanup();
//          tOverlapCheckSpeed(25);
//          cleanup();
//          tOverlapCheckSpeed(125);
//          cleanup();
        if(args.length == 0 || args[0].equals("0")) {
            tSimpleDownload();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("1")) {
            tSimpleSwarm();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("2")) {
            tUnbalancedSwarm();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("3")) {
            tSwarmWithInterrupt();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("4")) {
            tStealerInterrupted();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("5")) {
            tAddDownload();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("6")) {
            tStallingUploaderReplaced();
            cleanup();
        }
        //test corruption and ignore it
        //Note: callback.delCorrupt = false by default.
        if(args.length == 0 || args[0].equals("7")) {
            tOverlapCheckGrey(false);//wait for complete
            cleanup();
        }
        if(args.length == 0 || args[0].equals("8")) {
            tOverlapCheckWhite(false);//wait for complete
            cleanup();
        }
        if(args.length == 0 || args[0].equals("9")) {
            tMismatchedVerifyHash(false);
            cleanup();
        }
        //now test corruption without ignoring it
        callback.delCorrupt = true;
        if(args.length == 0 || args[0].equals("10")) {
            tOverlapCheckGrey(true);//wait for corrupt
            cleanup();
        }
        if(args.length == 0 || args[0].equals("11")) {
            tOverlapCheckWhite(true);//wait for corrupt
            cleanup();
        }
        if(args.length == 0 || args[0].equals("12")) {
            tMismatchedVerifyHash(true);
            cleanup(); 
        }
        if(args.length == 0 || args[0].equals("13")) {
            tSimpleAlternateLocations();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("14")) {
            tTwoAlternateLocations();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("15")) {
            tUploaderAlternateLocations();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("16")) {
            tWeirdAlternateLocations();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("17")) {
            tStealerInterruptedWithAlternate();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("18")) {
            tTwoAlternatesButOneWithNoSHA1();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("19")) {
            tUpdateWhiteWithFailingFirstUploader();
            cleanup();
        }
    }
    
    
    ////////////////////////// Test Cases //////////////////////////
    
    private static void tOverlapCheckSpeed(int rate) {
        RemoteFileDesc rfd=newRFD(6346, 100);
        debug("-Measuring time for download at rate "+rate+"... \n");
        uploader1.setRate(rate);
        long start1=System.currentTimeMillis();
        AlternateLocationCollection dcoll = new AlternateLocationCollection();
        try {
            HTTPDownloader downloader=new HTTPDownloader(rfd, file,dcoll);
            VerifyingFile vf = new VerifyingFile(true);
            vf.open(file,null);
            downloader.connectTCP(0);
            downloader.connectHTTP(0,TestFile.length());
            downloader.doDownload(vf);
        } catch (IOException e) {
            assertTrue("Unexpected exception: "+e,false);
        } 
        long elapsed1=System.currentTimeMillis()-start1;
        
        try{
            RandomAccessFile raf = new RandomAccessFile(file,"rw");
            raf.seek(300);
            raf.write(65);
        } catch (IOException io) {
            assertTrue("Error programing a spook", false);
        }
        
        long start2=System.currentTimeMillis();
        try {
            AlternateLocationCollection dcol=new AlternateLocationCollection();
            HTTPDownloader downloader=new HTTPDownloader(rfd, file,dcol);
            VerifyingFile vf = new VerifyingFile(false);
            vf.open(file,null);
            downloader.connectTCP(0);
            downloader.connectHTTP(0, TestFile.length());
            downloader.doDownload(vf);
        } catch (IOException e) {
            assertTrue("Unexpected exception: "+e, false);
        } 
        long elapsed2=System.currentTimeMillis()-start2;
        debug("  No check="+elapsed2+", check="+elapsed1 +"\n");
    }
    
    
    private static void tSimpleDownload() {
        debug("-Testing non-swarmed download...");
        RemoteFileDesc rfd=newRFD(6346, 100);
        RemoteFileDesc[] rfds = {rfd};
        tGeneric(rfds);
    }
    
    private static void tSimpleSwarm() {
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


    private static void tUnbalancedSwarm() {
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


    private static void tSwarmWithInterrupt() {
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
        check(u1<TestFile.length()-STOP_AFTER+FUDGE_FACTOR, 
              "u1 did all the work");
        check(u2==STOP_AFTER, "u2 did all the work ("+u2+")");
    }


    private static void tStealerInterrupted() {
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



    private static void tAddDownload() {
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
            download=dm.getFiles(new RemoteFileDesc[] {rfd1}, false);
            ((ManagedDownloader)download).addDownload(rfd2);
        } catch (FileExistsException e) {
            check(false, "FAILED: already exists");
            return;
        } catch (AlreadyDownloadingException e) {
            check(false, "FAILED: already downloading");
            return;
        } catch (java.io.FileNotFoundException e) {
            check(false, "FAILED: file not found (huh?)");
            return;
        }
        waitForComplete(download);
        if (isComplete())
            debug("pass"+"\n");
        else
            check(false, "FAILED: complete corrupt");

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

    private static void tStallingUploaderReplaced() {
        debug
        ("-Testing download completion with stalling downloader...");
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

    private static void tOverlapCheckGrey(boolean deleteCorrupt) {
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
            download=dm.getFiles(new RemoteFileDesc[] {rfd1,rfd2}, false);
        } catch (FileExistsException e) {
            check(false, "FAILED: already exists");
            return;
        } catch (AlreadyDownloadingException e) {
            check(false, "FAILED: already downloading");
            return;
        } catch (java.io.FileNotFoundException e) {
            check(false, "FAILED: file not found (huh?)");
            return;
        }
        if(deleteCorrupt)
            waitForCorrupt(download);
        else
            waitForComplete(download);
        if(deleteCorrupt) {//we are not interrupted if we choose to keep going
            check(download.getAmountRead()<2*TestFile.length()/3, 
                  "Didn't interrupt soon enough: "+download.getAmountRead());
        }
        debug("passed"+"\n");//got here? Test passed
        //TODO: check IncompleteFileManager, disk
    }


    private static void tOverlapCheckWhite(boolean deleteCorrupt) {
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
            download=dm.getFiles(new RemoteFileDesc[] {rfd1,rfd2}, false);
        } catch (FileExistsException e) {
            check(false, "FAILED: already exists");
            return;
        } catch (AlreadyDownloadingException e) {
            check(false, "FAILED: already downloading");
            return;
        } catch (java.io.FileNotFoundException e) {
            check(false, "FAILED: file not found (huh?)");
            return;
        }
        if(deleteCorrupt)
            waitForCorrupt(download);
        else
            waitForComplete(download);
        debug("passed"+"\n");//got here? Test passed
    }

    private static void tMismatchedVerifyHash(boolean deleteCorrupt) {
        debug("-Testing file declared corrupt, when hash of "+
                         "downloaded file mismatches bucket hash" +
                         "stop when corrupt "+ deleteCorrupt+" ");
        final int RATE=500;
        RemoteFileDesc rfd1 = newRFDWithURN(6346,100,null);
        Downloader download = null;
        try {
            download = dm.getFiles(new RemoteFileDesc[] {rfd1}, false);        
        } catch (FileExistsException e) {
            check(false, "FAILED: already exists");
            return;
        } catch (AlreadyDownloadingException e) {
            check(false, "FAILED: already downloading");
            return;
        } catch (java.io.FileNotFoundException e) {
            check(false, "FAILED: file not found (huh?)");
            return;
        }
        if(deleteCorrupt)
            waitForCorrupt(download);
        else
            waitForComplete(download);
        debug("passed"+"\n");//got here? Test passed
    }

    private static void tSimpleAlternateLocations() {  
        debug("-Testing AlternateLocation write...");
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, testHash.toString());
        RemoteFileDesc[] rfds = {rfd1};

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection ashould = new AlternateLocationCollection();
        try {
            ashould.addAlternateLocation(
                                         AlternateLocation.createAlternateLocation(rfdURL(rfd1)));
        } catch (Exception e) {
            check(false, "Couldn't setup test");
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

    private static void tTwoAlternateLocations() {  
        debug("-Testing Two AlternateLocations...");
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, testHash.toString());
        RemoteFileDesc rfd2=newRFDWithURN(6347, 100, testHash.toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection alt2 = uploader2.getAlternateLocations();
        AlternateLocationCollection ashould = new AlternateLocationCollection();
        try {
            URL url1 =   rfdURL(rfd1);
            URL url2 =   rfdURL(rfd2);
            AlternateLocation al1 =
            AlternateLocation.createAlternateLocation(url1);
            AlternateLocation al2 =
            AlternateLocation.createAlternateLocation(url2);
            ashould.addAlternateLocation(al1);
            ashould.addAlternateLocation(al2);
        } catch (Exception e) {
            check(false, "Couldn't setup test");
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

    private static void tUploaderAlternateLocations() {  
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
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, testHash.toString());
        RemoteFileDesc rfd2=newRFDWithURN(6347, 100, testHash.toString());
        RemoteFileDesc[] rfds = {rfd1};

        //Prebuild an uploader alts in lieu of rdf2
        AlternateLocationCollection ualt = new AlternateLocationCollection();
        try {
            URL url2 =   rfdURL(rfd2);
            AlternateLocation al2 =
            AlternateLocation.createAlternateLocation(url2);
            ualt.addAlternateLocation(al2);
        } catch (Exception e) {
            check(false, "Couldn't setup test");
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

    private static void tWeirdAlternateLocations() {  
        debug("-Testing AlternateLocation write...");
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, testHash.toString());
        RemoteFileDesc[] rfds = {rfd1};


        //Prebuild some uploader alts
        AlternateLocationCollection ualt = new AlternateLocationCollection();
        try {
            ualt.addAlternateLocation(
                                      AlternateLocation.createAlternateLocation(
                                                                                genericURL("http://211.211.211.211:6347/get/0/foobar.txt")));
            ualt.addAlternateLocation(
                                      AlternateLocation.createAlternateLocation(
                                                                                genericURL("http://211.211.211.211/get/0/foobar.txt")));
            ualt.addAlternateLocation(
                                      AlternateLocation.createAlternateLocation(
                                                                                genericURL("http://www.yahoo.com/foo/bar/foobar.txt")));
            ualt.addAlternateLocation(
                                      AlternateLocation.createAlternateLocation(
                                                                                genericURL("http://40000000.400.400.400/get/99999999999999999999999999999/foobar.txt")));
        } catch (Exception e) {
            check(false, "Couldn't setup test");
        }
        uploader1.setAlternateLocations(ualt);

        tGeneric(rfds);

        //Prepare to check the alternate locations
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection ashould = new AlternateLocationCollection();
        try {
            ashould.addAlternateLocation(
                                         AlternateLocation.createAlternateLocation(rfdURL(rfd1)));
        } catch (Exception e) {
            check(false, "Couldn't setup test");
        }
        //ashould.addAlternateLocationCollection(ualt);
        AlternateLocationCollection adiff = 
        ashould.diffAlternateLocationCollection(alt1); 

        AlternateLocationCollection adiff2 = 
        alt1.diffAlternateLocationCollection(ashould); 
        
        check(alt1.hasAlternateLocations(), "uploader didn't receive alt");
        check(!adiff.hasAlternateLocations(), "uploader got extra alts");
        check(!adiff2.hasAlternateLocations(), "uploader didn't get all alts");
    }

    private static void tStealerInterruptedWithAlternate() {
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
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, testHash.toString());
        RemoteFileDesc rfd2=newRFDWithURN(6347, 100, testHash.toString());
        RemoteFileDesc rfd3=newRFDWithURN(6348, 100, testHash.toString());
        RemoteFileDesc rfd4=newRFDWithURN(6349, 100, testHash.toString());
        RemoteFileDesc[] rfds = {rfd1,rfd2,rfd3};

        //Prebuild an uploader alt in lieu of rdf4
        AlternateLocationCollection ualt = new AlternateLocationCollection();
        try {
            URL url4 =   rfdURL(rfd4);
            AlternateLocation al4 =
            AlternateLocation.createAlternateLocation(url4);
            ualt.addAlternateLocation(al4);
        } catch (Exception e) {
            check(false, "Couldn't setup test");
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
        //be equal, because the uploaders are stated at different times.
        check(u1==STOP_AFTER, "u1 did all the work");
        check(u2>0, "u2 did no work");
        check(u3>0, "u3 did no work");
        check(u4==0, "u4 was used");
        SettingsManager.instance().setConnectionSpeed(capacity);
    }

    private static void tTwoAlternatesButOneWithNoSHA1() {  
        debug("-Testing Two Alternates but one with no sha1...");
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, testHash.toString());
        RemoteFileDesc rfd2=newRFD(6347, 100); // No SHA1
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Prepare to check the alternate location - second won't be there
        //Note: adiff should be blank
        AlternateLocationCollection alt1 = uploader1.getAlternateLocations();
        AlternateLocationCollection alt2 = uploader2.getAlternateLocations();
        AlternateLocationCollection ashould = new AlternateLocationCollection();
        try {
            URL url1 =   rfdURL(rfd1);
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

    private static void tUpdateWhiteWithFailingFirstUploader() {
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
        assertTrue("Search didn't happen", guid!=null);

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


    ////////////////////////// Helping Code ///////////////////////////
    
    private static void tGeneric(RemoteFileDesc[] rfds) {
        Downloader download=null;
        try {
            download=dm.getFiles(rfds, false);
        } catch (FileExistsException e) {
            check(false, "FAILED: already exists");
            return;
        } catch (AlreadyDownloadingException e) {
            check(false, "FAILED: already downloading");
            return;
        } catch (java.io.FileNotFoundException e) {
            check(false, "FAILED: file not found (huh?)");
            return;
        }
        waitForComplete(download);
        if (isComplete())
            debug("pass"+"\n");
        else
            check(false, "FAILED: complete corrupt");
    }


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

    private static URL genericURL(String url) {
        URL    theURL = null;
        try {
            theURL = new URL(url);
        } catch( Exception e ) {
            check(false, "Generic URL creation failed");
        }  
        return theURL;
    }


    private static RemoteFileDesc newRFD(int port, int speed) {
        return new RemoteFileDesc("127.0.0.1", port,
                                  0, file.getName(),
                                  TestFile.length(), new byte[16],
                                  speed, false, 4, false, null, null);
    }

    private static RemoteFileDesc newRFDWithURN(int port, int speed, 
                                                String urn) {
        com.sun.java.util.collections.Set set = 
        new com.sun.java.util.collections.HashSet();
        try {
            if (urn == null)
                set.add(URN.createSHA1Urn(
                                 "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"));
            else
                set.add(URN.createSHA1Urn(urn));
        } catch(Exception e) {
            debug("SHA1 not created for :"+file);
        }
        return new RemoteFileDesc("127.0.0.1", port,
                                  0, file.getName(),
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
        if (file.length()!=TestFile.length()) {
            debug("File too small"+file.length()+"\n");
            return false;
        }
        FileInputStream stream=null;
        try {
            stream = new FileInputStream(file);
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

    private static final boolean DEBUG=false;
    
    static void debug(String message) {
        if(DEBUG) 
            System.out.print(message);
    }

    /** Cleans up the complete file */
    private static void cleanup() {
        boolean deleted=file.delete();
        if (!deleted)
            debug("WARNING: couldn't delete "+file+"\n");
        testFile.delete();
        uploader1.reset();
        uploader2.reset();
        uploader3.reset();
        uploader4.reset();
    }
}
