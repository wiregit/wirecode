package com.limegroup.gnutella.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import junit.framework.Test;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.DownloadManagerStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.PrivilegedAccessor;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class ManagedDownloaderTest extends com.limegroup.gnutella.util.BaseTestCase {
    final static int PORT=6666;
    private DownloadManagerStub manager;
    private FileManager fileman;
    private ActivityCallback callback;
    private MessageRouter router;

    public ManagedDownloaderTest(String name) {
        super(name);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(ManagedDownloaderTest.class);
    }
    
    public void setUp() {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        manager = new DownloadManagerStub();
        fileman = new FileManagerStub();
        callback = new ActivityCallbackStub();
        router = new MessageRouterStub();
        manager.initialize(callback, router, fileman);
    }

    public void testLegacy() throws Exception {
        //Test removeBest
        Set urns1 = new HashSet();
        Set urns2 = new HashSet();
        try {
            urns1.add(URN.createSHA1Urn(
                         "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB"));
            urns2.add(URN.createSHA1Urn(
                         "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB"));
        } catch (IOException e) { 
			fail(e);
        }

        RemoteFileDesc rf1=new RemoteFileDesc("1.2.3.4", 6346, 0, 
                                              "some file.txt", 1010, 
                                              new byte[16], 
                                              SpeedConstants.T1_SPEED_INT, 
                                              false, 3, false, null, null,
                                              false, false,"",0,null, -1);
        RemoteFileDesc rf4=new RemoteFileDesc("1.2.3.6", 6346, 0, 
                                              "some file.txt", 1010, 
                                              new byte[16], 
                                              SpeedConstants.T3_SPEED_INT, 
                                              false, 0, false, null, null,
                                              false, false,"",0,null, -1);
        RemoteFileDesc rf5=new RemoteFileDesc("1.2.3.6", 6346, 0, 
                                              "some file.txt", 1010, 
                                              new byte[16], 
                                              SpeedConstants.T3_SPEED_INT+1, 
                                              false, 0, false, null, null,
                                              false, false,"",0,null, -1);
        RemoteFileDesc rf6=new RemoteFileDesc("1.2.3.7", 6346, 0,
                                              "some file.txt", 1010,
                                              new byte[16],
                                              SpeedConstants.MODEM_SPEED_INT,
                                              false, 0, false, null, urns1,
                                              false, false,"",0,null, -1);
        RemoteFileDesc rf7=new RemoteFileDesc("1.2.3.7", 6346, 0,
                                              "some file.txt", 1010,
                                              new byte[16],
                                              SpeedConstants.MODEM_SPEED_INT+1,
                                              false, 0, false, null, urns2,
                                              false, false,"",0,null, -1);

        List list = new LinkedList();
        list.add(rf4);
        list.add(rf6);
        list.add(rf1);
        list.add(rf5);
        list.add(rf7);
		RemoteFileDesc testRFD = null;
		
		Class[] args = new Class[1];
		args[0] = List.class;

		ManagedDownloader md = 
			new ManagedDownloader(new RemoteFileDesc[]{rf1}, 
								  new IncompleteFileManager(), null);
        PrivilegedAccessor.setValue(md, "files",
                                    list);
        
		Method m = PrivilegedAccessor.getMethod(ManagedDownloader.class, "removeBest",
												new Class[]{});
        Object[] nothing = new Object[0];
		m.setAccessible(true);
		testRFD = (RemoteFileDesc)m.invoke(md, nothing);
		assertEquals("rfds should be equal", testRFD, rf7);

		testRFD = (RemoteFileDesc)m.invoke(md, nothing);
		assertEquals("rfds should be equal", testRFD, rf6);

		testRFD = (RemoteFileDesc)m.invoke(md, nothing);
		assertEquals("rfds should be equal", testRFD, rf1);

		assertEquals("unexpected size", 2, list.size());

		assertTrue("should contain rfd", list.contains(rf4));
		assertTrue("should contain rfd", list.contains(rf5));

		testRFD = (RemoteFileDesc)m.invoke(md, nothing);
		assertEquals("rfds should be equal", testRFD, rf5);

		assertEquals("unexpected size", 1, list.size());

		assertTrue("should contain rfd", list.contains(rf4));

		testRFD = (RemoteFileDesc)m.invoke(md, nothing);
		assertEquals("rfds should be equal", testRFD, rf4);

		assertEquals("unexpected size", 0, list.size());
    }

    // requeries are gone now - now we only have user-driven queries (sort of
    // like requeries but not automatic.
    public void testNewRequery() throws Exception {
        RemoteFileDesc[] rfds={
            newRFD("Susheel_Daswani_Neil_Daswani.txt",
                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB"),
            newRFD("Susheel/cool\\Daswani.txt",
                   "urn:sha1:LSTHGIPQGSSZTS5FJUPAKPZWUGYQYPFB"),
            newRFD("Sumeet (Susheel) Anurag (Daswani)Chris.txt"),
            newRFD("Susheel/cool\\Daswani.txt",
                   "urn:sha1:XXXXGIPQGXXXXS5FJUPAKPZWUGYQYPFB")   //ignored
        };
        TestManagedDownloader downloader=new TestManagedDownloader(rfds);
        QueryRequest qr=downloader.newRequery2();
        assertNotNull("Couldn't make query", qr);
        assertTrue(qr.getQuery().equals("neil daswani susheel") ||
                   qr.getQuery().equals("neil susheel daswani") ||
                   qr.getQuery().equals("daswani neil susheel") ||
                   qr.getQuery().equals("daswani susheel neil") ||
                   qr.getQuery().equals("susheel neil daswani") ||
                   qr.getQuery().equals("susheel daswani neil"));
        assertEquals(224, qr.getMinSpeed());
        // the guid should be a lime guid but not a lime requery guid
        assertTrue((GUID.isLimeGUID(qr.getGUID())) && 
                   !(GUID.isLimeRequeryGUID(qr.getGUID())));
        assertTrue((qr.getRichQuery() == null) || 
                   (qr.getRichQuery().equals("")));
        Set urns=qr.getQueryUrns();
        assertEquals(0, urns.size());
    }

    /** Catches a bug with earlier keyword intersection code. */
    public void testNewRequery2() throws Exception {
        RemoteFileDesc[] rfds={ newRFD("LimeWire again LimeWire the 34") };
        TestManagedDownloader downloader=new TestManagedDownloader(rfds);
        QueryRequest qr=downloader.newRequery2();
        assertEquals("limewire again", qr.getQuery());
        assertEquals(new HashSet(), qr.getQueryUrns());
    }
    
    /** Tests that the progress is retained for deserialized downloaders. */
    public void testSerializedProgress() throws Exception {        
        IncompleteFileManager ifm=new IncompleteFileManager();
        RemoteFileDesc rfd=newRFD("some file.txt");
        File incompleteFile=ifm.getFile(rfd);
        int amountDownloaded=100;
        VerifyingFile vf=new VerifyingFile(false, 1024);
        vf.addInterval(new Interval(0, amountDownloaded-1));  //inclusive
        ifm.addEntry(incompleteFile, vf);

        //Start downloader, make it sure requeries, etc.
        ManagedDownloader downloader = 
            new ManagedDownloader(new RemoteFileDesc[] { rfd }, ifm, null);
        downloader.initialize(manager, fileman, callback);
        manager.requestStart(downloader);
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
            manager.requestStart(downloader);
        } catch (IOException e) {
            fail("Couldn't serialize", e);
        }

        //Check same state as before serialization.
        try { Thread.sleep(200); } catch (InterruptedException e) { }
        //assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());
        downloader.stop();
        try { Thread.sleep(1000); } catch (InterruptedException e) { }
    }

    /** Tests that the progress is not 0% when resume button is hit while
     *  requerying.  This was caused by the call to cleanup() from
     *  tryAllDownloads3() and was reported by Sam Berlin. 
     *  Changed after requeries have been shut off (requery-expunge-branch).
     */
    public void testRequeryProgress() throws Exception {
        TestUploader uploader=null;
        ManagedDownloader downloader=null;
        try {
            //Start uploader and download.
            uploader=new TestUploader("ManagedDownloaderTest", PORT);
            uploader.stopAfter(100);
            downloader=
				new ManagedDownloader(
						new RemoteFileDesc[] {newRFD("another testfile.txt")},
                        new IncompleteFileManager(), null);
            downloader.initialize(manager, 
                                  fileman,
                                  callback);
            manager.requestStart(downloader);
            //Wait for it to download until error, need to wait 
            Thread.sleep(140000);
            // no more auto requeries - so the download should be waiting for
            // input from the user
            assertEquals("should have read 100 bytes", 100, 
                         downloader.getAmountRead());
            assertEquals("should be waiting for user",
                         Downloader.WAITING_FOR_USER, 
                         downloader.getState());
            //Hit resume, make sure progress not erased.
            downloader.resume(); 
            try { Thread.sleep(2000); } catch (InterruptedException e) { }
            // you would think we'd expect wating for results here, but instead
            // we will wait for connections (since we have no one to query)
            assertEquals(Downloader.WAITING_FOR_CONNECTIONS, 
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
                fail("Couldn't create URN", e);
            }
        }        
        return new RemoteFileDesc("127.0.0.1", PORT, 13l,
                                  name, 1024,
                                  new byte[16], 56, false, 4, true, null, urns,
                                  false, false,"",0,null, -1);
    }

    /** Provides access to protected methods. */
    private static class TestManagedDownloader extends ManagedDownloader {
        public TestManagedDownloader(RemoteFileDesc[] files) {
            super(files, new IncompleteFileManager(), null);
        }

        public QueryRequest newRequery2() throws CantResumeException {
            return super.newRequery(2);
        }
    }
}
