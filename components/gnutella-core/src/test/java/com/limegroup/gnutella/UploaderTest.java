package com.limegroup.gnutella;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.limewire.service.ErrorService;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.auth.StubContentAuthority;
import com.limegroup.gnutella.downloader.ConnectionStatus;
import com.limegroup.gnutella.downloader.HTTPDownloader;
import com.limegroup.gnutella.downloader.QueuedException;
import com.limegroup.gnutella.downloader.TryAgainLaterException;
import com.limegroup.gnutella.downloader.UnknownCodeException;
import com.limegroup.gnutella.downloader.VerifyingFile;
import com.limegroup.gnutella.http.HTTPRequestMethod;
import com.limegroup.gnutella.messages.vendor.ContentRequest;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.settings.UploadSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.StubIOStateObserver;
import com.limegroup.gnutella.tigertree.HashTree;
import com.limegroup.gnutella.uploader.HTTPSession;
import com.limegroup.gnutella.uploader.StalledUploadWatchdog;
import com.limegroup.gnutella.uploader.UploadSlotManager;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.util.PipedSocketFactory;

@SuppressWarnings("unchecked")
public class UploaderTest extends com.limegroup.gnutella.util.LimeTestCase {

    private ActivityCallback ac;
    private static FileManager fm;
    private RouterService rs;
    private UploadManager upManager;
    private RemoteFileDesc rfd1;
    private RemoteFileDesc rfd2;
    private RemoteFileDesc rfd3;
    private RemoteFileDesc rfd4;
    private RemoteFileDesc rfd5;
    private URN urn1,urn2,urn3,urn4,urn5;


    public void setUp() throws Exception {
        ac = new ActivityCallbackStub();
        
        Map urns = new HashMap();
        Vector descs = new Vector();
        urn1 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFG");
        urn2 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFF");
        urn3 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE");
        urn4 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFD");
        urn5 = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFC");        
        
        FileDescStub descStub = new FileDescStub("abc1.txt",urn1,0);
        urns.put(urn1,descStub);
        descs.add(descStub);
        rfd1 = new RemoteFileDesc("1.1.1.1",1,0,"abc1.txt",FileDescStub.DEFAULT_SIZE,
                                  new byte[16], 56, false, 3,
                                  false, null, descStub.getUrns(), false, false,"", null, -1);
        
        descStub = new FileDescStub("abc2.txt",urn2,1);
        urns.put(urn2,descStub);
        descs.add(descStub);
        rfd2 = new RemoteFileDesc("1.1.1.2",1,1,"abc2.txt",FileDescStub.DEFAULT_SIZE,
                                  new byte[16], 56, false, 3,
                                  false, null, descStub.getUrns(), false, false,"",null, -1);
        
        descStub = new FileDescStub("abc3.txt",urn3,2);
        urns.put(urn3,descStub);
        descs.add(descStub);
        rfd3 = new RemoteFileDesc("1.1.1.3",1,2,"abc3.txt",FileDescStub.DEFAULT_SIZE,
                                  new byte[16], 56, false, 3,
                                  false, null, descStub.getUrns(), false, false,"",null, -1);
        
        descStub = new FileDescStub("abc4.txt",urn4,3);
        urns.put(urn4,descStub);
        descs.add(descStub);
        rfd4 = new RemoteFileDesc("1.1.1.4",1,3,"abc4.txt",FileDescStub.DEFAULT_SIZE,
                                  new byte[16], 56, false, 3,
                                  false, null, descStub.getUrns(), false, false,"",null, -1);
        
        descStub = new FileDescStub("abc5.txt",urn5,4);
        urns.put(urn5,descStub);
        descs.add(descStub);
        rfd5 = new RemoteFileDesc("1.1.1.5",1,4,"abc5.txt",FileDescStub.DEFAULT_SIZE,
                                  new byte[16], 56, false, 3,
                                  false, null, descStub.getUrns(), false, false,"",null, -1);
        
        // we don't want the tests confused by the stalled
        // watchdog killing stuff.
        // note that the testStalledUploads sets this to the
        // correct time.
        StalledUploadWatchdog.DELAY_TIME = Integer.MAX_VALUE;
        
        fm = new FileManagerStub(urns,descs);
        rs = new RouterService(ac);
        RouterService.getContentManager().initialize();
        upManager = new UploadManager(new UploadSlotManager());

        PrivilegedAccessor.setValue(rs,"fileManager",fm);
        PrivilegedAccessor.setValue(rs,"uploadManager", upManager);

        fm.get(0);
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
        UploadManager.tBandwidthTracker(new UploadManager(null));
    }
    
    /** 
     * Tests that an upload triggers a validation.
     */
    public void testContentVerified() throws Exception {
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);        
        UploadSettings.HARD_MAX_UPLOADS.setValue(2);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(9999);
        UploadSettings.UPLOADS_PER_PERSON.setValue(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
        
        StubContentAuthority auth = new StubContentAuthority();
        RouterService.getContentManager().setContentAuthority(auth);
        assertEquals(0, auth.getSent().size());
        
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        assertEquals(1, auth.getSent().size());
        assertEquals(urn1, ((ContentRequest)auth.getSent().get(0)).getURN());

        kill(d1);
        
        d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1, true, rfd1, true);
        assertEquals(1, auth.getSent().size()); // didn't send again.
    }
    
    /**
     * Tests that:
     * - uploads upto maxUploads get slots
     * - uploader after that but upto uploadQueueSize get queued
     * - uploads beyond that get try again later
     * - when an uploade with slot terminates first uploader gets slot
     * - when an uploader with slot terminates, everyone in queue advances.
     * - uploads not in slot one, but uploader has available for all get slot
     */
    public void testNormalQueueing() throws Exception {
    	
    	
        UploadSettings.HARD_MAX_UPLOADS.setValue(2);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(9999);
        UploadSettings.UPLOADS_PER_PERSON.setValue(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
        HTTPDownloader d3 = null;
        //first two uploads to get slots
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
        connectDloader(d2,true,rfd2,true);
        
        assertEquals("should have two active uploaders",
            2, upManager.uploadsInProgress());
        
        try { //queued at 1st position
            d3 = addUploader(upManager,rfd3,"1.1.1.3",true);
            connectDloader(d3,true,rfd3,true);
            fail("uploader should have been queued, but was given slot");
        } catch(QueuedException qx) {
            assertEquals(1, qx.getQueuePosition());
        } catch (Exception ioe) {
            fail("not queued", ioe);
        }
        
        assertEquals("should have 1 queued uploader",
            1, upManager.getNumQueuedUploads());
        
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
        
        
        assertEquals("should have 2 queued uploaders",
            2, upManager.getNumQueuedUploads());
        
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
        
        assertEquals("should have 1 active uploader",
            1, upManager.uploadsInProgress());
        
        //wait till min Poll time + 2 seconds to be safe, we have been
        //burned by this before - if d4 responds too fast it gets
        //disconnected
        Thread.sleep((HTTPSession.MIN_POLL_TIME+
                      HTTPSession.MAX_POLL_TIME)/2);
        //test that uploaders cannot jump the line
        try { //still queued - cannot jump line.
            connectDloader(d4,false, rfd4,true);
            fail("uploader allowed to jump the line");
        } catch (QueuedException qx) {
            assertEquals("should be queued", 2, qx.getQueuePosition());
        } catch (Exception e) {//any other is bad
            fail("wrong exception thrown", e);
        }
        

        assertEquals("should have 2 queued uploaders",
            2, upManager.getNumQueuedUploads());
        
        //test that first uploader in queue is given the slot
        try { //should get the slot
            connectDloader(d3,false,rfd3,true);
        } catch (QueuedException e) {
            fail("downloader should have got slot, but was queued at: "
                + e.getQueuePosition(), e);
        }
        
        assertEquals("should have 1 queued upload",
            1, upManager.getNumQueuedUploads());
        assertEquals("should have 2 active uploads",
            2, upManager.uploadsInProgress());            
        
        //Test that uploads in queue advance. d4 should have 0th position
        Thread.sleep((HTTPSession.MIN_POLL_TIME+
                      HTTPSession.MAX_POLL_TIME)/2);
        try {
            connectDloader(d4,false,rfd4,true);
        } catch(QueuedException qx) {
            assertEquals(1,qx.getQueuePosition());
        }
        
        assertEquals("should have 1 queued upload",
            1, upManager.getNumQueuedUploads());
        assertEquals("should have 2 active uploads",
            2, upManager.uploadsInProgress());
            
        //Add another queued guy
        try { //queued at 2nd position
            d5 = addUploader(upManager,rfd5,"1.1.1.5",true);
            connectDloader(d5,true,rfd5,true);
            fail("uploader should have been queued, but was given slot");
        } catch(QueuedException qx) {
            assertEquals(2,qx.getQueuePosition());
        } catch (Exception ioe) {
            fail("not queued", ioe);
        }
        
        assertEquals("should have 2 queued uploads",
            2, upManager.getNumQueuedUploads());
        assertEquals("should have 2 active uploads",
            2, upManager.uploadsInProgress());

        // Now kill both uploads in progress and see if
        // the second queued guy can get his slot before
        // the first one polls (he should).
        // D4 & D5 are queued
        // D3 & D2 are active
        kill(d3);
        kill(d2);
        
        assertEquals("should have 2 queued uploads",
            2, upManager.getNumQueuedUploads());
        assertEquals("should have 0 active uploads",
            0, upManager.uploadsInProgress());
            
        // Sleep so we don't hammer with requests.
        Thread.sleep((HTTPSession.MIN_POLL_TIME+
                      HTTPSession.MAX_POLL_TIME)/2);            

        //test that second uploader is given a slot.
        try {
            connectDloader(d5,false,rfd5,true);
        } catch (QueuedException e) {
            fail("downloader should have got slot, but was queued at: "
                + e.getQueuePosition(), e);
        }
        
        //test that the first uploader is also given a slot.        
        try {
            connectDloader(d4,false,rfd4,true);
        } catch (QueuedException e) {
            fail("downloader should have got slot, but was queued at: "
                + e.getQueuePosition(), e);
        }
        
        assertEquals("should have 0 queued uploads",
            0, upManager.getNumQueuedUploads());
        assertEquals("should have 2 active uploads",
            2, upManager.uploadsInProgress());                

    }


    

    public void testThexQueuing() throws Exception {
        UploadSettings.HARD_MAX_UPLOADS.setValue(2);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(9999);
        UploadSettings.UPLOADS_PER_PERSON.setValue(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
        
        for(int i = 0;i<5;i++)
        	fm.get(i).getHashTree();
        
        Thread.sleep(5000);
        
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
        }
        
        assertEquals("should have 1 queued uploads",
            1, upManager.getNumQueuedUploads());
        assertEquals("should have 2 active uploads",
            2, upManager.uploadsInProgress());
            
        // should queue next guy, but 'cause its thex we wont.
        HTTPDownloader d4 = addUploader(upManager,rfd4,"1.1.1.4",true);
        assertNotNull(connectThex(d4, true));
        
        
        // d5 will connect and get queued.
        HTTPDownloader d5 = addUploader(upManager, rfd2, "1.1.1.5", true);
        try {
            connectDloader(d5, true, rfd2, true);
            fail("should have been queued");
        } catch(QueuedException qx) {
            assertEquals(2, qx.getQueuePosition());
        }

        assertEquals("should have 1 queued uploads",
            2, upManager.getNumQueuedUploads());
        assertEquals("should have 2 active uploads",
            2, upManager.uploadsInProgress());
        
        //even though 5 is queued, he should be able to get the thex tree.
        assertNotNull(connectThex(d5, false));
        
        // no need to check the tree, is checked in lots of other tests.
        
        //but, when he tries to get the file again, he stays queued.
        assertEquals("should have 1 queued uploads",
            2, upManager.getNumQueuedUploads());
        assertEquals("should have 2 active uploads",
            2, upManager.uploadsInProgress());

        try {
            connectDloader(d5, false, rfd2, true);
            fail("should have been queued");
        } catch(QueuedException qx) {
            assertEquals(2, qx.getQueuePosition());
        }
    }
    
    
    /**
     * Tests that two requests for the same file on the same connection, does
     * not cause the second request to be queued.
     */
    public void testSameFileSameHostGivenSlot() throws Exception { 
        UploadSettings.HARD_MAX_UPLOADS.setValue(1);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(1);
        UploadSettings.UPLOADS_PER_PERSON.setValue(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
        //connect the first downloader.
        PipedSocketFactory psf = null;
        try {
            psf = new PipedSocketFactory("127.0.0.1", "1.1.1.1");
        } catch (Exception e) {
            fail("unable to create piped socket factory", e);
        }
        final Socket sa = psf.getSocketA();
        final UploadManager upman = upManager;
        Thread t = new Thread() {
            public void run() {
                try {
                    upman.acceptUpload(HTTPRequestMethod.GET, sa, false);
                } catch(Throwable e) {
                    ErrorService.error(e);
                }
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
            out.write("GET /get/0/abc1.txt HTTP/1.1\r\n");
            out.write("User-Agent: "+LimeWireUtils.getHttpServer()+"\r\n");
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
            out.write("GET /get/0/abc1.txt HTTP/1.1\r\n");
            out.write("User-Agent: "+LimeWireUtils.getHttpServer()+"\r\n");
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
    
    
    public void testBanning() throws Exception {
    	
        UploadSettings.HARD_MAX_UPLOADS.setValue(2);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(9999);
        UploadSettings.UPLOADS_PER_PERSON.setValue(2);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(10);
        Class cache = PrivilegedAccessor.getClass(UploadManager.class,
                                                  "RequestCache");
        PrivilegedAccessor.setValue(cache, "WAIT_TIME",
                                    new Long(20*1000));
        PrivilegedAccessor.setValue(cache, "FIRST_CHECK_TIME",
                                    new Long(10*1000));        
   
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.1",true);
        connectDloader(d2,true,rfd2,true);
        HTTPDownloader d3 = addUploader(upManager,rfd3,"1.1.1.1",true);
        try {
            connectDloader(d3,true,rfd3,true);
            fail("Host limit reached, should not have accepted d3");
        } catch (QueuedException qx) {
            fail("host limit reached should not queue", qx);
        } catch (TryAgainLaterException expectedException) {
        } catch (IOException ioe) {//This is similar to d5 being rejected
            //in testNormalQueueing, IOE may be thrown if uploader
            //closes the socket. 
        }            
        
        assertEquals("should have 2 active uploads",
            2, upManager.uploadsInProgress());

        int i = 0;
        try {
            for(; i < 20; i++) {
                Thread.sleep(1000);
                try {
                    d3 = addUploader(upManager, rfd3, "1.1.1.1", true);
                    connectDloader(d3, true, rfd3, true);
                    fail("should have thrown limit reached");
                }
                catch(TryAgainLaterException expected) {}
                catch(QueuedException notExpected) {
                	fail("d3 should not be queued", notExpected);
                }
                catch(IOException expectedToo) {
                    if(expectedToo instanceof UnknownCodeException)
                        throw expectedToo;
                }
                
            }
            fail("should have thrown exception");
        } catch(UnknownCodeException expected) {
            assertEquals("wrong code", 403, expected.getCode());
        }
        
        // make room for d3, but it still should be rejected
        kill(d1);
        kill(d2);
        assertEquals("should have 0 active uploads",
        		0, upManager.uploadsInProgress());
        try {
        	d3 = addUploader(upManager, rfd3, "1.1.1.1", true);
        	connectDloader(d3, true, rfd3, true);
        	fail("d3 should still be banned");
        } catch (TryAgainLaterException expected){}
        catch(UnknownCodeException expected) {
            assertEquals("wrong code", 403, expected.getCode());
        }
            
        // now wait awhile and we should be allowed back in.        
        Thread.sleep(60000);
        d3 = addUploader(upManager, rfd3, "1.1.1.1", true);
        connectDloader(d3, true, rfd1, true);
        assertEquals("should have 1 active uploads",
            1, upManager.uploadsInProgress());        
    }        
 
    
    public void testUniqueUploads() throws Exception {
    	UploadSettings.HARD_MAX_UPLOADS.setValue(2);
    	UploadSettings.SOFT_MAX_UPLOADS.setValue(9999);
    	UploadSettings.UPLOADS_PER_PERSON.setValue(2);
    	UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
    	
    	HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
    	connectDloader(d1,true,rfd1,true);
    	HTTPDownloader d2 = addUploader(upManager,rfd1,"1.1.1.1",true);
    	try{
    		connectDloader(d2,true,rfd2,true);
    		fail("Duplicate upload, should not have accepted d2");
    	}catch (QueuedException qx) {
    		fail("duplicate upload should not queue", qx);
    	} catch (TryAgainLaterException expectedException) {
    	}catch(IOException ioe){}
    	
    	d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
    	connectDloader(d2,true,rfd2,true);
    	
    	//upload slots are full, queue is empty
    	
    	HTTPDownloader d3 = addUploader(upManager,rfd2,"1.1.1.2",true);
    	try {
    		connectDloader(d3,true,rfd2,true);
    		fail("duplicate upload, should not have accepted d3");
    	} catch (QueuedException qx) {
    		fail("duplicate upload should not queue", qx);
    	} catch (TryAgainLaterException expectedException) {
    	} catch (IOException ioe) {//This is similar to d5 being rejected
    		//in testNormalQueueing, IOE may be thrown if uploader
    		//closes the socket. 
    	}
    	
    	//upload slots are still full, queue is still empty
    	
    	HTTPDownloader d4 = addUploader(upManager,rfd4,"1.1.1.4",true);
    	try {
    		connectDloader(d4,true,rfd4,true);
    		fail("slots are full, d4 should have been queued");
    	} catch (TryAgainLaterException tx) {
    		fail("d4 should have been queued", tx);
    	} catch (QueuedException expectedException) {
    	} catch (IOException ioe) {
    		fail("d4 should have been queued", ioe);
    	}
    	
 
    	
    
    	//System.out.println("passed");
    }
    
    /**
     * Makes sure a stalled uploader is disconnected.
     *  (We need to have a queue size of 1 because 0 isn't allowed, so
     *   the test is slightly more complicated than it needs to be.)
     */
    public void testStalledUploader() throws Exception {
        StalledUploadWatchdog.DELAY_TIME = 1000 * 30; // 30 seconds.
        UploadSettings.HARD_MAX_UPLOADS.setValue(2);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(9999);
        UploadSettings.UPLOADS_PER_PERSON.setValue(99999);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(1);
       
        HTTPDownloader d1, d2, d3, d4;
        d1 = addUploader(upManager, rfd1, "1.1.1.1", true);
        connectDloader(d1,true,rfd1,true);
        d2 = addUploader(upManager, rfd2, "1.1.1.2", true);
        connectDloader(d2,true,rfd2,true);
        
        assertEquals("should have 2 active uploaders",
            2, upManager.uploadsInProgress() );
       
        // assert that we can reach the limit
        d3 = addUploader(upManager, rfd3, "1.1.1.3", true);
        try {
            connectDloader(d3,true,rfd3,true);
            fail("expected queued later.");
        } catch(QueuedException e) {
            assertEquals(1, e.getQueuePosition());
        }
        
        assertEquals("should have 1 queued uploader",
            1, upManager.getNumQueuedUploads());
        
        // okay, we know its full, now drop the guy from the queue.
        try {
            connectDloader(d3, false, rfd3, true);
            fail("should have thrown ioexception");
        } catch(IOException e) {
            // expected behaviour.
        }
        
        assertEquals("should have no queued uploaders",
            0, upManager.getNumQueuedUploads());
        
        //sleep a little more than needed for the stall to die.
        Thread.sleep(StalledUploadWatchdog.DELAY_TIME+5*1000);
        
        assertEquals("should have no active uploaders",
            0, upManager.uploadsInProgress());
        
        // should be able to connect now.
        d3 = addUploader(upManager, rfd3, "1.1.1.3", false);
        connectDloader(d3, true, rfd3, true);        
        d4 = addUploader(upManager, rfd4, "1.1.1.4", false);
        connectDloader(d4,true,rfd4,true);
        
        kill(d3);
        kill(d4);
    }


    /**
     * Makes sure that if downloaders reconnect too soon they are dropped
     * also if uploaders respond too late they should be dropped. Downloader
     * MUST respond bewteen MIN_POLL_TIME and MAX_POLL_TIME
     */
    public void testQueueTiming() throws Exception {
        UploadSettings.HARD_MAX_UPLOADS.setValue(2);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(9999);
        UploadSettings.UPLOADS_PER_PERSON.setValue(99999);
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
        UploadSettings.HARD_MAX_UPLOADS.setValue(1);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(9999);
        UploadSettings.UPLOADS_PER_PERSON.setValue(99999);
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

    
    
    public void testPerHostLimit() throws Exception {
    	
    	/*
    	 * 1. d1 - host 1, slot 1
    	 * 2. d2 - host 1, slot 2
    	 * 3. d3 - host limit (rejection with free slot)
    	 * 4. d4 - host 2, slot 3
    	 * 5. d5 - host 2, queue 1
    	 * 6. d3 tries again, still rejected (rejection without free slot)
    	 * 7. d1 gets killed, d3 is queued.
    	 * 8. d1 tries again, rejected (rejection counts queued hosts)
    	 */
    	UploadSettings.HARD_MAX_UPLOADS.setValue(3);
    	UploadSettings.SOFT_MAX_UPLOADS.setValue(9999);
    	UploadSettings.UPLOADS_PER_PERSON.setValue(2);
    	UploadSettings.UPLOAD_QUEUE_SIZE.setValue(2);
    	
    	// 1. ============
    	HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
    	connectDloader(d1,true,rfd1,true);
    	// 2. ============
    	HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.1",true);
    	connectDloader(d2,true,rfd2,true);
    	// 3. ============
    	HTTPDownloader d3 = addUploader(upManager,rfd3,"1.1.1.1",true);
    	try {
    		connectDloader(d3,true,rfd3,true);
    		fail("Host limit reached, should not have accepted d3");
    	} catch (QueuedException qx) {
    		fail("host limit reached should not queue", qx);
    	} catch (TryAgainLaterException expectedException) {
    	} catch (IOException ioe) {//This is similar to d5 being rejected
    		//in testNormalQueueing, IOE may be thrown if uploader
    		//closes the socket. 
    	}            
    	
    	// 4. ============
    	HTTPDownloader d4 = addUploader(upManager, rfd4, "1.1.1.4", true);
    	connectDloader(d4, true, rfd4, true);
    	// 5. ============
    	HTTPDownloader d5 = addUploader(upManager,rfd5,"1.1.1.5",true);
    	try {
    		connectDloader(d5,true,rfd5,true);
    		fail("Host limit reached, should not have accepted d4");
    	} catch (TryAgainLaterException tx) {
    		fail("d4 should have been queued", tx);
    	} catch (QueuedException expectedException) {
    	} catch (IOException ioe) {
    		fail("d4 should have been queued", ioe);
    	}            
    	
    	// 6. ============
    	// try d3 again
    	d3 = addUploader(upManager,rfd3,"1.1.1.1",true);
    	try {
    		connectDloader(d3,true,rfd3,true);
    		fail("Host limit reached, should not have accepted d3");
    	} catch (QueuedException qx) {
    		fail("host limit reached should not queue", qx);
    	} catch (TryAgainLaterException expectedException) {
    	} catch (IOException ioe) {//This is similar to d5 being rejected
    		//in testNormalQueueing, IOE may be thrown if uploader
    		//closes the socket. 
    	}
    	
    	// 7. ============
    	// kill d1
    	kill(d1);
    	
    	// now d3 should be given a slot 
    	d3 = addUploader(upManager,rfd3,"1.1.1.1",true);
    	connectDloader(d3,true,rfd3,true);
    	
    	// 8. ============
    	// d1 tries again, rejected.
    	d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
    	try {
    		connectDloader(d1,true,rfd1,true);
    		fail("d1 should have been rejected");
    	} catch (TryAgainLaterException expected){}
    }
        
    public void testSoftMax() throws Exception {
        UploadSettings.HARD_MAX_UPLOADS.setValue(9999);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(2);
        UploadSettings.UPLOADS_PER_PERSON.setValue(99999);
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
        Thread.sleep((HTTPSession.MIN_POLL_TIME+
                      HTTPSession.MAX_POLL_TIME)/2);            
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
        UploadSettings.HARD_MAX_UPLOADS.setValue(1);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(1);
        UploadSettings.UPLOADS_PER_PERSON.setValue(1);
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
     * tests that if a queued uploader drops the connection, its slot is released.
     */
    public void testQueueDropping() throws Exception {
    	UploadSettings.HARD_MAX_UPLOADS.setValue(1);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(1);
        UploadSettings.UPLOADS_PER_PERSON.setValue(1);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(1);
        
        // take up the free slot
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        
        // take up the queue slot
        HTTPDownloader d2 = addUploader(upManager, rfd2, "1.1.1.2", true);
        try {
        	connectDloader(d2, true, rfd2, true);
        	fail("d2 should have been queued");
        } catch (QueuedException expected){}
        
        // try to queue up someone else
        HTTPDownloader d3 = addUploader(upManager, rfd3, "1.1.1.3", true);
        try {
        	connectDloader(d3, true, rfd3, true);
        	fail("d3 should not have been given slot");
        } catch (QueuedException qux){
        	fail("d3 should not have been queued");
        } catch (TryAgainLaterException expected){}
        
        // now kill the queued downloader
        kill(d2);
        
        // and a new d4 which should get queued.
        HTTPDownloader d4 = addUploader(upManager, rfd4, "1.1.1.4", true);
        try {
        	connectDloader(d4, true, rfd4, true);
        	fail("d4 should have been queued");
        } catch (QueuedException expected){}
    }
    
    /**
     * tests that even if the limit for a host is reached, its queued
     * requests will stay queued while polling.
     */
    public void testHostLimitExcludesQueued() throws Exception {
    	UploadSettings.HARD_MAX_UPLOADS.setValue(1);
        UploadSettings.SOFT_MAX_UPLOADS.setValue(1);
        UploadSettings.UPLOADS_PER_PERSON.setValue(2);
        UploadSettings.UPLOAD_QUEUE_SIZE.setValue(1);
        
        // take up the free slot
        HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
        connectDloader(d1,true,rfd1,true);
        
        // take up the queue slot
        HTTPDownloader d2 = addUploader(upManager, rfd2, "1.1.1.1", true);
        try {
        	connectDloader(d2, true, rfd2, true);
        	fail("d2 should have been queued");
        } catch (QueuedException expected){}
        
        // third request should get rejected
        HTTPDownloader d3 = addUploader(upManager,rfd3,"1.1.1.1",true);
    	try {
    		connectDloader(d3,true,rfd3,true);
    		fail("Host limit reached, should not have accepted d3");
    	} catch (QueuedException qx) {
    		fail("host limit reached should not queue", qx);
    	} catch (TryAgainLaterException expectedException) {
    	} catch (IOException ioe) {}
    	
    	// have the queued downloader re-poll, it should 
    	// still be queued.
    	
    	Thread.sleep(HTTPSession.MIN_POLL_TIME + HTTPSession.MAX_POLL_TIME / 2);
    	try {
    		connectDloader(d2, true, rfd2, true);
    		fail("should have been queued");
    	} catch (QueuedException expected){}
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
     *  hundredth byte -- UNUSED.
     * @exception TryAgainLaterException the downloader was denied 
     *  because upman was busy
     * @exception IOException some other exception
     */     
    private static HTTPDownloader addUploader(final UploadManager upman, 
                                              RemoteFileDesc rfd,
                                              String ip,
                                              boolean block) throws Exception{
        //Allow some fudging to prevent race conditons.
        try { Thread.sleep(1000); } catch (InterruptedException e) { }

        //PipedSocketFactory psf=new PipedSocketFactory(
                                        //"127.0.0.1", ip, block ? 5000 : -1, -1);
        PipedSocketFactory psf = new PipedSocketFactory("127.0.0.1", ip);
        //Socket A either has no limit or a limit of 1000 bytes to write.
        //Socket B has no limit
        final Socket sa=psf.getSocketA();
        Thread runner=new Thread() {
            public void run() {
                try {
                    InputStream ia = sa.getInputStream();
                    assertEquals('G', ia.read());
                    assertEquals('E', ia.read());
                    assertEquals('T', ia.read());
                    assertEquals(' ', ia.read());
                    upman.acceptUpload(HTTPRequestMethod.GET,sa, false);
                } catch(Throwable t) {
                    // make sure we know about errors.
                    ErrorService.error(t);
                }
            }
        };
        runner.setDaemon(true);
        runner.start();       

        Socket sb=psf.getSocketB();
        File tmp=File.createTempFile("UploadManager_Test", "dat");
        VerifyingFile vf = new VerifyingFile(0);
        vf.open(tmp);
        HTTPDownloader downloader = 
			new HTTPDownloader(sb, rfd, vf,true);
        tmp.delete();
        return downloader;
    }
    
    private static void connectDloader(HTTPDownloader dloader, boolean tcp, 
                                       RemoteFileDesc rfd,boolean queue) throws Exception {  
        if(tcp)
            dloader.connectTCP(0); //may throw TryAgainLater, etc.
        connectHTTP(dloader, 0, rfd.getSize(), queue);
    }
    
    private static HashTree connectThex(HTTPDownloader dloader,
                                                boolean tcp)
                                                throws Exception {
        if(tcp)
            dloader.connectTCP(0);
        addThexHeader(dloader);
        return requestHashTree(dloader);
    }
    
    private static void addThexHeader(HTTPDownloader dl) throws Exception {
        PrivilegedAccessor.invokeMethod(dl, "parseTHEXHeader",
                fm.get((int)dl.getIndex()).getHashTree().httpStringValue());
    }
    
    private static void kill(HTTPDownloader downloader) {
        downloader.stop();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) { }
    }
    
    private static void connectHTTP(HTTPDownloader dloader, int start, int stop, boolean queue) throws Exception {
        StubIOStateObserver observer = new StubIOStateObserver();
        synchronized(observer) {
            dloader.connectHTTP(start, stop, queue, 0, observer);
            observer.wait();
            dloader.parseHeaders();
        }
    }
    
    private static HashTree requestHashTree(HTTPDownloader dloader) throws Exception {
        StubIOStateObserver observer = new StubIOStateObserver();
        synchronized(observer) {
            dloader.requestHashTree(dloader.getRemoteFileDesc().getSHA1Urn(), observer);
            observer.wait();
            ConnectionStatus status = dloader.parseThexResponseHeaders();
            if(status.isConnected()) {
                dloader.downloadThexBody(dloader.getRemoteFileDesc().getSHA1Urn(), observer);
                observer.wait();
                return dloader.getHashTree();
            } else {
                return null;
            }
        }
    }
    
    
    
}
