package com.limegroup.gnutella.downloader;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
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
import org.limewire.collection.Range;
import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.service.ErrorService;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManagerStub;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.ProviderHacks;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.dht.DHTEventListener;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.DHTManagerStub;
import com.limegroup.gnutella.dht.db.AltLocFinder;
import com.limegroup.gnutella.dht.db.AltLocSearchListener;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.stubs.FileDescStub;
import com.limegroup.gnutella.stubs.FileManagerStub;
import com.limegroup.gnutella.stubs.IncompleteFileDescStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.util.LimeWireUtils;

@SuppressWarnings("unchecked")
public class ManagedDownloaderTest extends com.limegroup.gnutella.util.LimeTestCase {
    
    private static final Log LOG = LogFactory.getLog(ManagedDownloaderTest.class);
    
    final static int PORT=6666;
    private DownloadManagerStub manager;
    private FileManager fileman;
    private ActivityCallback callback;
    private MessageRouter router;
    private static ConnectionManager connectionManager;

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
        connectionManager = new ConnectionManagerStub() {
            public boolean isConnected() {
                return true;
            }
        };
        
        PrivilegedAccessor.setValue(RouterService.class,"manager", connectionManager);
        
        assertTrue(RouterService.isConnected());
    }
    
    public void setUp() throws Exception {
        if (connectionManager == null) {
            globalSetUp();
        }
        ConnectionSettings.EVER_ACCEPTED_INCOMING.setValue(true);
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        manager = new DownloadManagerStub();
        fileman = new FileManagerStub();
        callback = new ActivityCallbackStub();
        router = new MessageRouterStub();
        manager.initialize(callback, router, fileman);
        ProviderHacks.getAltLocManager().purge();
        PrivilegedAccessor.setValue(RouterService.class,"callback",callback);
        PrivilegedAccessor.setValue(RouterService.class,"messageRouter",router);
        PrivilegedAccessor.setValue(RouterService.class,"downloadManager",manager);
        RequeryManager.NO_DELAY = false;
    }

    
    /**
     * tests if firewalled altlocs are added to the file descriptor
     * but not sent to uploaders.  (i.e. the informMesh method)
     */
    public void testFirewalledLocs() throws Exception {
    	LOG.info("testing firewalled locations");
        PrivilegedAccessor.setValue(ProviderHacks.getAcceptor(),
                "_acceptedIncoming",new Boolean(true));
        assertTrue(ProviderHacks.getNetworkManager().acceptedIncomingConnection());
        
    	//first make sure we are sharing an incomplete file
    	
    	URN partialURN = 
    		URN.createSHA1Urn("urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFE");
        
    	IncompleteFileDescStub partialDesc = 
    		new IncompleteFileDescStub("incomplete",partialURN,3);
    	
    	
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
    	
    	FileDesc test = ProviderHacks.getFileManager().getFileDescForUrn(partialURN);
    	
    	assertEquals(0,ProviderHacks.getAltLocManager().getNumLocs(test.getSHA1Urn()));
    	
    	//add one fake downloader to the downloader list
    	Endpoint e = new Endpoint("1.2.3.5",12345);
    	RemoteFileDesc other = new RemoteFileDesc(newRFD("incomplete"),e);
    	AltlocDownloaderStub fakeDownloader = new AltlocDownloaderStub(other);
    	
    	TestManagedDownloader md = new TestManagedDownloader(new RemoteFileDesc[]{rfd});
        AltLocWorkerStub worker = new AltLocWorkerStub(md,rfd,fakeDownloader);
        
        List l = new LinkedList();l.add(worker);
    	md.initialize(DownloadProviderHacks.createDownloadReferences(manager,newFMStub,callback));
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
    	test = ProviderHacks.getFileManager().getFileDescForUrn(partialURN);
    	assertEquals(1,ProviderHacks.getAltLocManager().getNumLocs(test.getSHA1Urn()));
    	
    	//now repeat the test, pretending the uploader wants push altlocs
    	fakeDownloader.setWantsFalts(true);
    	
    	//and see if it behaves correctly
    	md.informMesh(rfd,true);
    	
    	//make sure the downloader did get notified
    	assertFalse(fakeDownloader._addedFailed);
    	assertTrue(fakeDownloader._addedSuccessfull);
    	
    	//make sure the file was added to the file descriptor
    	test = ProviderHacks.getFileManager().getFileDescForUrn(partialURN);
    	assertEquals(1,ProviderHacks.getAltLocManager().getNumLocs(test.getSHA1Urn()));
    	
    	//rince and repeat, saying this was a bad altloc. 
    	//it should be sent to the other downloaders. 
    	fakeDownloader._addedSuccessfull=false;
    	
    	md.informMesh(rfd,false);
    	
    	//make sure the downloader did get notified
    	assertTrue(fakeDownloader._addedFailed);
    	assertFalse(fakeDownloader._addedSuccessfull);
    	
    	//make sure the altloc is demoted now
    	assertEquals(0,ProviderHacks.getAltLocManager().getNumLocs(test.getSHA1Urn()));
    	
    	PrivilegedAccessor.setValue(ProviderHacks.getAcceptor(),
                "_acceptedIncoming",new Boolean(false));
        assertFalse(ProviderHacks.getNetworkManager().acceptedIncomingConnection());
    	
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
        
        ProviderHacks.getUdpService().setReceiveSolicited(true);
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
        VerifyingFile vf=ProviderHacks.getVerifyingFileFactory().createVerifyingFile(1024);
        vf.addInterval(Range.createRange(0, amountDownloaded-1));  //inclusive
        ifm.addEntry(incompleteFile, vf);

        //Start downloader, make it sure requeries, etc.
        ManagedDownloader downloader = 
            new ManagedDownloader(new RemoteFileDesc[] { rfd }, ifm, null);
        downloader.initialize(DownloadProviderHacks.createDownloadReferences(manager, fileman, callback));
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
            downloader.initialize(DownloadProviderHacks.createDownloadReferences(manager, fileman, callback));
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

    /**
     * tests that if a result arrives and fails while we're
     * waiting for results from a gnet or dht query, we'll
     * fall back into the appropriate state.
     */
    public void testWaitingForResults() throws Exception {
        TestFile.length();
        RequeryManager.NO_DELAY = true;
        final DHTManager originalManager = ProviderHacks.getDHTManager();
        final AltLocFinder originalFinder = ProviderHacks.getAltLocFinder();
        final MyDHTManager myManager = new MyDHTManager();
        final MyAltLocFinder myFinder = new MyAltLocFinder();
        DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.setValue(true);
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        DHTSettings.TIME_BETWEEN_DHT_ALT_LOC_QUERIES.setValue(31*1000);
        Object NORMAL_CONNECT_TIME = PrivilegedAccessor.getValue(DownloadWorker.class,"NORMAL_CONNECT_TIME");
        Object PUSH_CONNECT_TIME = PrivilegedAccessor.getValue(DownloadWorker.class,"PUSH_CONNECT_TIME");
        long TIME_BETWEEN_REQUERIES = RequeryManager.TIME_BETWEEN_REQUERIES;
        try {
            setLazyReference("DHT_MANAGER_REFERENCE",(DHTManager)myManager);
            setLazyReference("ALT_LOC_FINDER_REFERENCE",(AltLocFinder)myFinder);
            assertSame(myManager,ProviderHacks.getDHTManager());
            assertSame(myFinder,ProviderHacks.getAltLocFinder());
            PrivilegedAccessor.setValue(DownloadWorker.class,"NORMAL_CONNECT_TIME",1000);
            PrivilegedAccessor.setValue(DownloadWorker.class,"PUSH_CONNECT_TIME",1000);
            RequeryManager.TIME_BETWEEN_REQUERIES = 10000;
            ManagedDownloader downloader = null;
            try {
                downloader=
                    new ManagedDownloader(
                            new RemoteFileDesc[] {fakeRFD()},
                            new IncompleteFileManager(), null);
                downloader.initialize(DownloadProviderHacks.createDownloadReferences(manager, 
                                      fileman,
                                      callback));
                LOG.debug("starting downloader");
                requestStart(downloader);
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
                
                // click resume, launch a dht query
                LOG.debug("clicking resume");
                myManager.on = true;
                downloader.resume();
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                waitForFullPump(1); // extra pump because finishDownload is not synced
                assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
                
                // while we're querying the dht, a search result arrives
                LOG.debug("adding source");
                downloader.addDownload(fakeRFD(), false);
                waitForStateToEnd(DownloadStatus.QUERYING_DHT, downloader);
                waitForFullPump(1); // an extra pump - somehow queued goes by too fast?
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                
                // after the result fails, we should fall back in querying state
                assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
                
                LOG.debug("dht query fails");
                myFinder.listener.handleAltLocSearchDone(false);
                assertSame(DownloadStatus.GAVE_UP,downloader.getState());
                waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
                assertSame(DownloadStatus.QUEUED,downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING,downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                 
                // now we should try a gnet query
                assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
                
                // another result arrives
                LOG.debug("adding another source");
                downloader.addDownload(fakeRFD(), false);
                waitForStateToEnd(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader);
                waitForFullPump(1); // an extra pump 
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                
                // should be waiting for sources again
                assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
            } catch(Throwable x) {
                fail(x);
            } finally {
                if (downloader != null)
                    downloader.stop();
            }
        } finally {
            setLazyReference("DHT_MANAGER_REFERENCE",originalManager);
            setLazyReference("ALT_LOC_FINDER_REFERENCE",originalFinder);
            assertSame(originalManager,ProviderHacks.getDHTManager());
            assertSame(originalFinder,ProviderHacks.getAltLocFinder());
            PrivilegedAccessor.setValue(DownloadWorker.class,"NORMAL_CONNECT_TIME",NORMAL_CONNECT_TIME);
            PrivilegedAccessor.setValue(DownloadWorker.class,"PUSH_CONNECT_TIME",PUSH_CONNECT_TIME);
            RequeryManager.TIME_BETWEEN_REQUERIES = TIME_BETWEEN_REQUERIES;
        }
    }
    
    /**
     * Tests the following scenario:
     * 1. a downloader starts and fails
     * 2. dht is off, so downloader stays in NMS state 
     * 3. dht comes on, downloader stays in NMS state
     * 5. user clicks FMS, downloader queries DHT
     * 6. DHT doesn't find anything, downloader goes to QUEUED->GNET
     * 7. gnet doesn't find anything, goes to AWS
     * 8. stays there until timeout expires
     * 8. after that happens it does a dht query.
     * 9. after the dht query expires it goes back to AWS.
     * 
     * Prior to a dht or gnet query it goes to QUEUED and very quickly to CONNECTED.
     */
    public void testBasicRequeryBehavior() throws Exception {
        TestFile.length();
        RequeryManager.NO_DELAY = true;
        final DHTManager originalManager = ProviderHacks.getDHTManager();
        final AltLocFinder originalFinder = ProviderHacks.getAltLocFinder();
        final MyDHTManager myManager = new MyDHTManager();
        final MyAltLocFinder myFinder = new MyAltLocFinder();
        DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.setValue(true);
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        DHTSettings.TIME_BETWEEN_DHT_ALT_LOC_QUERIES.setValue(31*1000);
        Object NORMAL_CONNECT_TIME = PrivilegedAccessor.getValue(DownloadWorker.class,"NORMAL_CONNECT_TIME");
        Object PUSH_CONNECT_TIME = PrivilegedAccessor.getValue(DownloadWorker.class,"PUSH_CONNECT_TIME");
        long TIME_BETWEEN_REQUERIES = RequeryManager.TIME_BETWEEN_REQUERIES;
        try {
            setLazyReference("DHT_MANAGER_REFERENCE",(DHTManager)myManager);
            setLazyReference("ALT_LOC_FINDER_REFERENCE",(AltLocFinder)myFinder);
            assertSame(myManager,ProviderHacks.getDHTManager());
            assertSame(myFinder,ProviderHacks.getAltLocFinder());
            PrivilegedAccessor.setValue(DownloadWorker.class,"NORMAL_CONNECT_TIME",1000);
            PrivilegedAccessor.setValue(DownloadWorker.class,"PUSH_CONNECT_TIME",1000);
            RequeryManager.TIME_BETWEEN_REQUERIES = 10000;

            ManagedDownloader downloader = null;
            try {
                LOG.debug("starting downloader");
                downloader=
                    new ManagedDownloader(
                            new RemoteFileDesc[] {fakeRFD()},
                            new IncompleteFileManager(), null);
                downloader.initialize(DownloadProviderHacks.createDownloadReferences(manager, 
                                      fileman,
                                      callback));
                LOG.debug("starting downloader");
                requestStart(downloader);
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForFullPump(1);
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                waitForFullPump(2); // 2 pumps should be enough
                assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
                
                LOG.debug("waiting for a few handleInactivity() calls");
                waitForFullPump(3);
                // should still be waiting for user
                assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
                
                // now turn dht on 
                myManager.on = true;
                
                // a few pumps go by but we're still waiting for user
                waitForFullPump(1);
                assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
                waitForFullPump(1);
                assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
                
                // user hits FMS
                LOG.debug("hit resume");
                downloader.resume();
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForFullPump(2); 
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
                
                // few pumps, still querying dht
                waitForFullPump(3);
                assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
                
                // now tell them the dht query failed
                LOG.debug("dht query fails");
                assertNotNull(myFinder.listener);
                myFinder.listener.handleAltLocSearchDone(false);
                assertSame(DownloadStatus.GAVE_UP,downloader.getState());
                waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
                assertSame(DownloadStatus.QUEUED,downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING,downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                 
                // now we should try a gnet query
                assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
                
                // wait for gnet to return fail
                waitForStateToEnd(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader);
                waitForFullPump(1); 
                // should have given up
                assertSame(DownloadStatus.GAVE_UP, downloader.getState());
                
                // stays given up for a while
                waitForFullPump(1);
                assertSame(DownloadStatus.GAVE_UP, downloader.getState());
                waitForFullPump(1);
                assertSame(DownloadStatus.GAVE_UP, downloader.getState());
                waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
                
                // eventually we can query the dht again
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
                
                // lets say this query times out instead of receiving callback
                waitForStateToEnd(DownloadStatus.QUERYING_DHT, downloader);
                assertSame(DownloadStatus.GAVE_UP, downloader.getState());
                
                // back to awaiting sources
                waitForFullPump(2);
                assertSame(DownloadStatus.GAVE_UP, downloader.getState());
                waitForFullPump(2);
                assertSame(DownloadStatus.GAVE_UP, downloader.getState());
                
                // we stay there for good.
                
            } catch(Throwable x) {
                fail(x);
            } finally {
                if (downloader != null)
                    downloader.stop();
            }
        } finally {
            setLazyReference("DHT_MANAGER_REFERENCE",originalManager);
            setLazyReference("ALT_LOC_FINDER_REFERENCE",originalFinder);
            assertSame(originalManager,ProviderHacks.getDHTManager());
            assertSame(originalFinder,ProviderHacks.getAltLocFinder());
            PrivilegedAccessor.setValue(DownloadWorker.class,"NORMAL_CONNECT_TIME",NORMAL_CONNECT_TIME);
            PrivilegedAccessor.setValue(DownloadWorker.class,"PUSH_CONNECT_TIME",PUSH_CONNECT_TIME);
            RequeryManager.TIME_BETWEEN_REQUERIES = TIME_BETWEEN_REQUERIES;
        }

    }
    /**
     * Tests the following scenario:
     * 1. a downloader starts and fails
     * 2. dht is off, so downloader stays in NMS state 
     * 3. dht comes on, downloader goes looking for sources
     * 4. it doesn't find anything, goes back to NMS
     * 5. user clicks FMS, downloader goes to WFS
     * 6. FMS doesn't find anything, downloader goes to AWS
     * 7. it stays there until the timeout for a dht query elapses
     * 8. after that happens it does a dht query.
     * 
     * Prior to a dht or gnet query it goes to QUEUED and very quickly to CONNECTED.
     */
    public void testProRequeryBehavior() throws Exception {
        TestFile.length();
        RequeryManager.NO_DELAY = true;
        final DHTManager originalManager = ProviderHacks.getDHTManager();
        final AltLocFinder originalFinder = ProviderHacks.getAltLocFinder();
        final MyDHTManager myManager = new MyDHTManager();
        final MyAltLocFinder myFinder = new MyAltLocFinder();
        DHTSettings.ENABLE_DHT_ALT_LOC_QUERIES.setValue(true);
        DHTSettings.MAX_DHT_ALT_LOC_QUERY_ATTEMPTS.setValue(2);
        DHTSettings.TIME_BETWEEN_DHT_ALT_LOC_QUERIES.setValue(31*1000);
        PrivilegedAccessor.setValue(LimeWireUtils.class,"_isPro",Boolean.TRUE);
        assertTrue(LimeWireUtils.isPro());
        Object NORMAL_CONNECT_TIME = PrivilegedAccessor.getValue(DownloadWorker.class,"NORMAL_CONNECT_TIME");
        Object PUSH_CONNECT_TIME = PrivilegedAccessor.getValue(DownloadWorker.class,"PUSH_CONNECT_TIME");
        long TIME_BETWEEN_REQUERIES = RequeryManager.TIME_BETWEEN_REQUERIES;
        try {
            setLazyReference("DHT_MANAGER_REFERENCE",(DHTManager)myManager);
            setLazyReference("ALT_LOC_FINDER_REFERENCE",(AltLocFinder)myFinder);
            assertSame(myManager,ProviderHacks.getDHTManager());
            assertSame(myFinder,ProviderHacks.getAltLocFinder());
            PrivilegedAccessor.setValue(DownloadWorker.class,"NORMAL_CONNECT_TIME",1000);
            PrivilegedAccessor.setValue(DownloadWorker.class,"PUSH_CONNECT_TIME",1000);
            RequeryManager.TIME_BETWEEN_REQUERIES = 10000;

            ManagedDownloader downloader = null;
            try {
                LOG.debug("starting downloader");
                downloader=
                    new ManagedDownloader(
                            new RemoteFileDesc[] {fakeRFD()},
                            new IncompleteFileManager(), null);
                downloader.initialize(DownloadProviderHacks.createDownloadReferences(manager, 
                                      fileman,
                                      callback));
                LOG.debug("starting downloader");
                requestStart(downloader);
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
                
                LOG.debug("waiting for a few handleInactivity() calls");
                waitForFullPump(2);
                // should still be waiting for user
                assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
                
                // now turn dht on 
                LOG.debug("turning dht on");
                myManager.on = true;
                
                // in another pump or two we should be looking for sources
                waitForFullPump(1);
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
                
                // now tell them the dht query failed
                LOG.debug("dht query fails");
                assertNotNull(myFinder.listener);
                myFinder.listener.handleAltLocSearchDone(false);
                assertSame(DownloadStatus.GAVE_UP, downloader.getState());
                waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
                                
                waitForFullPump(2); // few more pumps
                // should still be waiting for user
                assertSame(DownloadStatus.WAITING_FOR_USER, downloader.getState());
                
                // hit resume()
                LOG.debug("hitting resume");
                downloader.resume();
                
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader); 
                assertSame(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader.getState());
                
                // if we find nothing, we give up
                waitForStateToEnd(DownloadStatus.WAITING_FOR_GNET_RESULTS, downloader);
                assertSame(DownloadStatus.GAVE_UP, downloader.getState());
                
                // we stay given up for a while but eventually launch another DHT_QUERY
                waitForFullPump(2); 
                assertSame(DownloadStatus.GAVE_UP, downloader.getState());
                waitForStateToEnd(DownloadStatus.GAVE_UP, downloader);
                
                // we should be making another dht attempt now
                assertSame(DownloadStatus.QUEUED, downloader.getState());
                waitForStateToEnd(DownloadStatus.QUEUED, downloader);
                assertSame(DownloadStatus.CONNECTING, downloader.getState());
                waitForStateToEnd(DownloadStatus.CONNECTING, downloader);
                assertSame(DownloadStatus.QUERYING_DHT, downloader.getState());
                
            } catch(Throwable x) {
                fail(x);
            } finally {
                if (downloader != null)
                    downloader.stop();
            }
        } finally {
            setLazyReference("DHT_MANAGER_REFERENCE",originalManager);
            setLazyReference("ALT_LOC_FINDER_REFERENCE",originalFinder);
            assertSame(originalManager,ProviderHacks.getDHTManager());
            assertSame(originalFinder,ProviderHacks.getAltLocFinder());
            PrivilegedAccessor.setValue(LimeWireUtils.class,"_isPro",Boolean.FALSE);
            assertFalse(LimeWireUtils.isPro());
            PrivilegedAccessor.setValue(DownloadWorker.class,"NORMAL_CONNECT_TIME",NORMAL_CONNECT_TIME);
            PrivilegedAccessor.setValue(DownloadWorker.class,"PUSH_CONNECT_TIME",PUSH_CONNECT_TIME);
            RequeryManager.TIME_BETWEEN_REQUERIES = TIME_BETWEEN_REQUERIES;
        }
    }
    
    /**
     * @return the number of pumps it took to get out of the current state.
     * Note: I'm not sure if this is a good additional check.  It may make the test
     * much stricter than necessary
     */
    private int waitForStateToEnd(DownloadStatus status, Downloader downloader) throws Exception {
        LOG.debug("waiting for "+status+" to end");
        int pumps = 0;
        while(downloader.getState() == status) {
            waitForFullPump(1);
            pumps++;
            LOG.debug("pump "+pumps);
        }
        LOG.debug("out of "+status+" now "+downloader.getState());
        return pumps;
    }
    
    private void waitForFullPump(int pumps) throws Exception {
        for (int i = 0; i < pumps; i++) {
            synchronized(manager.pump) {
                manager.pump.wait();
            }
        }
    }
    
    private <T>void setLazyReference(String name, final T value) throws Exception{
        PrivilegedAccessor.setValue(RouterService.class,name,
                new AbstractLazySingletonProvider<T>() {
            public T createObject() {
                return value;
            }
        });
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
            uploader=new TestUploader("ManagedDownloaderTest", PORT, false);
            uploader.stopAfter(500);
            uploader.setSendThexTreeHeader(false);
            uploader.setSendThexTree(false);
            downloader=
				new ManagedDownloader(
						new RemoteFileDesc[] {newRFD("another testfile.txt",FileDescStub.DEFAULT_URN.toString())},
                        new IncompleteFileManager(), null);
            downloader.initialize(DownloadProviderHacks.createDownloadReferences(manager, 
                                  fileman,
                                  callback));
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
            new ManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()),
                    saveDir, "does not matter", false);
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
        // we force filename normalization
        try {
            new ManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()),
                    null, "./", false);
            new ManagedDownloader(rfds,
                    new IncompleteFileManager(), new GUID(GUID.makeGuid()),
                    null, "../myfile.txt", false);
        }
        catch (SaveLocationException sle) {
            fail("Should not have thrown" + sle);
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
                                  false, false,"",null, -1, false);
    }
    
    private static RemoteFileDesc fakeRFD() {
        return new RemoteFileDesc("0.0.0.1", (int)(Math.random() * Short.MAX_VALUE +1000), 13l,
                "badger", 1024,
                new byte[16], 56, false, 4, true, null, new HashSet(),
                false, false,"",null, -1, false);
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
    
    private static class MyDHTManager extends DHTManagerStub {

        private volatile DHTEventListener listener;
        private volatile boolean on;
        
        public void addEventListener(DHTEventListener listener) {
            this.listener = listener;
        }

        @Override
        public boolean isMemberOfDHT() {
            return on;
        }

        @Override
        public void removeEventListener(DHTEventListener listener) {
            if (this.listener == listener)
                this.listener = null;
        }
    }
    
    private static class MyAltLocFinder extends AltLocFinder {
        private volatile AltLocSearchListener listener;
        
        volatile boolean cancelled;
        public MyAltLocFinder() {
            super(null, ProviderHacks.getAlternateLocationFactory());
        }
        
        
        @Override
        public Shutdownable findAltLocs(URN urn, AltLocSearchListener listener) {
            this.listener = listener;
            return new Shutdownable() {
                public void shutdown() {
                    cancelled = true;
                }
            };
        }

        @Override
        public boolean findPushAltLocs(GUID guid, URN urn) {
            return true;
        }
        
    }
}
