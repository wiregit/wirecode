package com.limegroup.gnutella.downloader;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.stubs.*;
import com.limegroup.gnutella.settings.*;
import com.sun.java.util.collections.*;
import java.io.*;

public class ManagedDownloaderTest extends TestCase {
    final static int PORT=6666;

    public ManagedDownloaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(ManagedDownloaderTest.class);
    }
    
    public void setUp() {
        try {
            SettingsManager.instance().setSaveDirectory(new File("."));
            ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        } catch (IOException e) {
            fail("Couldn't set save directory");
        }
    }

    public void testLegacy() {
        ManagedDownloader.unitTest();
    }

    public void testNewRequery() {
        RemoteFileDesc[] rfds={
            newRFD("Susheel_Daswani_Neil_Daswani.txt",
                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB"),
            newRFD("Susheel Ruchika Mahesh Kyle Daswani.txt"),
            newRFD("Susheel/cool\\Daswani.txt",
                   "urn:sha1:LSTHGIPQGSSZTS5FJUPAKPZWUGYQYPFB"),
            newRFD("Sumeet (Susheel) Anurag (Daswani)Chris.txt"),
            newRFD("Susheel/cool\\Daswani.txt",
                   "urn:sha1:XXXXGIPQGXXXXS5FJUPAKPZWUGYQYPFB")   //ignored
        };
        TestManagedDownloader downloader=new TestManagedDownloader(rfds);
        try {
            QueryRequest qr=downloader.newRequery2();
            assertEquals("daswani susheel",  //"susheel daswani" also ok
                         qr.getQuery());
            assertEquals(131, qr.getMinSpeed());
            assertTrue(GUID.isLimeRequeryGUID(qr.getGUID()));
            assertEquals("", qr.getRichQuery());
            Set urns=qr.getQueryUrns();
            assertEquals(2, urns.size());
            assertTrue(urns.contains(URN.createSHA1Urn(
                "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB")));
            assertTrue(urns.contains(URN.createSHA1Urn(
                "urn:sha1:LSTHGIPQGSSZTS5FJUPAKPZWUGYQYPFB")));
        } catch (CantResumeException e) {
            fail("Couldn't make requery");
        } catch (IOException e) {
            fail("Couldn't make hash");
        }
    }

    /** Catches a bug with earlier keyword intersection code. */
    public void testNewRequery2() {
        RemoteFileDesc[] rfds={ newRFD("LimeWire again LimeWire the 34") };
        TestManagedDownloader downloader=new TestManagedDownloader(rfds);
        try {
            QueryRequest qr=downloader.newRequery2();
            assertEquals("limewire again", qr.getQuery());
            assertEquals(new HashSet(), qr.getQueryUrns());
        } catch (CantResumeException e) {
            fail("Couldn't make requery");
        }
    }
    
    /** Tests that the progress is retained for deserialized downloaders. */
    public void testSerializedProgress() {        
        DownloadManager manager=new DownloadManagerStub();
        FileManager fileman=new FileManagerStub();
        ActivityCallback callback=new ActivityCallbackStub();

        IncompleteFileManager ifm=new IncompleteFileManager();
        RemoteFileDesc rfd=newRFD("some file.txt");
        File incompleteFile=ifm.getFile(rfd);
        int amountDownloaded=100;
        VerifyingFile vf=new VerifyingFile(false);
        vf.addInterval(new Interval(0, amountDownloaded-1));  //inclusive
        ifm.addEntry(incompleteFile, vf);

        //Start downloader, make it sure requeries, etc.
        ManagedDownloader downloader=new ManagedDownloader(
            new RemoteFileDesc[] { rfd }, ifm);
        downloader.initialize(manager, fileman, callback);
        try { Thread.sleep(200); } catch (InterruptedException e) { }
        //assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());

        try {
            //Serialize it!
            ByteArrayOutputStream baos=new ByteArrayOutputStream();
            ObjectOutputStream out=new ObjectOutputStream(baos);
            out.writeObject(downloader);
            out.flush(); out.close();
            downloader.stop();

            //Deserialize it as a different instance.  Initialize.
            ObjectInputStream in=new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()));
            downloader=(ManagedDownloader)in.readObject();
            in.close();
            downloader.initialize(manager, fileman, callback);
        } catch (IOException e) {
            e.printStackTrace();
            fail("Couldn't serialize");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            fail("No class");
        }

        //Check same state as before serialization.
        try { Thread.sleep(200); } catch (InterruptedException e) { }
        //assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());
        downloader.stop();
    }

    /** Tests that the progress is not 0% when resume button is hit while
     *  requerying.  This was caused by the call to cleanup() from
     *  tryAllDownloads3() and was reported by Sam Berlin. */
    public void testRequeryProgress() {
        TestUploader uploader=null;
        ManagedDownloader downloader=null;
        try {
            //Start uploader and download.
            uploader=new TestUploader("ManagedDownloaderTest", PORT);
            uploader.stopAfter(100);
            downloader=new ManagedDownloader(
                new RemoteFileDesc[] {newRFD("some file.txt")},
                new IncompleteFileManager());
            downloader.initialize(new DownloadManagerStub(), 
                                  new FileManagerStub(),
                                  new ActivityCallbackStub());
            //Wait for it to download until error.
            try { Thread.sleep(4000); } catch (InterruptedException e) { }
            assertEquals(Downloader.WAITING_FOR_RESULTS, 
                         downloader.getState());
            assertEquals(100, 
                         downloader.getAmountRead());
            //Hit resume, make sure progress not erased.
            try { 
                downloader.resume(); 
            } catch (AlreadyDownloadingException e) {
                fail("No other downloads!");
            }
            try { Thread.sleep(1000); } catch (InterruptedException e) { }
            assertEquals(Downloader.WAITING_FOR_RESULTS, 
                         downloader.getState());
            assertEquals(100, 
                         downloader.getAmountRead());
        } finally {
            if (uploader!=null)
                uploader.stopThread();
            if (downloader!=null)
                downloader.stop();
        }
    }

    private static RemoteFileDesc newRFD(String name) {
        return newRFD(name, null);
    }

    private static RemoteFileDesc newRFD(String name, String hash) {
        Set urns=null;
        if (hash!=null) {
            urns=new HashSet(1);
            try {
                urns.add(URN.createSHA1Urn(hash));
            } catch (IOException e) {
                fail("Couldn't create URN");
            }
        }        
        return new RemoteFileDesc("127.0.0.1", PORT, 13l,
                                  name, 1024,
                                  new byte[16], 56, false, 4, true, null, urns);
    }
}

/** Provides access to protected methods. */
class TestManagedDownloader extends ManagedDownloader {
    public TestManagedDownloader(RemoteFileDesc[] files) {
        super(files);
    }

    public QueryRequest newRequery2() throws CantResumeException {
        return super.newRequery(0);
    }
}
