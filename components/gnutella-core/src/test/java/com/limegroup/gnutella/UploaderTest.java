package com.limegroup.gnutella;

import junit.framework.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.security.Authenticator;
import com.limegroup.gnutella.security.DummyAuthenticator;
import com.limegroup.gnutella.downloader.*;
import com.limegroup.gnutella.uploader.StalledUploadWatchdog;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.settings.*;
import java.io.*;
import java.net.*;

public class UploaderTest extends com.limegroup.gnutella.util.BaseTestCase {

    private ActivityCallback ac;
    private FileManager fm;
    private RouterService rs;
    private UploadManager upManager;
    private RemoteFileDesc rfd1;
    private RemoteFileDesc rfd2;
    private RemoteFileDesc rfd3;
    private RemoteFileDesc rfd4;
    private RemoteFileDesc rfd5;


    public void setUp() throws Exception {
        ac = new ActivityCallbackStub();
        fm = new FileManagerStub();
        rs = new RouterService(ac);
        upManager = new UploadManager();

        PrivilegedAccessor.setValue(rs,"fileManager",fm);
        PrivilegedAccessor.setValue(rs,"uploadManager", upManager);

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
        fm = null;
        rs = null;
        upManager = null;
        rfd1 = null;
        rfd2 = null;
        rfd3 = null;
        rfd4 = null;
        rfd5 = null;
    }

    public static Test suite() {
        return buildTestSuite(UploaderTest.class);
    }

    /**
     * - Bandwidth tracker works properly.
     */
    public void testLegacy() {
        UploadManager.tBandwidthTracker(new UploadManager());
    }
    
    /**
     * Makes sure a stalled uploader is disconnected.
     *  (We need to have a queue size of 1 because 0 isn't allowed, so
     *   the test is slightly more complicated than it needs to be.)
     */
    public void testStalledUploader() throws Exception {
        SettingsManager.instance().setMaxUploads(2);
        SettingsManager.instance().setSoftMaxUploads(9999);
        SettingsManager.instance().setUploadsPerPerson(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(1);
       
        HTTPDownloader d1, d2, d3, d4;
        d1 = addUploader(upManager, rfd1, "1.1.1.1", true);
        connectDloader(d1,true,rfd1,true);
        d2 = addUploader(upManager, rfd2, "1.1.1.2", true);
        connectDloader(d2,true,rfd2,true);
       
        // assert that we can reach the limit
        d3 = addUploader(upManager, rfd3, "1.1.1.3", true);
        try {
            connectDloader(d3,true,rfd3,true);
            fail("expected queued later.");
        } catch(QueuedException e) {
            assertEquals(1, e.getQueuePosition());
        }
        
        // okay, we know its full, now drop the guy from the queue.
        try {
            connectDloader(d3, false, rfd3, true);
            fail("should have thrown ioexception");
        } catch(IOException e) {
            // expected behaviour.
        }
        
        //sleep a little more than needed for the stall to die.
        Thread.sleep(StalledUploadWatchdog.DELAY_TIME+5);
        // should be able to connect now.
        d3 = addUploader(upManager, rfd3, "1.1.1.3", false);
        connectDloader(d3, true, rfd3, true);        
        d4 = addUploader(upManager, rfd4, "1.1.1.4", false);
        connectDloader(d4,true,rfd4,true);
        
        kill(d3);
        kill(d4);
    }       

    /**
     * Tests that:
     * - uploads upto maxUploads get slots
     * - uploader after that but upto uploadQueueSize get queued
     * - uploads beyond that get try again later
     * - when an uploade with slot terminates first uploader gets slot
     * - when an uploader with slot terminates, everyone in queue advances.
     */
    public void testNormalQueueing() throws Exception {
        SettingsManager.instance().setMaxUploads(2);
        SettingsManager.instance().setSoftMaxUploads(9999);
        SettingsManager.instance().setUploadsPerPerson(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
        HTTPDownloader d3 = null;
        //first two uploads to get slots
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
        connectDloader(d2,true,rfd2,true);
        try { //queued at 1st position
            d3 = addUploader(upManager,rfd3,"1.1.1.3",true);
            connectDloader(d3,true,rfd3,true);
            fail("uploader should have been queued, but was given slot");
        } catch(QueuedException qx) {
            assertEquals(1, qx.getQueuePosition());
        } catch (Exception ioe) {
            fail("not queued", ioe);
        }
        HTTPDownloader d4 = null;
        try { //queued at 2nd position
            d4 = addUploader(upManager,rfd4,"1.1.1.4",true);
            connectDloader(d4,true,rfd4,true);
            fail("uploader should have been queued, but was given slot");
        } catch(QueuedException qx) {
            assertEquals(2,qx.getQueuePosition());
        } catch (Exception ioe) {
            fail("not queued", ioe);
        }
        HTTPDownloader d5 = null;
        try { //rejected
            d5 = addUploader(upManager,rfd5,"1.1.1.5",true);
            connectDloader(d5,true,rfd5,true);
            fail("downloader should have been rejected");
        } catch (QueuedException qx) {
            fail("queued after capacity ", qx);
        } catch (TryAgainLaterException tax) {//correct
        } catch (IOException ioe) {//TODO1: Is this really acceptable??
            //this is OK(see TODO) for now, since the uploader
            //may close the socket
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
            connectDloader(d4,false, rfd4,true);
            fail("uploader allowed to jump the line");
        } catch (QueuedException talx){//correct behaviour
        } catch (Exception e) {//any other is bad
            fail("wrong exception thrown", e);
        }
        //test that first uploader in queue is given the slot
        try { //should get the slot
            connectDloader(d3,false,rfd3,true);
        } catch (QueuedException e) {
            fail("downloader should have got slot "+e.getQueuePosition(), e);
        }
        //Test that uploads in queue advance. d4 should have 0th position
        Thread.sleep((UploadManager.MIN_POLL_TIME+
                      UploadManager.MAX_POLL_TIME)/2);
        try {
            connectDloader(d4,false,rfd4,true);
        } catch(QueuedException qx) {
            assertEquals(1,qx.getQueuePosition());
        }
        //System.out.println("Passed");
    }
        
        
    
    /**
     * Makes sure that if downloaders reconnect too soon they are dropped
     * also if uploaders respond too late they should be dropped. Downloader
     * MUST respond bewteen MIN_POLL_TIME and MAX_POLL_TIME
     */
    public void testQueueTiming() throws Exception {
        SettingsManager.instance().setMaxUploads(2);
        SettingsManager.instance().setSoftMaxUploads(9999);
        SettingsManager.instance().setUploadsPerPerson(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
        HTTPDownloader d3 = null;
        //first two uploads to get slots
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
        connectDloader(d2,true,rfd2,true);
        try { //queued at 0th position
            d3 = addUploader(upManager,rfd3,"1.1.1.3",true);
            connectDloader(d3,true,rfd3,true);
            fail("uploader should have been queued, but was given slot");
        } catch(QueuedException qx) {
            assertEquals(1, qx.getQueuePosition());
        } catch (Exception ioe) {
            fail("not queued", ioe);
        }
        HTTPDownloader d4 = null;
        try { //queued at 1st position
            d4 = addUploader(upManager,rfd4,"1.1.1.4",true);
            connectDloader(d4,true,rfd4,true);
            fail("uploader should have been queued, but was given slot");
        } catch(QueuedException qx) {
            assertEquals(2,qx.getQueuePosition());
        } catch (Exception ioe) {
            fail("not queued", ioe);
        }
        //OK. we have two uploaders queued. one is going to respond
        //too soon and the other too late - both will be dropped.
        try { //too soon.
            connectDloader(d3,false,rfd3,true);
            fail("downloader should have been dropped");
        } catch (QueuedException qx) {
            fail("Should have been dropped, not queued", qx);
        } catch (IOException ioe) {//correct behavoiour
        }
        //TODO3: The test below does not work because we are not using
        // real sockets, so the setting the soTimeout does not work.
        //reading after soTimeout does not cause an exception to be thrown
        // for now, we will have to test this manually
//              Thread.sleep(UploadManager.MAX_POLL_TIME+10);//Now we are too late
//              try { 
//                  connectDloader(d4,false,rfd4,true);
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
        //System.out.println("Passed");
    }

    /**
     * Tests that Uploader only queues downloaders that specify that they 
     * support queueing
     */
    public void testNotQueuedUnlessHeaderSent() throws Exception {
        SettingsManager.instance().setMaxUploads(1);
        SettingsManager.instance().setSoftMaxUploads(9999);
        SettingsManager.instance().setUploadsPerPerson(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(1);
        //take the only available slto
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        //now there are no slots
        HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
        try {
            connectDloader(d2,true,rfd2,false);//dont queue
            fail("d2 was accepted after slot limit reached");
        } catch(QueuedException qe) {
            fail("Downloader d2 not send x-queue, but was queued", qe);
        } catch (TryAgainLaterException expectedException) { 
        } catch (IOException ioe) {//This is similar to d5 being rejected
            //in testNormalQueueing, IOE may be thrown if uploader
            //closes the socket. 
        }
        HTTPDownloader d3 = addUploader(upManager,rfd3,"1.1.1.3",true);
        try {
            connectDloader(d3,true,rfd3,true);
            fail("d3 was accepted instead of being queueud");
        } catch(TryAgainLaterException tx) {
            fail("d3 should have been queued TALX thrown", tx);
        } catch (QueuedException expectedException) {
            assertEquals("should be first in queue", 1,expectedException.getQueuePosition());
        }
        //System.out.println("Passed");
    }

    public void testPerHostLimitedNotQueued() throws Exception {
        SettingsManager.instance().setMaxUploads(2);
        SettingsManager.instance().setSoftMaxUploads(9999);
        SettingsManager.instance().setUploadsPerPerson(2);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
   
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.1",true);
        connectDloader(d2,true,rfd1,true);
        HTTPDownloader d3 = addUploader(upManager,rfd3,"1.1.1.1",true);
        try {
            connectDloader(d3,true,rfd1,true);
            fail("Host limit reached, should not have accepted d3");
        } catch (QueuedException qx) {
            fail("host limit reached should not queue", qx);
        } catch (TryAgainLaterException expectedException) {
        } catch (IOException ioe) {//This is similar to d5 being rejected
            //in testNormalQueueing, IOE may be thrown if uploader
            //closes the socket. 
        }            
        HTTPDownloader d4 = addUploader(upManager,rfd4,"1.1.1.2",true);
        try {
            connectDloader(d4,true,rfd1,true);
            fail("Host limit reached, should not have accepted d4");
        } catch (TryAgainLaterException tx) {
            fail("d4 should have been queued", tx);
        } catch (QueuedException expectedException) {
        } catch (IOException ioe) {
            fail("d4 should have been queued", ioe);
        }            
        //System.out.println("passed");
    }
 
    public void testSoftMax() throws Exception {
        SettingsManager.instance().setMaxUploads(9999);
        SettingsManager.instance().setSoftMaxUploads(2);
        SettingsManager.instance().setUploadsPerPerson(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
        HTTPDownloader d3 = null;
        //first two uploads to get slots
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
        connectDloader(d2,true,rfd2,true);
        try { //queued at 1st position
            d3 = addUploader(upManager,rfd3,"1.1.1.3",true);
            connectDloader(d3,true,rfd3,true);
            fail("uploader should have been queued, but was given slot");
        } catch(QueuedException qx) {
            assertEquals("should be first in queue", 1, qx.getQueuePosition());
        } catch (Exception ioe) {
            fail("not queued", ioe);
        }
        kill(d1);
        Thread.sleep((UploadManager.MIN_POLL_TIME+
                      UploadManager.MAX_POLL_TIME)/2);            
        try { //should get the slot
            connectDloader(d3,false,rfd3,true);
        } catch (QueuedException e) {
            fail("downloader should have got slot "+e.getQueuePosition(), e);
        } catch (TryAgainLaterException tlx) {
            fail("exception thrown in connectHTTP", tlx);
        }
        //System.out.println("passed");
    }
    
    /**
     * We should count the number of uploads in progress AND the number of
     * uploads the upload queue before deciding the upload per host limit.
     * Tests this. 
     */
    public void testUploadLimtIncludesQueue() throws Exception {
        SettingsManager.instance().setMaxUploads(1);
        SettingsManager.instance().setSoftMaxUploads(1);
        SettingsManager.instance().setUploadsPerPerson(1);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(10);
        //first two uploads to get slots
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        try { //queued at 1st position
            HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
            connectDloader(d2,true,rfd2,true);
            fail("uploader should have been queued");
        } catch (QueuedException qx) {
            assertEquals("should be first in queue", 1,qx.getQueuePosition());
        }
        try {
            HTTPDownloader d3 = addUploader(upManager,rfd2,"1.1.1.2",true);
            connectDloader(d3,true,rfd2,true);
            fail("uploader should have been rejected ");
        } catch (QueuedException qx) {
            fail("uploader should have been rejected not queued ", qx);
        } catch (TryAgainLaterException talx) {
            //expected behaviour
        } catch (IOException e) {  
            //IOException is OK because we are using pipedSocketFactory
            //which does not allow the downloader to read the bytes in
            //the buffer if the uploader closes the socket first, rather
            //it throws an IOException.
        }
        //System.out.println("passed");
    }

    /**
     * Tests that two requests for the same file on the same connection, does
     * not cause the second request to be queued.
     */
    public void testSameFileSameHostGivenSlot() throws Exception { 
        SettingsManager.instance().setMaxUploads(1);
        SettingsManager.instance().setSoftMaxUploads(1);
        SettingsManager.instance().setUploadsPerPerson(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
        //connect the first downloader.
        PipedSocketFactory psf = null;
        try {
            psf = new PipedSocketFactory("127.0.0.1", "1.1.1.1",-1,-1);
        } catch (Exception e) {
            fail("unable to create piped socket factory", e);
        }
        final Socket sa = psf.getSocketA();
        final UploadManager upman = upManager;
        Thread t = new Thread() {
            public void run() {
                upman.acceptUpload(HTTPRequestMethod.GET, sa, false);
            }
        };
        t.setDaemon(true);
        t.start();
        BufferedWriter out = null;
        ByteReader byteReader = null;
        String s = null;
        try {
            Socket sb = psf.getSocketB();
            OutputStream os = sb.getOutputStream();
            out = new BufferedWriter(new OutputStreamWriter(os));
            out.write("GET /get/0/abc.txt HTTP/1.1\r\n");
            out.write("User-Agent: "+CommonUtils.getHttpServer()+"\r\n");
            out.write("X-Queue: 0.1\r\n");//we support remote queueing
            out.write("Range: bytes=0-12000 \r\n");
            out.write("\r\n");
            out.flush();
            byteReader = new ByteReader(sb.getInputStream());
            s = byteReader.readLine();
            assertEquals("HTTP/1.1 206 Partial Content",s);
        } catch (Exception e) {
            fail("problem with first downloader", e);
        }
        //second request. This one will be queued at position 1        
        try {
            HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
            connectDloader(d2,true,rfd2,true);
            fail("d2 should not have been given slot");
        } catch (QueuedException qEx) {
            assertEquals("should be first in queue", 1,qEx.getQueuePosition());
        } catch (Exception other) {
            fail("download should have been queued", other);
        }
        //second request from the uploader which already has the slot
        try {
            while(!s.equals(""))
                s = byteReader.readLine();
            byte[] buf = new byte[1024];
            int d=0;
            while(true) {
                int u = byteReader.read(buf,0,1024);
                d+=u;
                if(d>11999)
                    break;
            }
            out.write("GET /get/0/abc.txt HTTP/1.1\r\n");
            out.write("User-Agent: "+CommonUtils.getHttpServer()+"\r\n");
            out.write("X-Queue: 0.1\r\n");//we support remote queueing
            out.write("Range: bytes=1200-2400 \r\n");
            out.write("\r\n");
            out.flush();
            s = byteReader.readLine();
            assertEquals("HTTP/1.1 206 Partial Content",s);
        } catch(Exception e) {
            fail("exception thrown while trying HTTP 1.1 pipling", e);
        }
        //third uploader must be queued at position 2.
        try {
            HTTPDownloader d3 = addUploader(upManager,rfd3,"1.1.1.3",true);
            connectDloader(d3,true,rfd2,true);
        } catch (QueuedException q) {
            assertEquals("should be second in queue", 2,q.getQueuePosition());
        } catch (Exception other) {
            fail("download should have been queued", other);
        }
        //System.out.println("Passed");
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
                upman.acceptUpload(HTTPRequestMethod.GET,sa, false);
            }
        };
        runner.setDaemon(true);
        runner.start();       

        Socket sb=psf.getSocketB();
        File tmp=File.createTempFile("UploadManager_Test", "dat");
        HTTPDownloader downloader = 
			new HTTPDownloader(sb, rfd, tmp, null);
        tmp.delete();
        return downloader;
    }
    
    private static void connectDloader(HTTPDownloader dloader, boolean tcp, 
                                       RemoteFileDesc rfd,boolean queue) throws 
                                       TryAgainLaterException, IOException {  
        if(tcp)
            dloader.connectTCP(0); //may throw TryAgainLater, etc.
        dloader.connectHTTP(0,rfd.getSize(),queue);
    }

    private static void kill(HTTPDownloader downloader) {
        downloader.stop();
        try { Thread.sleep(400); } catch (InterruptedException ignored) { }
    }
 
}
