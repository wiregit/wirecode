package com.limegroup.gnutella.downloader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.UDPService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.IncompleteFileDescStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.PrivilegedAccessor;

public class ManagedDownloaderTest extends com.limegroup.gnutella.util.BaseTestCase {
    
    private static final Log LOG = LogFactory.getLog(ManagedDownloaderTest.class);
    
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
    
    public static void globalSetUp() throws Exception{
        ConnectionManagerStub cmStub = new ConnectionManagerStub() {
            public boolean isConnected() {
                return true;
            }
        };
        
        PrivilegedAccessor.setValue(RouterService.class,"manager",cmStub);
        
        assertTrue(RouterService.isConnected());
    }
    
    public void setUp() throws Exception {
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        manager = new DownloadManagerStub();
        fileman = new FileManagerStub();
        callback = new ActivityCallbackStub();
        router = new MessageRouterStub();
        manager.initialize(callback, router, fileman);
        RouterService.getAltlocManager().purge();
        PrivilegedAccessor.setValue(RouterService.class,"callback",callback);
        PrivilegedAccessor.setValue(RouterService.class,"router",router);
        PrivilegedAccessor.setValue(RouterService.class,"downloadManager",manager);
    }

    
    /**
     * tests if firewalled altlocs are added to the file descriptor
     * but not sent to uploaders.  (i.e. the informMesh method)
     */
    public void testFirewalledLocs() throws Exception {
    	LOG.info("testing firewalled locations");
        PrivilegedAccessor.setValue(RouterService.getAcceptor(),
                "_acceptedIncoming",new Boolean(true));
        assertTrue(RouterService.acceptedIncomingConnection());
        
    	//first make sure we are sharing an incomplete file
    	
    	URN partialURN = 
    		URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE");
    	
    	AlternateLocationCollection 
			col = AlternateLocationCollection.create(partialURN);
    	
    	IncompleteFileDescStub partialDesc = 
    		new IncompleteFileDescStub("incomplete",partialURN,3);
    	
    	partialDesc.setAlternateLocationCollection(col);
    	
    	Map urnMap = new HashMap(); urnMap.put(partialURN,partialDesc);
    	List descList = new LinkedList();descList.add(partialDesc);
    	File f = new File("incomplete");
    	Map fileMap = new HashMap();fileMap.put(f,partialDesc);
    	
    	FileManagerStub newFMStub = new FileManagerStub(urnMap,descList);
    	newFMStub.setFiles(fileMap);
    	
    	PrivilegedAccessor.setValue(RouterService.class,"fileManager",newFMStub);
    	
    	// then create an rfd from a firewalled host
    	RemoteFileDesc rfd = 
    		newPushRFD("incomplete","urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE",GUID.makeGuid());
    	
    	//test that currently we have no altlocs for the incomplete file
    	
    	FileDesc test = RouterService.getFileManager().getFileDescForUrn(partialURN);
    	
    	assertEquals(0,RouterService.getAltlocManager().getNumLocs(test.getSHA1Urn()));
    	
    	//add one fake downloader to the downloader list
    	Endpoint e = new Endpoint("1.2.3.5",12345);
    	RemoteFileDesc other = new RemoteFileDesc(newRFD("incomplete"),e);
    	AltlocDownloaderStub fakeDownloader = new AltlocDownloaderStub(other);
    	
    	TestManagedDownloader md = new TestManagedDownloader(new RemoteFileDesc[]{rfd});
        AltLocWorkerStub worker = new AltLocWorkerStub(md,rfd,fakeDownloader);
        
        List l = new LinkedList();l.add(worker);
    	md.initialize(manager,newFMStub,callback);
    	md.setDloaders(l);
    	md.setSHA1(partialURN);
    	md.setFM(newFMStub);
    	md.setIncompleteFile(f);
    	
    	
    	//and see if it behaves correctly
    	md.informMesh(rfd,true);
    	
    	//make sure the downloader did not get notified
    	assertFalse(fakeDownloader._addedFailed);
    	assertFalse(fakeDownloader._addedSuccessfull);
    	
    	//the altloc should have been added to the file descriptor
    	test = RouterService.getFileManager().getFileDescForUrn(partialURN);
    	assertEquals(1,RouterService.getAltlocManager().getNumLocs(test.getSHA1Urn()));
    	
    	//now repeat the test, pretending the uploader wants push altlocs
    	fakeDownloader.setWantsFalts(true);
    	
    	//and see if it behaves correctly
    	md.informMesh(rfd,true);
    	
    	//make sure the downloader did get notified
    	assertFalse(fakeDownloader._addedFailed);
    	assertTrue(fakeDownloader._addedSuccessfull);
    	
    	//make sure the file was added to the file descriptor
    	test = RouterService.getFileManager().getFileDescForUrn(partialURN);
    	assertEquals(1,RouterService.getAltlocManager().getNumLocs(test.getSHA1Urn()));
    	
    	//rince and repeat, saying this was a bad altloc. 
    	//it should be sent to the other downloaders. 
    	fakeDownloader._addedSuccessfull=false;
    	
    	md.informMesh(rfd,false);
    	
    	//make sure the downloader did get notified
    	assertTrue(fakeDownloader._addedFailed);
    	assertFalse(fakeDownloader._addedSuccessfull);
    	
    	//make sure the altloc is demoted now
    	assertEquals(0,RouterService.getAltlocManager().getNumLocs(test.getSHA1Urn()));
    	
    	PrivilegedAccessor.setValue(RouterService.getAcceptor(),
                "_acceptedIncoming",new Boolean(false));
        assertFalse(RouterService.acceptedIncomingConnection());
    	
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
        TestManagedDownloader downloader=new TestManagedDownloader(rfds);
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
        
        UDPService.instance().setReceiveSolicited(true);
        qr=downloader.newRequery2();
        // minspeed mask | firewalled | xml | firewall transfer = 226
        assertEquals(226, qr.getMinSpeed());
            
        
    }

    /** Catches a bug with earlier keyword intersection code. */
    public void testNewRequery2() throws Exception {
        LOG.info("testing new requery 2");
        RemoteFileDesc[] rfds={ newRFD("LimeWire again LimeWire the 34") };
        TestManagedDownloader downloader=new TestManagedDownloader(rfds);
        QueryRequest qr=downloader.newRequery2();
        assertEquals("limewire again", qr.getQuery());
        assertEquals(new HashSet(), qr.getQueryUrns());
    }
    
    /** Tests that the progress is retained for deserialized downloaders. */
    public void testSerializedProgress() throws Exception {     
        LOG.info("test serialized progress");
        IncompleteFileManager ifm=new IncompleteFileManager();
        RemoteFileDesc rfd=newRFD("some file.txt",FileDescStub.DEFAULT_URN.toString());
        File incompleteFile=ifm.getFile(rfd);
        int amountDownloaded=100;
        VerifyingFile vf=new VerifyingFile(1024);
        vf.addInterval(new Interval(0, amountDownloaded-1));  //inclusive
        ifm.addEntry(incompleteFile, vf);

        //Start downloader, make it sure requeries, etc.
        ManagedDownloader downloader = 
            new ManagedDownloader(new RemoteFileDesc[] { rfd }, ifm, null);
        downloader.initialize(manager, fileman, callback);
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
            downloader.initialize(manager, fileman, callback);
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
        try {
            //Start uploader and download.
            uploader=new TestUploader("ManagedDownloaderTest", PORT);
            uploader.stopAfter(500);
            uploader.setSendThexTreeHeader(false);
            uploader.setSendThexTree(false);
            downloader=
				new ManagedDownloader(
						new RemoteFileDesc[] {newRFD("another testfile.txt",FileDescStub.DEFAULT_URN.toString())},
                        new IncompleteFileManager(), null);
            downloader.initialize(manager, 
                                  fileman,
                                  callback);
            requestStart(downloader);
            //Wait for it to download until error, need to wait 
            Thread.sleep(140000);
            // no more auto requeries - so the download should be waiting for
            // input from the user
            assertEquals("should have read 500 bytes", 500, 
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
            assertEquals(500, 
                         downloader.getAmountRead());
        } finally {
            if (uploader!=null)
                uploader.stopThread();
            if (downloader!=null)
                downloader.stop();
        }
    }


	public void testSetSaveFileExceptions() throws Exception {
		
		RemoteFileDesc[] rfds = new RemoteFileDesc[] { newRFD("download") };
		File file = File.createTempFile("existing", "file");
		file.deleteOnExit();

		File noWritePermissionDir = null;
		if (CommonUtils.isLinux()) {
			noWritePermissionDir = new File("/");
		}
		else if (CommonUtils.isWindows()) {
			// doesn't work on 
//			noWritePermissionDir = new File("C:\\WINDOWS\\");
		}
		if (noWritePermissionDir != null) {
			try {
				new ManagedDownloader(rfds,
						new IncompleteFileManager(), new GUID(GUID.makeGuid()),
						noWritePermissionDir, "does not matter", false);
				fail("No exception thrown for dir " + noWritePermissionDir);
			}
			catch (SaveLocationException sle) {
				assertEquals("Should have no write permissions",
						SaveLocationException.DIRECTORY_NOT_WRITEABLE,
						sle.getErrorCode());
			}
		}
        
		try {
			new ManagedDownloader(rfds,
					new IncompleteFileManager(), new GUID(GUID.makeGuid()),
					new File("/non existent directory"), null, false);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: directory does not exist",
						 SaveLocationException.DIRECTORY_DOES_NOT_EXIST,
						 sle.getErrorCode());
		}
		try {
			new ManagedDownloader(rfds,
					new IncompleteFileManager(), new GUID(GUID.makeGuid()),
					file.getParentFile(), file.getName(), false);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: file exists",
						 SaveLocationException.FILE_ALREADY_EXISTS,
						 sle.getErrorCode());
		}
		try {
			// should not throw an exception because of overwrite 
			new ManagedDownloader(rfds,	new IncompleteFileManager(), 
					new GUID(GUID.makeGuid()), file.getParentFile(), file.getName(),
					true); 
		}
		catch (SaveLocationException sle) {
			fail("There shouldn't have been an exception of type " + sle.getErrorCode());
		}
		try {
			File f = File.createTempFile("notadirectory", "file");
			f.deleteOnExit();
			new ManagedDownloader(rfds,
					new IncompleteFileManager(), new GUID(GUID.makeGuid()),
					f, null, false);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: file not a directory", 
						 SaveLocationException.NOT_A_DIRECTORY,
						 sle.getErrorCode());
		}
		try {
			new ManagedDownloader(rfds,
					new IncompleteFileManager(), new GUID(GUID.makeGuid()),
					null, "./", false);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
			assertEquals("Error code should be: file not regular",
						 SaveLocationException.FILE_NOT_REGULAR,
						 sle.getErrorCode());
		}
		try {
			new ManagedDownloader(rfds,
					new IncompleteFileManager(), new GUID(GUID.makeGuid()),
					null, "../myfile.txt", false);
			fail("No exception thrown");
		}
		catch (SaveLocationException sle) {
//			assertEquals("Error code should be: security violation", 
//						 SaveLocationException.SECURITY_VIOLATION,
//						 sle.getErrorCode());
		}
		try {
			// should not throw an exception
			new ManagedDownloader(rfds, new IncompleteFileManager(), 
					new GUID(GUID.makeGuid()), null, null, false);
		}
		catch (SaveLocationException sle) {
			fail("There shouldn't have been an exception of type " + sle.getErrorCode());
		}

		// already downloading based on filename and same size
		try {
			manager.download(rfds, Collections.EMPTY_LIST, 
					new GUID(GUID.makeGuid()), false, null, null);
		}
		catch (SaveLocationException sle) {
			fail("There should not have been an exception of type " + sle.getErrorCode());
		}
		try {
			manager.download(rfds, Collections.EMPTY_LIST,
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
			manager.download(hashRFDS, Collections.EMPTY_LIST,
					new GUID(GUID.makeGuid()), false, null, null);
		}
		catch (SaveLocationException sle) {
			fail("There should not have been an exception of type " + sle.getErrorCode());
		}
		try {
			manager.download(hashRFDS, Collections.EMPTY_LIST,
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
			manager.download(fds, Collections.EMPTY_LIST,
					new GUID(GUID.makeGuid()), false, null, "alreadysavedto");
		}
		catch (SaveLocationException sle) {
			fail("There should not have been an exception of type " + sle.getErrorCode());
		}
		try {
			RemoteFileDesc[] fds = new RemoteFileDesc[] {
					newRFD("otherfd", "urn:sha1:PLSTHIPQGSSZTS5FJUPAKOZWUGZQYPFB")
			};
			manager.download(fds, Collections.EMPTY_LIST,
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
                                  false, false,"",null, -1);
    }
    
    private void requestStart(ManagedDownloader dl) throws Exception {
        List waiting = (List)PrivilegedAccessor.getValue(manager, "waiting");
        List active = (List)PrivilegedAccessor.getValue(manager, "active");
        synchronized(manager) {
            waiting.remove(dl);
            active.add(dl);
            dl.startDownload();
        }
    }

    
    private static RemoteFileDesc newPushRFD(String name,String hash,byte [] guid) 
    	throws Exception {
    	
		IpPort ppi = 
			new IpPortImpl("1.2.3.10",2000);
		
		Set ppis = new TreeSet(IpPort.COMPARATOR);ppis.add(ppi);
		
    	PushEndpoint pe = new PushEndpoint(guid,ppis);
    	pe.updateProxies(true);
    	
    	return new RemoteFileDesc(newRFD(name,hash),pe);
	}
    
    /** Provides access to protected methods. */
    private static class TestManagedDownloader extends ManagedDownloader {
        public TestManagedDownloader(RemoteFileDesc[] files) {
            super(files, new IncompleteFileManager(), null);
        }

        public QueryRequest newRequery2() throws CantResumeException {
            return super.newRequery(2);
        }
        
        /**
         * allows overriding of the list of current downloaders
         */
        public void setDloaders(List l) {
        	try {
        		PrivilegedAccessor.setValue(this,"_activeWorkers",l);
        	}catch(Exception e) {
        		ErrorService.error(e);
        	}
        }
        
        public void setSHA1(URN sha1) throws Exception{
        	PrivilegedAccessor.setValue(this,"downloadSHA1",sha1);
        }
        
        public void setFM(FileManager fm) throws Exception{
        	PrivilegedAccessor.setValue(this,"fileManager",fm);
        }
        
        public void setIncompleteFile(File f) throws Exception {
        	PrivilegedAccessor.setValue(this,"incompleteFile",f);
        }
    }
    
    static class AltlocDownloaderStub extends HTTPDownloaderStub {
    	
    	boolean _stubFalts;
    	
    	RemoteFileDesc rfd;
    	public AltlocDownloaderStub(RemoteFileDesc fd){
    		super(fd,null);
    		rfd =fd;
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
    
    static class AltLocWorkerStub extends DownloadWorkerStub {
        AltlocDownloaderStub alt;
        public AltLocWorkerStub(ManagedDownloader manager, RemoteFileDesc rfd, AltlocDownloaderStub alt) {
            super(manager,rfd);
            this.alt = alt;
        }
        
        public HTTPDownloader getDownloader() {
            return alt;
        }
    }
}
