package com.limegroup.gnutella;

import junit.framework.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.security.DummyAuthenticator;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.http.*;
import java.io.*;
import java.net.*;

public class UploaderTest extends TestCase {

    ActivityCallback ac;
    MessageRouter mr;
    FileManager fm;
    Authenticator a;
    RouterService rs;
    RemoteFileDesc rfd1;
    RemoteFileDesc rfd2;
    RemoteFileDesc rfd3;
    RemoteFileDesc rfd4;
    RemoteFileDesc rfd5;


    public void setUp() {
        ac = new ActivityCallbackStub();
        mr = new MessageRouterStub();
        fm = new FileManagerStub();
        a= new DummyAuthenticator();
        rs = new RouterService(ac,mr,fm,a);
        FileDesc fd = fm.get(0);
        rfd1 = new RemoteFileDesc("1.1.1.1",0,0,"abc.txt",1000000,
                                             new byte[16], 56, false, 3,
                                             false, null, null);
        rfd2 = new RemoteFileDesc("1.1.1.2",0,0,"abc.txt",1000000,
                                             new byte[16], 56, false, 3,
                                             false, null, null);
        rfd3 = new RemoteFileDesc("1.1.1.3",0,0,"abc.txt",1000000,
                                             new byte[16], 56, false, 3,
                                             false, null, null);
        rfd4 = new RemoteFileDesc("1.1.1.4",0,0,"abc.txt",1000000,
                                             new byte[16], 56, false, 3,
                                             false, null, null);
        rfd5 = new RemoteFileDesc("1.1.1.5",0,0,"abc.txt",1000000,
                                             new byte[16], 56, false, 3,
                                             false, null, null);
    }

    public UploaderTest(String name) {
        super(name);
    }

    public void tearDown() {
        ac = null;
        mr = null;
        fm = null;
        a = null;
        rs = null;
        rfd1 = null;
        rfd2 = null;
        rfd3 = null;
        rfd4 = null;
        rfd5 = null;
    }

    public static Test suite() {
        return new TestSuite(UploaderTest.class);
    }
    /**
     * Tests that:
     * - uploads upto maxUploads get slots
     * - uploader after that but upto uploadQueueSize get queued
     * - uploads beyond that get try again later
     * - when an uploade with slot terminates first uploader gets slot
     * - when an uploader with slot terminates, everyone in queue advances.
     */
    public void testNormalQueueing() {
        UploadManager upManager = new UploadManager(ac,mr,fm);
        SettingsManager.instance().setMaxUploads(2);
        SettingsManager.instance().setSoftMaxUploads(9999);
        SettingsManager.instance().setUploadsPerPerson(99999);
        SettingsManager.instance().setUploadQueueSize(2);
        HTTPDownloader d3 = null;
        try { //first two uploads to get slots
            HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
            connectDloader(d1,true,rfd1);
            HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
            connectDloader(d2,true,rfd2);
            try { //queued at 0th position
                d3 = addUploader(upManager,rfd3,"1.1.1.3",true);
                connectDloader(d3,true,rfd3);
                fail("uploader should have been queued, but was given slot");
            } catch(QueuedException qx) {
                assertEquals(0, qx.getQueuePosition());
            } catch (Exception ioe) {
                ioe.printStackTrace();
                fail("not queued");
            }
            HTTPDownloader d4 = null;
            try { //queued at 1st position
                d4 = addUploader(upManager,rfd4,"1.1.1.4",true);
                connectDloader(d4,true,rfd4);
                fail("uploader should have been queued, but was given slot");
            } catch(QueuedException qx) {
                assertEquals(1,qx.getQueuePosition());
            } catch (Exception ioe) {
                fail("not queued");
            }
            HTTPDownloader d5 = null;
            try { //rejected
                d5 = addUploader(upManager,rfd5,"1.1.1.5",true);
                connectDloader(d5,true,rfd5);
                fail("downloader should have been rejected");
            } catch (QueuedException qx) {
                fail("queued after capacity ");
            } catch (TryAgainLaterException tax) {//correct
            } catch (IOException ioe) {//TODO1: Is this really acceptable??
                //this is OK(see TODO) for now, since the uploader
                //may close the socket
            }catch(Exception e) { //any other error is bad
                e.printStackTrace();
                fail("unknown exception");
            }
            //free up a slot
            kill(d1);//now the queue should free up, but we need to request
            
            //wait till min Poll time + 2 seconds to be safe, we have been
            //burned by this before - if d4 responds too fast it gets
            //disconnected
            Thread.sleep((UploadManager.MIN_POLL_TIME+
                          UploadManager.MAX_POLL_TIME)/2);
            //test that uploaders cannot jump the line
            try { //still queued - cannot jump line.
                connectDloader(d4,false, rfd4);
                fail("uploader allowed to jump the line");
            } catch (QueuedException talx){//correct behaviour
            } catch (Exception e) {//any other is bad
                e.printStackTrace();
                fail("wrong exception thrown");
            }
            //test that first uploader in queue is given the slot
            try { //should get the slot
                connectDloader(d3,false,rfd3);
            } catch (QueuedException e) {
                e.printStackTrace();
                fail("downloader should have got slot "+e.getQueuePosition());
            } catch (TryAgainLaterException tlx) {
                fail("exception thrown in connectHTTP");
            } catch (Exception ign) {
                ign.printStackTrace();
            }
            //Test that uploads in queue advance. d4 should have 0th position
            Thread.sleep((UploadManager.MIN_POLL_TIME+
                          UploadManager.MAX_POLL_TIME)/2);
            try {
                connectDloader(d4,false,rfd4);
            } catch(QueuedException qx) {
                assertEquals(0,qx.getQueuePosition());
            } catch (Exception other) {
                other.printStackTrace();
                fail("other exception thrown");
            }
            System.out.println("Passed");
        } catch(Exception anyother) {
            System.out.println("FAILED");
            anyother.printStackTrace();
        }
    }
    
    /**
     * Makes sure that if downloaders reconnect too soon they are dropped
     * also if uploaders respond too late they should be dropped. Downloader
     * MUST respond bewteen MIN_POLL_TIME and MAX_POLL_TIME
     */
    public void testQueueTiming() {
        UploadManager upManager = new UploadManager(ac,mr,fm);
        SettingsManager.instance().setMaxUploads(2);
        SettingsManager.instance().setSoftMaxUploads(9999);
        SettingsManager.instance().setUploadsPerPerson(99999);
        SettingsManager.instance().setUploadQueueSize(2);
        HTTPDownloader d3 = null;
        try { //first two uploads to get slots
            HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
            connectDloader(d1,true,rfd1);
            HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
            connectDloader(d2,true,rfd2);
            try { //queued at 0th position
                d3 = addUploader(upManager,rfd3,"1.1.1.3",true);
                connectDloader(d3,true,rfd3);
                fail("uploader should have been queued, but was given slot");
            } catch(QueuedException qx) {
                assertEquals(0, qx.getQueuePosition());
            } catch (Exception ioe) {
                ioe.printStackTrace();
                fail("not queued");
            }
            HTTPDownloader d4 = null;
            try { //queued at 1st position
                d4 = addUploader(upManager,rfd4,"1.1.1.4",true);
                connectDloader(d4,true,rfd4);
                fail("uploader should have been queued, but was given slot");
            } catch(QueuedException qx) {
                assertEquals(1,qx.getQueuePosition());
            } catch (Exception ioe) {
                fail("not queued");
            }
            //OK. we have two uploaders queued. one is going to respond
            //too soon and the other too late - both will be dropped.
            try { //too soon.
                connectDloader(d3,false,rfd3);
                fail("downloader should have been dropped");
            } catch (QueuedException qx) {
                fail("Should have been dropped, not queued");
            } catch (IOException ioe) {//correct behavoiour
            } catch (Exception e) {
                e.printStackTrace();
                fail("other exception thrown");
            }
            //TODO3: The test below does not work because we are not using
            // real sockets, so the setting the soTimeout does not work.
            //reading after soTimeout does not cause an exception to be thrown
            // for now, we will have to test this manually
//              Thread.sleep(UploadManager.MAX_POLL_TIME+10);//Now we are too late
//              try { 
//                  connectDloader(d4,false,rfd4);
//                  fail("downloader should have been dropped");
//              } catch(QueuedException qx) {
//                  fail("downloader should have been dropped, its queued");
//              } catch(TryAgainLaterException talx) {
//                  fail("downloader should have been dropped talx thrown");
//              } catch (IOException iox) { //correct behaviour
//              } catch (Exception other) {
//                  other.printStackTrace();
//                  fail("unknown exception thrown");
//              }
            System.out.println("Passed");
        } catch(Exception anyother) {
            System.out.println("Failed");
            anyother.printStackTrace();
        }
    }

    /** 
     * Adds a downloader to upman, returning the downloader.  The downloader
     * is connected but doesn't actually read the contents.
     *
     * @param upman the UploadManager responsible for accepting or denying
     *  the upload
     * @param rfd the file requested by the downloader
     * @param ip the address reported by the downloader.  This need
     *  not be a connectable address, though it must be resolvable
     * @param block if true, force the uploader to block after writing
     *  hundredth byte
     * @exception TryAgainLaterException the downloader was denied 
     *  because upman was busy
     * @exception IOException some other exception
     */     
    private static HTTPDownloader addUploader(final UploadManager upman, 
                                              RemoteFileDesc rfd,
                                              String ip,
                                              boolean block) throws IOException{
        //Allow some fudging to prevent race conditons.
        try { Thread.sleep(1000); } catch (InterruptedException e) { }

        PipedSocketFactory psf=new PipedSocketFactory(
                                        "127.0.0.1", ip, block ? 1000 : -1, -1);
        //Socket A either has no limit or a limit of 1000 bytes to write.
        //Socket B has no limit
        final Socket sa=psf.getSocketA();
        Thread runner=new Thread() {
            public void run() {
                upman.acceptUpload(HTTPRequestMethod.GET,sa);
            }
        };
        runner.setDaemon(true);
        runner.start();       

        Socket sb=psf.getSocketB();
        File tmp=File.createTempFile("UploadManager_Test", "dat");
        HTTPDownloader downloader=new HTTPDownloader(sb, rfd, tmp, 
                                          new AlternateLocationCollection());
        tmp.delete();
        return downloader;
    }
    
    private static void connectDloader(HTTPDownloader dloader, boolean tcp, 
                                       RemoteFileDesc rfd) throws 
                                       TryAgainLaterException, IOException {  
        if(tcp)
            dloader.connectTCP(0); //may throw TryAgainLater, etc.
        dloader.connectHTTP(0,rfd.getSize(),true);
    }

    private static void kill(HTTPDownloader downloader) {
        downloader.stop();
        try { Thread.sleep(400); } catch (InterruptedException ignored) { }
    }
    
 
}
