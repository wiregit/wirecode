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

public class UploadManagerTest extends TestCase {


    public UploadManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(UploadManagerTest.class);
    }
    

    public void testLegacy() {
        ActivityCallback ac = new ActivityCallbackStub();
        MessageRouter mr = new MessageRouterStub();
        FileManager fm = new FileManagerStub();
        Authenticator a= new DummyAuthenticator();
        RouterService rs = new RouterService(ac,mr,fm,a);
        //Test measured upload speed code
        UploadManager upman=new UploadManager(ac, mr, fm);
        UploadManager.tBandwidthTracker(upman);

        //Test limits
        FileDesc fd=fm.get(0);
        RemoteFileDesc rfd= new RemoteFileDesc( "x.x.x.x", 0, 0, 
                               "abc.txt",100000, new byte[16], 56, 
                                false,3,false,null,null);

        upman=new UploadManager(ac,mr,fm);
        tTotalUploadLimit(upman, rfd);

        upman=new UploadManager(ac,mr,fm);
        tPerPersonUploadLimit(upman, rfd);

        upman=new UploadManager(ac,mr,fm);
        tSoftUploadLimit(upman, rfd);
    }

    private static void tTotalUploadLimit(UploadManager upman, 
                                             RemoteFileDesc rfd) {
        SettingsManager.instance().setMaxUploads(2);
        SettingsManager.instance().setSoftMaxUploads(99999);
        SettingsManager.instance().setUploadsPerPerson(99999);               
        
        System.out.print("-Testing total upload limit...");
        try {
            //Add two downloaders
            HTTPDownloader d1=addUploader(upman, rfd, "1.1.1.1", true);
            HTTPDownloader d2=addUploader(upman, rfd, "1.1.1.2", true);
            //Third is denied
            try {
                HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.3", true);
                Assert.that(false, "Downloader not denied");
            } catch (TryAgainLaterException e) {            
            }
            //But killing 1st allows third
            kill(d1);
            HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.3", true);
            //But not a fourth
            try {
                HTTPDownloader d4=addUploader(upman, rfd, "1.1.1.3", true);
                Assert.that(false, "Downloader not denied");
            } catch (TryAgainLaterException e) {            
            }
            System.out.println("passed");
        } catch (Throwable e) {
            System.out.println("FAILED!");
            e.printStackTrace();
        }
    }

    
    private static void tPerPersonUploadLimit(UploadManager upman, 
                                                 RemoteFileDesc rfd) {
        SettingsManager.instance().setMaxUploads(99999);
        SettingsManager.instance().setSoftMaxUploads(99999);
        SettingsManager.instance().setUploadsPerPerson(2);               

        System.out.print("-Testing per person upload limit...");    
        try {
            //Add two downloaders
            HTTPDownloader d1=addUploader(upman, rfd, "1.1.1.1", true);
            HTTPDownloader d2=addUploader(upman, rfd, "1.1.1.1", true);
            //Third from same address is denied
            try {
                //TODO: we have to disable blocking behavior below since
                //TryAgainLater uploads ARE included in _uploadsInProgress.
                //That's really a bug, but we can live with it.
                HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.1", false);
                Assert.that(false, "Downloader denied");
            } catch (TryAgainLaterException e) {            
            }
            //But allow another with different address in
            HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.2", true);
            //And killing d1 allows another from the first address
            kill(d1);
            HTTPDownloader d4=addUploader(upman, rfd, "1.1.1.1", true);
            System.out.println("passed");
        } catch (Throwable e) {
            System.out.println("FAILED");
            e.printStackTrace();
        }
    }
    
    private static void tSoftUploadLimit(UploadManager upman, 
                                            RemoteFileDesc rfd) {
        SettingsManager.instance().setMaxUploads(99999);
        SettingsManager.instance().setSoftMaxUploads(2);
        SettingsManager.instance().setUploadsPerPerson(99999);               

        System.out.print("-Testing soft upload limit (incomplete)...");    
        //TODO: this doesn't test that the number of slots is increased
        //if all uploaders are fast.
        try {
            //Add two downloaders
            HTTPDownloader d1=addUploader(upman, rfd, "1.1.1.1", true);
            HTTPDownloader d2=addUploader(upman, rfd, "1.1.1.2", true);
            //Third is denied
            try {
                HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.3", true);
                Assert.that(false, "Downloader denied");
            } catch (TryAgainLaterException e) {            
            }
            //But killing 1st allows third
            kill(d1);
            HTTPDownloader d3=addUploader(upman, rfd, "1.1.1.3", true);
            //But not a fourth
            try {
                HTTPDownloader d4=addUploader(upman, rfd, "1.1.1.3", true);
                Assert.that(false, "Downloader denied");
            } catch (TryAgainLaterException e) {            
            }
            System.out.println("passed");
        } catch (Throwable e) {
            System.out.println("FAILED!");
            e.printStackTrace();
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
