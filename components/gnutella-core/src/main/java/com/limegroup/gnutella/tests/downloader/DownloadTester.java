package com.limegroup.gnutella.tests.downloader;

import java.io.*;
import java.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.tests.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.tests.stubs.*;
import com.limegroup.gnutella.gui.*;
import com.limegroup.gnutella.gui.search.*;
import javax.swing.JOptionPane;


public class DownloadTester {
    static File directory=new File(".");
    static File file;

    static TestUploader uploader1=new TestUploader("6346", 6346);
    static TestUploader uploader2=new TestUploader("6347", 6347);
    static TestUploader uploader3=new TestUploader("6348", 6348);
    static final DownloadManager dm = new DownloadManager();

    public static void main(String[] args) {
        try {
            //Pick random name for file.  Delete if already exists from previous
            //run.
            file=new File("DownloadTester2834343.out");
            file.delete();
            SettingsManager.instance().setSaveDirectory(directory);
            SettingsManager.instance().setConnectionSpeed(1000);
        } catch (IOException e) {
            System.err.println("Couldn't create temp file.");
            System.exit(1);
        }
        RouterService rs=new RouterService(null, null, null, null);
        dm.initialize(new ActivityCallbackStub(), new MessageRouterStub(), 
                      null, new FileManagerStub(), rs);

        SimpleTimer timer = new SimpleTimer(true);
        Runnable click = new Runnable() {
                public void run() {
                    dm.measureBandwidth();
                }
            };
        timer.schedule(click,0,SupernodeAssigner.TIMER_DELAY);

//         testOverlapCheckSpeed(5);
//         cleanup();
//         testOverlapCheckSpeed(25);
//         cleanup();
//         testOverlapCheckSpeed(125);
//         cleanup();

//          testSimpleDownload();
//          cleanup();
//          testSimpleSwarm();
//          cleanup();
//          testUnbalancedSwarm();
//          cleanup();
        testSwarmWithInterrupt();
        cleanup();
        testStealerInterrupted();
        cleanup();
        testAddDownload();
        cleanup();
        testStallingUploaderReplaced();
        cleanup();
        
        testOverlapCheckGrey();
        cleanup();
        testOverlapCheckWhite();
        cleanup();
    }


    ////////////////////////// Test Cases //////////////////////////

    private static void testOverlapCheckSpeed(int rate) {
        RemoteFileDesc rfd=newRFD(6346, 100);
        System.out.println("-Measuring time for download at rate "+rate+"...");
        uploader1.setRate(rate);
        long start1=System.currentTimeMillis();
        try {
            HTTPDownloader downloader=new HTTPDownloader(
                rfd, file);
            downloader.connectTCP(0);
            downloader.connectHTTP(0,TestFile.length());
            downloader.doDownload(true);        
        } catch (IOException e) {
            Assert.that(false, "Unexpected exception: "+e);
        } catch (OverlapMismatchException e) {
            Assert.that(false, "Unexpected mismatch");
        }
        long elapsed1=System.currentTimeMillis()-start1;
        
//         try{
//             RandomAccessFile raf = new RandomAccessFile(file,"rw");
//             raf.seek(300);
//             raf.write(65);
//         } catch (IOException io) {
//             Assert.that(false, "Error programing a spook");
//         }
        
        long start2=System.currentTimeMillis();
        try {
            HTTPDownloader downloader=new HTTPDownloader(
                rfd, file);
            downloader.connectTCP(0);
            downloader.connectHTTP(0, TestFile.length());
            downloader.doDownload(false);        
        } catch (IOException e) {
            Assert.that(false, "Unexpected exception: "+e);
        } catch (OverlapMismatchException e) {
            Assert.that(false, "Unexpected mismatch");
        }
        long elapsed2=System.currentTimeMillis()-start2;
        System.out.println("  No check="+elapsed2+", check="+elapsed1);
    }


    private static void testSimpleDownload() {
        System.out.print("-Testing non-swarmed download...");
        RemoteFileDesc rfd=newRFD(6346, 100);
        RemoteFileDesc[] rfds = {rfd};
        testGeneric(rfds);
    }

    private static void testSimpleSwarm() {
        System.out.print("-Testing swarming from two sources...");
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

        testGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        System.out.println("\tu1: "+u1);
        System.out.println("\tu2: "+u2);
        System.out.println("\tTotal: "+(u1+u2));

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        check(u1<TestFile.length()/2+FUDGE_FACTOR, "u1 did all the work");
        check(u2<TestFile.length()/2+FUDGE_FACTOR, "u2 did all the work");
    }


    private static void testUnbalancedSwarm() {
        System.out.print("-Testing swarming from two unbalanced sources...");
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE/10);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        testGeneric(rfds);

        //Make sure there weren't too many overlapping regions.  uploader1
        //should transfer 2/3 of the file and uploader2 should transfer 1/3 of
        //the file.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        System.out.println("\tu1: "+u1);
        System.out.println("\tu2: "+u2);
        System.out.println("\tTotal: "+(u1+u2));

        check(u1<9*TestFile.length()/10+FUDGE_FACTOR*10, "u1 did all the work");
        check(u2<TestFile.length()/10+FUDGE_FACTOR, "u2 did all the work");
    }


    private static void testSwarmWithInterrupt() {
        System.out.print("-Testing swarming from two sources (one broken)...");
        final int RATE=500;
        final int STOP_AFTER = TestFile.length()/4;       
        final int FUDGE_FACTOR=RATE*1024;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        uploader2.stopAfter(STOP_AFTER);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        testGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        System.out.println("\tu1: "+u1);
        System.out.println("\tu2: "+u2);
        System.out.println("\tTotal: "+(u1+u2));

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        check(u1<TestFile.length()-STOP_AFTER+FUDGE_FACTOR, 
              "u1 did all the work");
        check(u2==STOP_AFTER, "u2 did all the work");
    }


    private static void testStealerInterrupted() {
        System.out.print("-Testing unequal swarming with stealer dying...");
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

        testGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        System.out.println("\tu1: "+u1);
        System.out.println("\tu2: "+u2);
        System.out.println("\tTotal: "+(u1+u2));

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        check(u1<TestFile.length()-STOP_AFTER+2*FUDGE_FACTOR,
              "u1 did all the work");
        check(u2==STOP_AFTER, "u2 did all the work");
    }



    private static void testAddDownload() {
        System.out.print("-Testing addDownload (increases swarming)...");
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
            System.out.println("pass");
        else
            check(false, "FAILED: complete corrupt");

        //Make sure there weren't too many overlapping regions. Each upload
        //should do roughly half the work.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        System.out.println("\tu1: "+u1);
        System.out.println("\tu2: "+u2);
        System.out.println("\tTotal: "+(u1+u2));

        check(u1<(TestFile.length()/2+(FUDGE_FACTOR)), "u1 did all the work");
        check(u2<(TestFile.length()/2+(FUDGE_FACTOR)), "u2 did all the work");
    }

    private static void testStallingUploaderReplaced() {
        System.out.print
        ("-Testing download completion with stalling downloader...");
        //Throttle rate at 500KB/s to give opportunities for swarming.
        final int RATE=500;
        uploader1.setRate(0);//stalling uploader
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        testGeneric(rfds);


        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        System.out.println("\tu1: "+u1);
        System.out.println("\tu2: "+u2);
        System.out.println("\tTotal: "+(u1+u2));

        //Note: The amount downloaded from each uploader will not 

        System.out.println("passed");//file downloaded? passed
    }

    private static void testOverlapCheckGrey() {
        System.out.print("-Testing overlap checking from Grey area...");
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
        waitForCorrupt(download);
        check(download.getAmountRead()<2*TestFile.length()/3, 
              "Didn't interrupt soon enough: "+download.getAmountRead());
        System.out.println("passed");//got here? Test passed
        //TODO: check IncompleteFileManager, disk
    }


    private static void testOverlapCheckWhite() {
        System.out.print("-Testing overlap checking from White area...");
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
        waitForCorrupt(download);
        System.out.println("passed");//got here? Test passed
    }

//     private static void testGUI() {
//         final int RATE=500;
//         uploader1.setCorruption(true);
//         uploader1.setRate(RATE);
//         uploader2.setRate(RATE);

//         //Bring up application.  Make sure disconnected.
//         System.out.println("Bringing up GUI...");
//         com.limegroup.gnutella.gui.Main.main(new String[0]);
//         RouterService router=GUIMediator.instance().getRouter();
//         router.disconnect();
        
//         //Do dummy search
//         byte[] guid=GUIMediator.instance().triggerSearch(file.getName());
//         Assert.that(guid!=null, "Search didn't happen");

//         //Add normal dummy result
//         ActivityCallback callback=router.getActivityCallback();        
//         byte[] localhost={(byte)127, (byte)0, (byte)0, (byte)1};
//         Response[] responses=new Response[1];
//         responses[0]=new Response(0l, file.length(), file.getName());
//         QueryReply qr=new QueryReply(guid, (byte)5, 6346,
//                                      localhost, Integer.MAX_VALUE,
//                                      responses, new byte[16]);
//         responses=new Response[1];
//         responses[0]=new Response(0l, file.length(), file.getName());
//         qr=new QueryReply(guid, (byte)5, 6347,
//                           localhost, Integer.MAX_VALUE,
//                           responses, new byte[16]);
//         callback.handleQueryReply(qr);
//     }

    ////////////////////////// Helping Code ///////////////////////////
    
    private static void testGeneric(RemoteFileDesc[] rfds) {
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
            System.out.println("pass");
        else
            check(false, "FAILED: complete corrupt");
    }



    private static RemoteFileDesc newRFD(int port, int speed) {
        return new RemoteFileDesc("127.0.0.1", port,
                                  0, file.getName(),
                                  TestFile.length(), new byte[16],
                                  speed, false, 4);
    }

    /** Waits for the given download to complete. */
    private static void waitForComplete(Downloader d) {
        //Current implementation: polling (ugh)
        while (d.getState()!=Downloader.COMPLETE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
            dm.writeSnapshot();  //Try to mess up downloaders
        }
    }


    private static void waitForCorrupt(Downloader d) {
        //Current implementation: polling (ugh)
        while (d.getState()!=Downloader.CORRUPT_FILE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) { }
            dm.writeSnapshot();  //Try to mess up downloaders
        }
    }

    /** Returns true if the complete file exists and is complete */
    private static boolean isComplete() {
        if (file.length()!=TestFile.length()) {
            System.out.println("File too small"+file.length());
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
                    System.out.println("Bad byte at "+i);
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
        if (! ok) {
            System.out.println("FAILED: "+message);
        }
    }

    /** Cleans up the complete file */
    private static void cleanup() {
        boolean deleted=file.delete();
        if (!deleted)
            System.err.println("WARNING: couldn't delete "+file);
        uploader1.reset();
        uploader2.reset();
        uploader3.reset();
    }
}
