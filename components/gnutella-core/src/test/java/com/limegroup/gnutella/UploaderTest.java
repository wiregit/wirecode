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

    public static Test suite() {
        return new TestSuite(UploaderTest.class);
    }
    
    public void testNormalQueueing() {
        UploadManager upManager = new UploadManager(ac,mr,fm);
        SettingsManager.instance().setMaxUploads(2);
        SettingsManager.instance().setSoftMaxUploads(9999);
        SettingsManager.instance().setUploadsPerPerson(99999);
        SettingsManager.instance().setUploadQueueSize(2);
        HTTPDownloader d3 = null;
        try {
            HTTPDownloader d1 = addUploader(upManager,rfd1,"1.1.1.1",true);
            HTTPDownloader d2 = addUploader(upManager,rfd2,"1.1.1.2",true);
            try {
                d3 = addUploader(upManager,rfd3,"1.1.1.3",true);
            } catch(QueuedException qx) {
                assertEquals(0, qx.getQueuePosition());
            } catch (Exception ioe) {
                ioe.printStackTrace();
                fail("not queued");
            }
            HTTPDownloader d4 = null;
            try {
                d4 = addUploader(upManager,rfd4,"1.1.1.4",true);
            } catch(QueuedException qx) {
                assertEquals(1,qx.getQueuePosition());
            } catch (Exception ioe) {
                fail("not queued");
            }
            HTTPDownloader d5 = null;
            try {
                d5 = addUploader(upManager,rfd5,"1.1.1.5",true);
            } catch (QueuedException qx) {
                fail("queued after capacity ");
            } catch (TryAgainLaterException tax) {//correct
            } catch (Exception e) {
                fail("unknown exception");
            }
            kill(d1);//now the queue should free up, but we need to request
            Thread.sleep(UploadManager.MIN_POLL_TIME);//wait till min Poll  time
            final HTTPDownloader dloader = d3;
            Thread runner = new Thread() {
                public void run() {
                    try {
                        dloader.connectHTTP(0,rfd3.getSize());                
                    } catch (QueuedException e) {
                        e.printStackTrace();
                        fail("exception thrown in connectHTTP");
                    } catch (TryAgainLaterException tlx) {
                        fail("exception thrown in connectHTTP");
                    } catch (Exception ign) {
                        ign.printStackTrace();
                    } 
                }
            };
            runner.start();
            System.out.println("Passed");
        } catch(Exception anyother) {
            System.out.println("FAILED");
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
                                              boolean block) 
        throws TryAgainLaterException, IOException {        
        //Allow some fudging to prevent race conditons.
        try { Thread.sleep(1000); } catch (InterruptedException e) { }

        PipedSocketFactory psf=new PipedSocketFactory(
                                        "127.0.0.1", ip, block ? 1000 : -1, -1);
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
        try {
            downloader.connectTCP(0); //may throw TryAgainLater, etc.
            downloader.connectHTTP(0,rfd.getSize());
            return downloader;
        } finally {
            tmp.delete();
        }
    }

    private static void kill(HTTPDownloader downloader) {
        downloader.stop();
        try { Thread.sleep(400); } catch (InterruptedException ignored) { }
    }
    
 
}
