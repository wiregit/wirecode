package com.limegroup.gnutella.downloader;

import junit.framework.*;
import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;
import java.io.*;

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
            assertEquals(0, qr.getMinSpeed());
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
        return new RemoteFileDesc("1.2.3.4", 6346, 13l,
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
