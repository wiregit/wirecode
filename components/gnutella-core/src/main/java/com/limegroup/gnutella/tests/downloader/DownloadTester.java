package com.limegroup.gnutella.tests.downloader;

import java.io.*;
import java.util.*;
import java.net.URL;
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
    static TestUploader uploader4=new TestUploader("6349", 6349);
    static final DownloadManager dm = new DownloadManager();
    static final ActivityCallbackStub callback = new ActivityCallbackStub();

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
        dm.initialize(callback, new MessageRouterStub(), 
                      null, new FileManagerStub());
        dm.postGuiInit(rs);

        SimpleTimer timer = new SimpleTimer(true);
        Runnable click = new Runnable() {
                public void run() {
                    dm.measureBandwidth();
                }
            };
        timer.schedule(click,0,SupernodeAssigner.TIMER_DELAY);

//          testOverlapCheckSpeed(5);
//          cleanup();
//          testOverlapCheckSpeed(25);
//          cleanup();
//          testOverlapCheckSpeed(125);
//          cleanup();
        if(args.length == 0 || args[0].equals("0")) {
            testSimpleDownload();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("1")) {
            testSimpleSwarm();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("2")) {
            testUnbalancedSwarm();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("3")) {
            testSwarmWithInterrupt();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("4")) {
            testStealerInterrupted();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("5")) {
            testAddDownload();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("6")) {
            testStallingUploaderReplaced();
            cleanup();
        }
        //test corruption and ignore it
        //Note: callback.delCorrupt = false by default.
        if(args.length == 0 || args[0].equals("7")) {
            testOverlapCheckGrey(false);//wait for complete
            cleanup();
        }
        if(args.length == 0 || args[0].equals("8")) {
            testOverlapCheckWhite(false);//wait for complete
            cleanup();
        }
        //now test corruption without ignoring it
        callback.delCorrupt = true;
        if(args.length == 0 || args[0].equals("9")) {
            testOverlapCheckGrey(true);//wait for corrupt
            cleanup();
        }
        if(args.length == 0 || args[0].equals("10")) {
            testOverlapCheckWhite(true);//wait for corrupt
            cleanup();
        }
        if(args.length == 0 || args[0].equals("11")) {
            testSimpleAlternateLocations();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("12")) {
            testTwoAlternateLocations();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("13")) {
            testUploaderAlternateLocations();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("14")) {
            testWeirdAlternateLocations();
            cleanup();
        }
        if(args.length == 0 || args[0].equals("15")) {
    		testStealerInterruptedWithAlternate();
            cleanup();
        }
    }


    ////////////////////////// Test Cases //////////////////////////

    private static void testOverlapCheckSpeed(int rate) {
        RemoteFileDesc rfd=newRFD(6346, 100);
        System.out.println("-Measuring time for download at rate "+rate+"...");
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
            Assert.that(false, "Unexpected exception: "+e);
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
            AlternateLocationCollection dcol=new AlternateLocationCollection();
            HTTPDownloader downloader=new HTTPDownloader(rfd, file,dcol);
            VerifyingFile vf = new VerifyingFile(false);
            vf.open(file,null);
            downloader.connectTCP(0);
            downloader.connectHTTP(0, TestFile.length());
            downloader.doDownload(vf);
        } catch (IOException e) {
            Assert.that(false, "Unexpected exception: "+e);
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

    private static void testOverlapCheckGrey(boolean deleteCorrupt) {
        System.out.print("-Testing overlap checking from Grey area..." +
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
        System.out.println("passed");//got here? Test passed
        //TODO: check IncompleteFileManager, disk
    }


    private static void testOverlapCheckWhite(boolean deleteCorrupt) {
        System.out.print("-Testing overlap checking from White area..."+
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
        System.out.println("passed");//got here? Test passed
    }

    private static void testSimpleAlternateLocations() {  
        System.out.print("-Testing AlternateLocation write...");
		com.sun.java.util.collections.Set set = 
		  new com.sun.java.util.collections.HashSet();
		try {
		    set.add(URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB"));
		} catch(Exception e) {
		    System.err.println("SHA1 not created for :"+file);
		}
        RemoteFileDesc rfd1=newRFDWithURN(6346, 100, set);
        RemoteFileDesc[] rfds = {rfd1};

        testGeneric(rfds);

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

    private static void testTwoAlternateLocations() {  
        System.out.print("-Testing Two AlternateLocations...");
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        testGeneric(rfds);

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

    private static void testUploaderAlternateLocations() {  
		// This is a modification of simple swarming based on alternate location
		// for the second swarm
        System.out.print("-Testing swarming from two sources one based on alt...");
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

    private static void testWeirdAlternateLocations() {  
        System.out.print("-Testing AlternateLocation write...");
        RemoteFileDesc rfd1=newRFD(6346, 100);
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

        testGeneric(rfds);

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

    private static void testStealerInterruptedWithAlternate() {
        System.out.print("-Testing swarming of rfds ignoring alt ...");
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
        RemoteFileDesc rfd1=newRFD(6346, 100);
        RemoteFileDesc rfd2=newRFD(6347, 100);
        RemoteFileDesc rfd3=newRFD(6348, 100);
        RemoteFileDesc rfd4=newRFD(6349, 100);
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

        testGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = uploader1.amountUploaded();
        int u2 = uploader2.amountUploaded();
        int u3 = uploader3.amountUploaded();
        int u4 = uploader4.amountUploaded();
        System.out.println("\tu1: "+u1);
        System.out.println("\tu2: "+u2);
        System.out.println("\tu3: "+u3);
        System.out.println("\tu4: "+u4);
        System.out.println("\tTotal: "+(u1+u2+u3));

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        check(u1==STOP_AFTER, "u1 did all the work");
        check(u2>0, "u2 did no work");
        check(u3>0, "u3 did no work");
        check(u4==0, "u4 was used");
        SettingsManager.instance().setConnectionSpeed(capacity);
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
		com.sun.java.util.collections.Set set) {
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
        uploader4.reset();
    }
}
