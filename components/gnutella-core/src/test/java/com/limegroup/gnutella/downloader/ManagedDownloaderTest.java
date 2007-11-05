package com.limegroup.gnutella.downloader;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Range;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.io.LocalSocketAddressService;
import org.limewire.net.SocketsManager;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.SaveLocationManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.IncompleteFileDescStub;
import com.limegroup.gnutella.stubs.LocalSocketAddressProviderStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.stubs.NetworkManagerStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class ManagedDownloaderTest extends LimeTestCase {
    
    private static final Log LOG = LogFactory.getLog(ManagedDownloaderTest.class);
    
    private final static int PORT=6666;
    private DownloadManagerStub downloadManager;
    private FileManagerStub fileManager;
    private GnutellaDownloaderFactory gnutellaDownloaderFactory;
    private NetworkManagerStub networkManager;
    private DownloadReferencesFactory downloadReferencesFactory;
    private Injector injector;

    public ManagedDownloaderTest(String name) {
        super(name);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite() {
        return buildTestSuite(ManagedDownloaderTest.class);
    }
    
    public void setUp() throws Exception {
        doSetUp();
    }
    
    private void doSetUp(Module... modules) throws Exception {
        List<Module> allModules = new LinkedList<Module>();
        allModules.add(new AbstractModule() {
           @Override
            protected void configure() {
               bind(ConnectionManager.class).to(ConnectionManagerStub.class);
               bind(DownloadManager.class).to(DownloadManagerStub.class);
               bind(MessageRouter.class).to(MessageRouterStub.class);
               bind(FileManager.class).to(FileManagerStub.class);
               bind(NetworkManager.class).to(NetworkManagerStub.class);
            } 
        });
        allModules.addAll(Arrays.asList(modules));
        injector = LimeTestUtils.createInjector(allModules.toArray(new Module[0]));
        ConnectionManagerStub connectionManager = (ConnectionManagerStub)injector.getInstance(ConnectionManager.class);
        connectionManager.setConnected(true);
        
        LocalSocketAddressProviderStub localSocketAddressProvider = new LocalSocketAddressProviderStub();
        localSocketAddressProvider.setLocalAddressPrivate(false);
        LocalSocketAddressService.setSocketAddressProvider(localSocketAddressProvider);
        
        downloadManager = (DownloadManagerStub)injector.getInstance(DownloadManager.class);
        fileManager = (FileManagerStub)injector.getInstance(FileManager.class);
        gnutellaDownloaderFactory = injector.getInstance(GnutellaDownloaderFactory.class);
        networkManager = (NetworkManagerStub)injector.getInstance(NetworkManager.class);
        downloadReferencesFactory = injector.getInstance(DownloadReferencesFactory.class);
        
        downloadManager.initialize();
        RequeryManager.NO_DELAY = false;
    }

    
    /**
     * tests if firewalled altlocs are added to the file descriptor
     * but not sent to uploaders.  (i.e. the informMesh method)
     */
    public void testFirewalledLocs() throws Exception {
        doSetUp(new AbstractModule() {
           @Override
            protected void configure() {
               bind(HTTPDownloaderFactory.class).to(AltLocDownloaderStubFactory.class);
               bind(DownloadWorkerFactory.class).to(AltLocWorkerStubFactory.class);
            } 
        });
        networkManager.setAcceptedIncomingConnection(true);
        
    	//first make sure we are sharing an incomplete file
    	
    	URN partialURN = URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE");
        
    	IncompleteFileDescStub partialDesc = new IncompleteFileDescStub("incomplete",partialURN,3);
    	Map<URN, FileDesc> urnMap = new HashMap<URN, FileDesc>();
    	urnMap.put(partialURN, partialDesc);
    	List<FileDesc> descList = new LinkedList<FileDesc>();
    	descList.add(partialDesc);
    	File f = new File("incomplete");
    	Map<File, FileDesc> fileMap = new HashMap<File, FileDesc>();
    	fileMap.put(f,partialDesc);
    	
    	fileManager.setUrns(urnMap);
    	fileManager.setDescs(descList);
    	fileManager.setFiles(fileMap);
    	
    	// then create an rfd from a firewalled host
    	RemoteFileDesc rfd = newPushRFD("incomplete","urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE",GUID.makeGuid());
    	
    	//test that currently we have no altlocs for the incomplete file
    	
    	FileDesc test = fileManager.getFileDescForUrn(partialURN);
    	AltLocManager altLocManager = injector.getInstance(AltLocManager.class);    	
    	assertEquals(0, altLocManager.getNumLocs(test.getSHA1Urn()));
    	
    	//add one fake downloader to the downloader list
    	Endpoint e = new Endpoint("1.2.3.5",12345);
    	RemoteFileDesc other = new RemoteFileDesc(newRFD("incomplete"),e);
    	HTTPDownloaderFactory httpDownloaderFactory =injector.getInstance(HTTPDownloaderFactory.class);
    	AltLocDownloaderStub fakeDownloader = (AltLocDownloaderStub)httpDownloaderFactory.create(null, other, null, false);
    	
    	TestManagedDownloader md = new TestManagedDownloader(new RemoteFileDesc[]{rfd}, downloadManager);
        DownloadWorkerFactory downloadWorkerFactory = injector.getInstance(DownloadWorkerFactory.class);
        AltLocWorkerStub worker = (AltLocWorkerStub)downloadWorkerFactory.create(md, rfd, null);
        worker.setHTTPDownloader(fakeDownloader);
        
        List<DownloadWorker> l = new LinkedList<DownloadWorker>();
        l.add(worker);
    	md.initialize(downloadReferencesFactory.create(md));
    	md.setDloaders(l);
    	md.setSHA1(partialURN);
    	md.setIncompleteFile(f);
    	
    	
    	//and see if it behaves correctly
    	md.informMesh(rfd,true);
    	
    	//make sure the downloader did not get notified
    	assertFalse(fakeDownloader._addedFailed);
    	assertFalse(fakeDownloader._addedSuccessfull);
    	
    	//the altloc should have been added to the file descriptor
    	test = fileManager.getFileDescForUrn(partialURN);
    	assertEquals(1, altLocManager.getNumLocs(test.getSHA1Urn()));
    	
    	//now repeat the test, pretending the uploader wants push altlocs
    	fakeDownloader.setWantsFalts(true);
    	
    	//and see if it behaves correctly
    	md.informMesh(rfd,true);
    	
    	//make sure the downloader did get notified
    	assertFalse(fakeDownloader._addedFailed);
    	assertTrue(fakeDownloader._addedSuccessfull);
    	
    	//make sure the file was added to the file descriptor
    	test = fileManager.getFileDescForUrn(partialURN);
    	assertEquals(1, altLocManager.getNumLocs(test.getSHA1Urn()));
    	
    	//rince and repeat, saying this was a bad altloc. 
    	//it should be sent to the other downloaders. 
    	fakeDownloader._addedSuccessfull=false;
    	
    	md.informMesh(rfd,false);
    	
    	//make sure the downloader did get notified
    	assertTrue(fakeDownloader._addedFailed);
    	assertFalse(fakeDownloader._addedSuccessfull);
    	
    	//make sure the altloc is demoted now
    	assertEquals(0, altLocManager.getNumLocs(test.getSHA1Urn()));
    }
    
    // requeries are gone now - now we only have user-driven queries (sort of
    // like requeries but not automatic.
    public void testNewRequery() throws Exception {
        LOG.info("testing new requery");
        RemoteFileDesc[] rfds={
            newRFD("Susheel_Daswani_Neil_Daswani.txt",
                   "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB"),
            newRFD("Susheel/cool\\Daswani.txt",
                   "urn:sha1:LSTHGIPQGSSZTS5FJUPAKPZWUGYQYPFB"),
            newRFD("Sumeet (Susheel) Anurag (Daswani)Chris.txt"),
            newRFD("Susheel/cool\\Daswani.txt",
                   "urn:sha1:XXXXGIPQGXXXXS5FJUPAKPZWUGYQYPFB")   //ignored
        };
        TestManagedDownloader downloader=new TestManagedDownloader(rfds,downloadManager);
        downloader.initialize(downloadReferencesFactory.create(downloader));
        QueryRequest qr=downloader.newRequery2();
        assertNotNull("Couldn't make query", qr);
        
        assertTrue(qr.getQuery().equals("neil daswani susheel") ||
                   qr.getQuery().equals("neil susheel daswani") ||
                   qr.getQuery().equals("daswani neil susheel") ||
                   qr.getQuery().equals("daswani susheel neil") ||
                   qr.getQuery().equals("susheel neil daswani") ||
                   qr.getQuery().equals("susheel daswani neil"));
        // minspeed mask | firewalled | xml = 224
        assertEquals(224, qr.getMinSpeed());
        // the guid should be a lime guid but not a lime requery guid
        
        assertTrue((GUID.isLimeGUID(qr.getGUID())) && 
                   !(GUID.isLimeRequeryGUID(qr.getGUID())));
        assertTrue((qr.getRichQuery() == null) || 
                   (qr.getRichQuery().equals("")));
        Set urns=qr.getQueryUrns();
        assertEquals(0, urns.size());
        
        networkManager.setCanDoFWT(true);
        qr=downloader.newRequery2();
        // minspeed mask | firewalled | xml | firewall transfer = 226
        assertEquals(226, qr.getMinSpeed());
            
        
    }

    /** Catches a bug with earlier keyword intersection code. */
    public void testNewRequery2() throws Exception {
        LOG.info("testing new requery 2");
        RemoteFileDesc[] rfds={ newRFD("LimeWire again LimeWire the 34") };
        TestManagedDownloader downloader=new TestManagedDownloader(rfds,downloadManager);
        downloader.initialize(downloadReferencesFactory.create(downloader));
        QueryRequest qr=downloader.newRequery2();
        assertEquals("limewire again", qr.getQuery());
        assertEquals(new HashSet(), qr.getQueryUrns());
    }
    
    /** Tests that the progress is retained for deserialized downloaders. */
    public void testSerializedProgress() throws Exception {
        LOG.info("test serialized progress");
        
        networkManager.setAddress(new byte[] { 127, 0, 0, 1 });
        
        IncompleteFileManager ifm=new IncompleteFileManager();
        RemoteFileDesc rfd=newRFD("some file.txt",FileDescStub.DEFAULT_URN.toString());
        File incompleteFile=ifm.getFile(rfd);
        int amountDownloaded=100;
        
        VerifyingFileFactory verifyingFileFactory = injector.getInstance(VerifyingFileFactory.class);
        
        VerifyingFile vf=verifyingFileFactory.createVerifyingFile(1024);
        vf.addInterval(Range.createRange(0, amountDownloaded-1));  //inclusive
        ifm.addEntry(incompleteFile, vf, false);

        //Start downloader, make it sure requeries, etc.
        ManagedDownloader downloader = 
            gnutellaDownloaderFactory.createManagedDownloader(new RemoteFileDesc[] { rfd },
                ifm, null);
        downloader.initialize(downloadReferencesFactory.create(downloader));
        requestStart(downloader);
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
            downloader.initialize(downloadReferencesFactory.create(downloader));
            requestStart(downloader);
        } catch (IOException e) {
            fail("Couldn't serialize", e);
        }

        //Check same state as before serialization.
        try {Thread.sleep(500);}catch(InterruptedException ignroed){}
        //assertEquals(Downloader.WAITING_FOR_RESULTS, downloader.getState());
        assertEquals(amountDownloaded, downloader.getAmountRead());
        downloader.stop();
        Thread.sleep(1000); 
    }
    
    /** Tests that the progress is not 0% when resume button is hit while
     *  requerying.  This was caused by the call to cleanup() from
     *  tryAllDownloads3() and was reported by Sam Berlin. 
     *  Changed after requeries have been shut off (requery-expunge-branch).
     */
    public void testRequeryProgress() throws Exception {
        LOG.info("test requery progress");
        TestUploader uploader=null;
        ManagedDownloader downloader=null;
	// TestFile's static initializer does all kinds of expensive stuff
	// that can cause the http communication to timeout when it's
	// done lazily
	TestFile.length();
        try {
            //Start uploader and download.
            uploader = new TestUploader(injector.getInstance(AlternateLocationFactory.class), injector.getInstance(NetworkManager.class));
            uploader.start("ManagedDownloaderTest", PORT, false);
            uploader.stopAfter(500);
            uploader.setSendThexTreeHeader(false);
            uploader.setSendThexTree(false);
            downloader=
				gnutellaDownloaderFactory.createManagedDownloader(
                        new RemoteFileDesc[] {newRFD("another testfile.txt",FileDescStub.DEFAULT_URN.toString())}, new IncompleteFileManager(), null);
            downloader.initialize(downloadReferencesFactory.create(downloader));
            requestStart(downloader);
            //Wait for it to download until error, need to wait 
            uploader.waitForUploaderToStop();
        
            try { Thread.sleep(65 * 1000); } catch (InterruptedException ie) { }
            // no more auto requeries - so the download should be waiting for
            // input from the user
            assertEquals("should have read 500 bytes", 500, 
                         downloader.getAmountRead());
            assertEquals("should be waiting for user",
                    DownloadStatus.WAITING_FOR_USER, 
                         downloader.getState());
            //Hit resume, make sure progress not erased.
            downloader.resume(); 
            try { Thread.sleep(2000); } catch (InterruptedException e) { }
            // you would think we'd expect wating for results here, but instead
            // we will wait for connections (since we have no one to query)
            assertEquals(DownloadStatus.WAITING_FOR_CONNECTIONS, 
                         downloader.getState());
            assertEquals(500, 
                         downloader.getAmountRead());
        } finally {
            if (uploader!=null)
                uploader.stopThread();
            if (downloader!=null)
                downloader.stop();
        }
    }

    public void testSetSaveFileExceptionPathNameTooLong() throws Exception {
        RemoteFileDesc[] rfds = new RemoteFileDesc[] { newRFD("download") };
        File saveDir = createMaximumPathLengthDirectory();
        
        try {
            gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()), saveDir, "does not matter", false);
            fail("No exception thrown for dir " + saveDir);
        }
        catch (SaveLocationException sle) {
            assertEquals("Parent dir should exceed max path length",
                    SaveLocationException.PATH_NAME_TOO_LONG,
                    sle.getErrorCode());
        }
        finally {
            File parent = saveDir;
            while (parent.getName().startsWith("zzzzzzz")) {
                saveDir = parent;
                parent = parent.getParentFile();
            }
            FileUtils.deleteRecursive(saveDir);
        }
    }

	public void testSetSaveFileExceptions() throws Exception {
		
		RemoteFileDesc[] rfds = new RemoteFileDesc[] { newRFD("download") };
		File file = File.createTempFile("existing", "file");
		file.deleteOnExit();

		File noWritePermissionDir = null;
		if (OSUtils.isLinux()) {
			noWritePermissionDir = new File("/");
		}
		else if (OSUtils.isWindows()) {
			// doesn't work on 
//			noWritePermissionDir = new File("C:\\WINDOWS\\");
		}
		if (noWritePermissionDir != null) {
			try {
				gnutellaDownloaderFactory.createManagedDownloader(rfds,
                        new IncompleteFileManager(), new GUID(GUID.makeGuid()), noWritePermissionDir, "does not matter", false);
				fail("No exception thrown for dir " + noWritePermissionDir);
			}
			catch (SaveLocationException sle) {
				assertEquals("Should have no write permissions",
						SaveLocationException.DIRECTORY_NOT_WRITEABLE,
						sle.getErrorCode());
			}
		}
        
		try {
			gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()), new File("/non existent directory"), null, false);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: directory does not exist",
						 SaveLocationException.DIRECTORY_DOES_NOT_EXIST,
						 sle.getErrorCode());
		}
		try {
			gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()), file.getParentFile(), file.getName(), false);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: file exists",
						 SaveLocationException.FILE_ALREADY_EXISTS,
						 sle.getErrorCode());
		}
		try {
			// should not throw an exception because of overwrite 
			gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()), file.getParentFile(), file.getName(), true); 
		}
		catch (SaveLocationException sle) {
			fail("There shouldn't have been an exception of type " + sle.getErrorCode());
		}
		try {
			File f = File.createTempFile("notadirectory", "file");
			f.deleteOnExit();
			gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()), f, null, false);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: file not a directory", 
						 SaveLocationException.NOT_A_DIRECTORY,
						 sle.getErrorCode());
		}
        // we force filename normalization
        try {
            gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()), null, "./", false);
            gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()), null, "../myfile.txt", false);
        }
        catch (SaveLocationException sle) {
            fail("Should not have thrown" + sle);
        }
		try {
			// should not throw an exception
			gnutellaDownloaderFactory.createManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()), null, null, false);
		}
		catch (SaveLocationException sle) {
			fail("There shouldn't have been an exception of type " + sle.getErrorCode());
		}

		List<RemoteFileDesc> emptyRFDList = Collections.emptyList();
		// already downloading based on filename and same size
		try {
			downloadManager.download(rfds, emptyRFDList, 
					new GUID(GUID.makeGuid()), false, null, null);
		}
		catch (SaveLocationException sle) {
			fail("There should not have been an exception of type " + sle.getErrorCode());
		}
		try {
			downloadManager.download(rfds, emptyRFDList,
					new GUID(GUID.makeGuid()), false, null, null);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: already downloading", 
					SaveLocationException.FILE_ALREADY_DOWNLOADING,
					sle.getErrorCode());
		}
		
		// already downloading based on hash
		RemoteFileDesc[] hashRFDS = new RemoteFileDesc[] {
				newRFD("dl", "urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB")
		};
		try {
			downloadManager.download(hashRFDS, emptyRFDList,
					new GUID(GUID.makeGuid()), false, null, null);
		}
		catch (SaveLocationException sle) {
			fail("There should not have been an exception of type " + sle.getErrorCode());
		}
		try {
			downloadManager.download(hashRFDS, emptyRFDList,
					new GUID(GUID.makeGuid()), false, null, null);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: already downloading", 
					SaveLocationException.FILE_ALREADY_DOWNLOADING,
					sle.getErrorCode());
		}
				
		// other download is already being saved to the same file with different hashes
		try {
			RemoteFileDesc[] fds = new RemoteFileDesc[] {
					newRFD("savedto", "urn:sha1:QLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB")
			};
			downloadManager.download(fds, emptyRFDList,
					new GUID(GUID.makeGuid()), false, null, "alreadysavedto");
		}
		catch (SaveLocationException sle) {
			fail("There should not have been an exception of type " + sle.getErrorCode());
		}
		try {
			RemoteFileDesc[] fds = new RemoteFileDesc[] {
					newRFD("otherfd", "urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB")
			};
			downloadManager.download(fds, emptyRFDList,
					new GUID(GUID.makeGuid()), false, null, "alreadysavedto");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: already being saved to", 
					SaveLocationException.FILE_IS_ALREADY_DOWNLOADED_TO,
					sle.getErrorCode());
		}
		
		// TODO SaveLocationException.FILE_ALREADY_SAVED
		// SaveLocationException.FILESYSTEM_ERROR is not really reproducible
	}
	
    
    private static RemoteFileDesc newRFD(String name) {
        return newRFD(name, null);
    }

    private static RemoteFileDesc newRFD(String name, String hash) {
        Set<URN> urns=null;
        if (hash!=null) {
            urns=new HashSet<URN>(1);
            try {
                urns.add(URN.createSHA1Urn(hash));
            } catch (IOException e) {
                fail("Couldn't create URN", e);
            }
        }        
        return new RemoteFileDesc("127.0.0.1", PORT, 13l,
                                  name, 1024,
                                  new byte[16], 56, false, 4, true, null, urns,
                                  false, false,"",null, -1, false);
    }
    
    
    @SuppressWarnings("unchecked")
    private void requestStart(ManagedDownloader dl) throws Exception {
        List<AbstractDownloader> waiting = (List<AbstractDownloader>)PrivilegedAccessor.getValue(downloadManager, "waiting");
        List<AbstractDownloader> active = (List<AbstractDownloader>)PrivilegedAccessor.getValue(downloadManager, "active");
        synchronized(downloadManager) {
            waiting.remove(dl);
            active.add(dl);
            dl.startDownload();
        }
    }

    
    private RemoteFileDesc newPushRFD(String name, String hash, byte[] guid) throws Exception {

        IpPort ppi = new IpPortImpl("1.2.3.10", 2000);

        Set<IpPort> ppis = new TreeSet<IpPort>(IpPort.COMPARATOR);
        ppis.add(ppi);

        PushEndpointFactory pushEndpointFactory = injector.getInstance(PushEndpointFactory.class);
        PushEndpoint pe = pushEndpointFactory.createPushEndpoint(guid, ppis);
        pe.updateProxies(true);

        return new RemoteFileDesc(newRFD(name, hash), pe);
    }
    
    private File createMaximumPathLengthDirectory() throws IOException {
        File tmpFile = File.createTempFile("temp", "file");
        File tmpDir = tmpFile.getParentFile();
        tmpFile.delete();
        
        char[] dirName = new char[OSUtils.getMaxPathLength() - tmpDir.getAbsolutePath().length()];
        Arrays.fill(dirName, 'z');
        for (int i = 0; i < dirName.length; i += 254) {
            dirName[i] = File.separatorChar;
        }
        File longestDir = new File(tmpDir.getAbsolutePath() + new String(dirName));
        // currently fails due to JRE bug
        if (!longestDir.mkdirs()) {
            // go through path and create them one by one till JRE bug is fixed
            LinkedList<File> dirs = new LinkedList<File>();
            File current = longestDir;
            while (!current.equals(tmpDir)) {
                dirs.add(current);
                current = current.getParentFile();
            }
            while (!dirs.isEmpty()) {
                current = dirs.removeLast();
                assertTrue("Could not create " + current, current.mkdir());
            }
        }
        return longestDir;
    }
    

    /** Provides access to protected methods. */
    private static class TestManagedDownloader extends ManagedDownloader {
        public TestManagedDownloader(RemoteFileDesc[] files, SaveLocationManager saveLocationManager) {
            super(files, new IncompleteFileManager(), null, saveLocationManager);
        }

        public QueryRequest newRequery2() throws CantResumeException {
            return super.newRequery(2);
        }
        
        /**
         * allows overriding of the list of current downloaders
         */
        public void setDloaders(List l) throws Exception {
            PrivilegedAccessor.setValue(this,"_activeWorkers",l);
        }
        
        public void setSHA1(URN sha1) throws Exception{
        	PrivilegedAccessor.setValue(this,"downloadSHA1",sha1);
        }
        
        public void setIncompleteFile(File f) throws Exception {
        	PrivilegedAccessor.setValue(this,"incompleteFile",f);
        }
    }
    
    @Singleton    
    private static class AltLocDownloaderStubFactory implements HTTPDownloaderFactory {

        private final NetworkManager networkManager;
        private final AlternateLocationFactory alternateLocationFactory;
        private final DownloadManager downloadManager;
        private final Provider<CreationTimeCache> creationTimeCache;
        private final BandwidthManager bandwidthManager;
        private final Provider<PushEndpointCache> pushEndpointCache;

        @Inject
        public AltLocDownloaderStubFactory(NetworkManager networkManager,
                AlternateLocationFactory alternateLocationFactory,
                DownloadManager downloadManager,
                Provider<CreationTimeCache> creationTimeCache,
                BandwidthManager bandwidthManager,
                Provider<PushEndpointCache> pushEndpointCache) {
            this.networkManager = networkManager;
            this.alternateLocationFactory = alternateLocationFactory;
            this.downloadManager = downloadManager;
            this.creationTimeCache = creationTimeCache;
            this.bandwidthManager = bandwidthManager;
            this.pushEndpointCache = pushEndpointCache;
        }
        
        public HTTPDownloader create(Socket socket, RemoteFileDesc rfd,
                VerifyingFile incompleteFile, boolean inNetwork) {
            return new AltLocDownloaderStub(rfd, incompleteFile,
                    networkManager, alternateLocationFactory, downloadManager,
                    creationTimeCache.get(), bandwidthManager, pushEndpointCache);
        }

    }
    
    private static class AltLocDownloaderStub extends HTTPDownloaderStub {
    	private boolean _stubFalts;
        private final RemoteFileDesc rfd;
        
    	public AltLocDownloaderStub(RemoteFileDesc rfd, VerifyingFile incompleteFile,
                NetworkManager networkManager, AlternateLocationFactory alternateLocationFactory,
                DownloadManager downloadManager, CreationTimeCache creationTimeCache,
                BandwidthManager bandwidthManager, Provider<PushEndpointCache> pushEndpointCache) {
            super(rfd, null, networkManager, alternateLocationFactory, downloadManager,
                    creationTimeCache, bandwidthManager, pushEndpointCache);
            this.rfd = rfd;
        }
    	
    	public boolean _addedFailed,_addedSuccessfull;
   		public void addFailedAltLoc(AlternateLocation loc) {
   			_addedFailed = true;
		}
		public void addSuccessfulAltLoc(AlternateLocation loc) {
			_addedSuccessfull=true;
		}
		
		public RemoteFileDesc getRemoteFileDesc() {
			return rfd;
		}
		
		public void setWantsFalts(boolean doesIt) {
			_stubFalts=doesIt;
		}
		
		public boolean wantsFalts(){
			return _stubFalts;
		}
    }
    
    @Singleton
    private static class AltLocWorkerStubFactory implements DownloadWorkerFactory {
        private final HTTPDownloaderFactory httpDownloaderFactory;
        private final ScheduledExecutorService backgroundExecutor;
        private final ScheduledExecutorService nioExecutor;
        private final Provider<PushDownloadManager> pushDownloadManager;
        private final SocketsManager socketsManager;
        
        @Inject
        public AltLocWorkerStubFactory(
                HTTPDownloaderFactory httpDownloaderFactory,
                @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor,
                @Named("nioExecutor") ScheduledExecutorService nioExecutor,
                Provider<PushDownloadManager> pushDownloadManager,
                SocketsManager socketsManager) {
            this.httpDownloaderFactory = httpDownloaderFactory;
            this.backgroundExecutor = backgroundExecutor;
            this.nioExecutor = nioExecutor;
            this.pushDownloadManager = pushDownloadManager;
            this.socketsManager = socketsManager;
        }
        
        public DownloadWorker create(ManagedDownloader manager,
                RemoteFileDesc rfd, VerifyingFile vf) {
            return new AltLocWorkerStub(manager, rfd, vf, httpDownloaderFactory,
                    backgroundExecutor, nioExecutor, pushDownloadManager,
                    socketsManager);
        }
    }
    
    private static class AltLocWorkerStub extends DownloadWorkerStub {
        private volatile HTTPDownloader httpDownloader;
        
        public AltLocWorkerStub(ManagedDownloader manager, RemoteFileDesc rfd, VerifyingFile vf,
                HTTPDownloaderFactory httpDownloaderFactory,
                ScheduledExecutorService backgroundExecutor, ScheduledExecutorService nioExecutor,
                Provider<PushDownloadManager> pushDownloadManager, SocketsManager socketsManager) {
            super(manager, rfd, vf, httpDownloaderFactory, backgroundExecutor, nioExecutor,
                    pushDownloadManager, socketsManager);
        }

        public void setHTTPDownloader(HTTPDownloader httpDownloader) {
            this.httpDownloader = httpDownloader;
        }

        public HTTPDownloader getDownloader() {
            return httpDownloader;
        }
    }
}
