package com.limegroup.gnutella.downloader;

import junit.framework.*;
import com.limegroup.gnutella.*;

public class ManagedDownloaderTest extends TestCase {

    public ManagedDownloaderTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return new TestSuite(ManagedDownloaderTest.class);
    }
    
    public void testLegacy() {
        ManagedDownloader.unitTest();
    }

    public void testNewRequery() {
        RemoteFileDesc[] rfds={
            newRFD("Susheel_Daswani_Neil_Daswani.txt"),
            newRFD("Susheel Ruchika Mahesh Kyle Daswani.txt"),
            newRFD("Susheel/cool\\Daswani.txt"),
            newRFD("Sumeet (Susheel) Anurag (Daswani)Chris.txt")
        };
        TestManagedDownloader downloader=new TestManagedDownloader(rfds);
        try {
            QueryRequest qr=downloader.newRequery2();
            assertEquals("daswani susheel",  //"susheel daswani" also ok
                         qr.getQuery());
        } catch (CantResumeException e) {
            fail("Couldn't make requery");
        }
    }

    public void testNewRequery2() {
        RemoteFileDesc[] rfds={ newRFD("LimeWire again LimeWire the 34") };
        TestManagedDownloader downloader=new TestManagedDownloader(rfds);
        try {
            QueryRequest qr=downloader.newRequery2();
            assertEquals("limewire again",
                         qr.getQuery());
        } catch (CantResumeException e) {
            fail("Couldn't make requery");
        }
    }

    private static RemoteFileDesc newRFD(String name) {
        return new RemoteFileDesc("1.2.3.4", 6346, 13l,
                                  name, 1024,
                                  new byte[16], 56, false, 4, true, null, null);
    }
}

/** Provides access to protected methods. */
class TestManagedDownloader extends ManagedDownloader {
    public TestManagedDownloader(RemoteFileDesc[] files) {
        super(files);
    }

    public QueryRequest newRequery2() throws CantResumeException {
        return super.newRequery();
    }
}
