package com.limegroup.gnutella.tests.downloader;

import java.io.*;
import java.util.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.tests.*;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.tests.stubs.*;

public class DownloadTester {
    static File directory=new File(".");
    static File file;

    static TestUploader uploader1=new TestUploader("6346", 6346);
    static TestUploader uploader2=new TestUploader("6347", 6347);
    static TestUploader uploader3=new TestUploader("6348", 6348);
    static DownloadManager dm;

    public static void main(String[] args) {
        try {
            //Pick random name for file.  Delete if already exists from previous
            //run.
            file=new File("DownloadTester2834343.out");
            file.delete();
            SettingsManager.instance().setSaveDirectory(directory);
        } catch (IOException e) {
            System.err.println("Couldn't create temp file.");
            System.exit(1);
        }
        dm=new DownloadManager();
        RouterService rs=new RouterService(null, null, null, null);
        dm.initialize(new ActivityCallbackStub(), new MessageRouterStub(), 
                      null, new FileManagerStub(), rs);
     
        testSimpleDownload();
        cleanup();
        testSimpleSwarm();
        cleanup();
        testUnbalancedSwarm();
        cleanup();
        testSwarmWithInterrupt();
        cleanup();
        testAddDownload();
        cleanup();
    }


    ////////////////////////// Test Cases //////////////////////////

    private static void testSimpleDownload() {
        System.out.print("-Testing non-swarmed download...");
        RemoteFileDesc rfd=newRFD(6346, 100);
        RemoteFileDesc[] rfds = {rfd};
        testGeneric(rfds);
    }

    private static void testSimpleSwarm() {
        System.out.print("-Testing swarming from two sources...");
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=10;
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
        final int RATE=10;
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
        final int RATE=10;
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
        check(u1<TestFile.length()-STOP_AFTER+FUDGE_FACTOR, "u1 did all the work");
        check(u2==STOP_AFTER, "u2 did all the work");
    }


    private static void testAddDownload() {
        System.out.print("-Testing addDownload (increases swarming)...");
        final int RATE=10;
        final int FUDGE_FACTOR=15000;  
        uploader1.setRate(RATE);
        uploader2.setRate(RATE);
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);

        Downloader download=null;
        try {
            //Start one location, wait a bit, then add another.
            download=dm.getFiles(new RemoteFileDesc[] {rfd1}, false);
            try { Thread.sleep(10); } catch (InterruptedException e) { }
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

        //Make sure there weren't too many overlapping regions. Each upload should
        //do roughly half the work.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        System.out.println("\tu1: "+u1);
        System.out.println("\tu2: "+u2);
        System.out.println("\tTotal: "+(u1+u2));

        check(u1<TestFile.length()/2+FUDGE_FACTOR, "u1 did all the work");
        check(u2<TestFile.length()/2+FUDGE_FACTOR, "u2 did all the work");
    }



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

    /** Returns true if the complete file exists and is complete */
    private static boolean isComplete() {
        if (file.length()!=TestFile.length())
            return false;
        try {
            FileInputStream stream = new FileInputStream(file);
            for (int i=0 ; ; i++) {
                int c=stream.read();
                if (c==-1)//eof
                    break;
                if ((byte)c!=TestFile.getByte(i))
                    return false;
            }
        } catch (IOException ioe) {
            return false;
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
        file.delete();
        uploader1.reset();
        uploader2.reset();
        uploader3.reset();
    }
}
