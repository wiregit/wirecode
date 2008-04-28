package com.limegroup.gnutella;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.connection.BlockingConnection;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;
import com.limegroup.gnutella.downloader.TestFile;
import com.limegroup.gnutella.downloader.TestUploader;
import com.limegroup.gnutella.library.SharingUtils;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.QueryRequestFactory;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.NetworkSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;

/**
 * Tests that What is new support is fully functional.  We use a leaf here - we
 * assume that an Ultrapeer will be equally functional.
 * 
 */
@SuppressWarnings( { "unchecked", "cast" } )
public class ServerSideWhatIsNewTest 
    extends ClientSideTestCase {
    private static final int PORT=6669;
    private static final int TIMEOUT=1000;

    private static File berkeley = null;
    private static File susheel = null;
    private static File tempFile1 = null;
    private static File tempFile2 = null;
    private MessagesSupportedVendorMessage messagesSupportedVendorMessage;
    private CapabilitiesVMFactory capabilitiesVMFactory;
    private FileManager fileManager;
    private CreationTimeCache creationTimeCache;
    private QueryRequestFactory queryRequestFactory;
    private Injector injector;
    private DownloadServices downloadServices;
    
    public ServerSideWhatIsNewTest(String name) {
        super(name);
    }
    
    @Override
    public int getNumberOfPeers() {
        return 1;
    }

    public static Test suite() {
        return buildTestSuite(ServerSideWhatIsNewTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    @Override
    public void setSettings() throws Exception {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client in leaf mode to listen on port
        //6669, with no slots and no connections.  But you need to re-enable
        //the interactive prompts below.
        NetworkSettings.PORT.setValue(PORT);
        ConnectionSettings.DO_NOT_BOOTSTRAP.setValue(true);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        //  Required so that the "swarmDownloadCatchesEarlyCreationTest" actually works  =)
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.T3_SPEED_INT);
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;exe;bin;dmg");
        LimeTestUtils.setSharedDirectories( new File[] { _sharedDir, _savedDir } );
        // get the resource file for com/limegroup/gnutella
        berkeley = 
            TestUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        susheel = 
            TestUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        // now move them to the share dir
        File berkeleyDest = new File(_sharedDir, "berkeley.txt");
        File susheelDest = new File(_sharedDir, "susheel.txt");
        
        FileUtils.copy(berkeley, berkeleyDest);
        // make sure their creation times are a little different.
        int sleeps = 0;
        do {
            Thread.sleep(10);
            FileUtils.copy(susheel, susheelDest);
        } while (susheelDest.lastModified() == berkeleyDest.lastModified() && ++sleeps < 100);
        if (sleeps >= 10 && susheelDest.lastModified() == berkeleyDest.lastModified())
            fail("couldn't create a file with newer timestamp");
        
        berkeley = berkeleyDest;
        susheel = susheelDest;
        // make sure results get through
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }        
    
    @Override
    public void setUp() throws Exception  {
        injector = LimeTestUtils.createInjector();
        super.setUp(injector);
        
        messagesSupportedVendorMessage = injector.getInstance(MessagesSupportedVendorMessage.class);
        capabilitiesVMFactory = injector.getInstance(CapabilitiesVMFactory.class);
        fileManager = injector.getInstance(FileManager.class);
        creationTimeCache = injector.getInstance(CreationTimeCache.class);
        queryRequestFactory = injector.getInstance(QueryRequestFactory.class);
        downloadServices = injector.getInstance(DownloadServices.class);

        fileManager.loadSettingsAndWait(500);
        
        exchangeCapabilitiesMessage();
    }
    
    @Override
    public boolean shouldRespondToPing() {
        return false;
    }

    public void exchangeCapabilitiesMessage() throws Exception {
        //testUP = connect(rs, 6355, true);
        BlockingConnection connection = testUP[0];

        // send a MessagesSupportedMessage and capabilities VM
        connection.send(messagesSupportedVendorMessage);
        connection.send(capabilitiesVMFactory.getCapabilitiesVM());
        connection.flush();

        Thread.sleep(100);
        
        // we expect to get a CVM back
        Message m = null;
        do {
            m = connection.receive(TIMEOUT);
        } while (!(m instanceof CapabilitiesVM)) ;
        assertTrue(((CapabilitiesVM)m).supportsWhatIsNew());

        // client side seems to follow the setup process A-OK
    }

    // test that the CreationTimeCache is as expected
    public void testCreationTimeCacheInitialState() throws Exception {
        // wait for fm to finish
        int i = 0;
        for (; (i < 15) && (fileManager.getNumFiles() < 2); i++)
            Thread.sleep(1000);
        if (i == 15) assertTrue(false);

        // we should be sharing two files - two text files.
        assertEquals(2, fileManager.getNumFiles());

        FileManager fm = fileManager;
        URN berkeleyURN = fm.getURNForFile(berkeley);
        URN susheelURN = fm.getURNForFile(susheel);

        Map urnToLong =  creationTimeCache.getUrnToTime();
        assertEquals(2, urnToLong.size());
        assertNotNull(""+urnToLong, urnToLong.get(berkeleyURN));
        assertNotNull(""+urnToLong, urnToLong.get(susheelURN));

        Map longToUrns = creationTimeCache.getTimeToUrn();
        if (longToUrns.size() == 1) {
            Iterator iter = longToUrns.entrySet().iterator();
            Set urnSet = (Set)((Map.Entry)iter.next()).getValue();
            assertTrue(urnSet.contains(berkeleyURN));
            assertTrue(urnSet.contains(susheelURN));
            assertEquals(2, urnSet.size());
        }
        else if (longToUrns.size() == 2) {
            Iterator iter = longToUrns.entrySet().iterator();
            Set urnSet = (Set)((Map.Entry)iter.next()).getValue();
            assertTrue(
                    ( urnSet.contains(berkeleyURN) && 
                            !urnSet.contains(susheelURN) )
                            ||
                            ( !urnSet.contains(berkeleyURN) && 
                                    urnSet.contains(susheelURN) )
            );
            assertEquals(1, urnSet.size());
            urnSet = (Set)((Map.Entry)iter.next()).getValue();
            assertTrue(
                    ( urnSet.contains(berkeleyURN) && 
                            !urnSet.contains(susheelURN) )
                            ||
                            ( !urnSet.contains(berkeleyURN) && 
                                    urnSet.contains(susheelURN) )
            );
            assertEquals(1, urnSet.size());
        }
        else assertTrue("Bad Cache!", false);
    }

    // make sure that a what is new query is answered correctly
    public void testWhatIsNewQueryBasic() throws Exception {
        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        whatIsNewQuery.hop();
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(2, reply.getResultCount());
        Iterator iter = reply.getResults();
        Response currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        assertFalse(iter.hasNext());
    }

    // make sure that a what is new query meta query is answered correctly
    public void testWhatIsNewQueryMeta() throws Exception {

        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        {
        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW, false, 0 | QueryRequest.AUDIO_MASK);
        whatIsNewQuery.hop();
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(2000);

        QueryReply reply = 
            (QueryReply) BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNull(reply);
        }

        {
        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW, false, 0 | QueryRequest.DOC_MASK);
        whatIsNewQuery.hop();
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(2, reply.getResultCount());
        Iterator iter = reply.getResults();
        Response currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        currResp = (Response) iter.next();
        assertTrue(currResp.getName().equals("berkeley.txt") ||
                   currResp.getName().equals("susheel.txt"));
        assertFalse(iter.hasNext());
        }
    }


    // test that the creation time cache handles the additional sharing of files
    // fine
    public void testAddSharedFiles() throws Exception {
        FileManager fm = fileManager;
        URN berkeleyURN = fm.getURNForFile(berkeley);
        URN susheelURN = fm.getURNForFile(susheel);

        tempFile1 = new File("tempFile1.txt");
        tempFile2 = new File("tempFile2.txt");
        tempFile1.deleteOnExit(); tempFile2.deleteOnExit();

        // we start with one or two timestamps
        Map longToUrns = creationTimeCache.getTimeToUrn();
        int startTimeStamps = longToUrns.size();
        assertTrue(""+longToUrns, startTimeStamps == 1 || startTimeStamps == 2);
        
        Map urnToLong = creationTimeCache.getUrnToTime();
        long previousTime = (Long)urnToLong.get(berkeleyURN);
        previousTime = Math.max(previousTime, (Long)urnToLong.get(susheelURN));
        FileWriter writer = null;
        int attempts = 0;
        do {
            writer = new FileWriter(tempFile1);
            writer.write(tempFile1.getName(), 0, tempFile1.getName().length());
            writer.flush();
            writer.close();
            Thread.sleep(100);
        } while(tempFile1.lastModified() == previousTime && attempts++ < 10);
        
        assertNotEquals("couldn't set up test",tempFile1.lastModified(), previousTime);
        
        attempts = 0;
        do {
            writer = new FileWriter(tempFile2);
            writer.write(tempFile2.getName(), 0, tempFile2.getName().length());
            writer.flush();
            writer.close();
            Thread.sleep(100);
        } while(tempFile2.lastModified() == previousTime && attempts++ < 10);
        assertNotEquals("couldn't set up test",tempFile2.lastModified(), previousTime);
        
        // now move them to the share dir
        FileUtils.copy(tempFile1, new File(_sharedDir, "tempFile1.txt"));
        FileUtils.copy(tempFile2, new File(_sharedDir, "tempFile2.txt"));
        tempFile1 = new File(_sharedDir, "tempFile1.txt");
        tempFile2 = new File(_sharedDir, "tempFile2.txt");
        assertTrue(tempFile1.exists());
        assertTrue(tempFile2.exists());

        fileManager.loadSettingsAndWait(1000);
        assertEquals("Files were not loaded by filemanager", 4, fileManager.getNumFiles());

        URN tempFile1URN = fm.getURNForFile(tempFile1);
        URN tempFile2URN = fm.getURNForFile(tempFile2);

        assertEquals(4, urnToLong.size());
        assertNotNull(""+urnToLong, urnToLong.get(berkeleyURN));
        assertNotNull(""+urnToLong, urnToLong.get(susheelURN));
        assertNotNull(""+urnToLong, urnToLong.get(tempFile1URN));
        assertNotNull(""+urnToLong, urnToLong.get(tempFile2URN));
        
        // we end with 2, 3 or 4 timestamps, depending on fs performance
        assertGreaterThan(startTimeStamps, longToUrns.size());
        assertLessThan(5, longToUrns.size());
    }

    // test that after the sharing of additional files, the what is new query
    // results in something else
    public void testWhatIsNewQueryNewFiles() throws Exception {
        testAddSharedFiles();
        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(3, reply.getResultCount());
        boolean gotTempFile1 = false, gotTempFile2 = false;
        
        Iterator iter = reply.getResults();
        while (iter.hasNext()) {
            Response currResp = (Response) iter.next();
            if (currResp.getName().equals(tempFile1.getName()))
                gotTempFile1 = true;
            if (currResp.getName().equals(tempFile2.getName()))
                gotTempFile2 = true;
        }
        assertTrue("file 1? " + gotTempFile1 + ", file 2? " +
                   gotTempFile2, gotTempFile1 && gotTempFile2);
    }

    
    // test that the fileChanged method of FM does the right thing.  this isn't
    // a total black box test, but hacking up the changing of ID3 data isn't
    // worth the cost.  this test should be good enough....
    public void testFileChanged() throws Exception {
        testAddSharedFiles();
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;
        URN tempFile1URN = fm.getURNForFile(tempFile1);
        Long cTime = ctCache.getCreationTime(tempFile1URN);

        FileWriter writer = null;
        {
            long modified = tempFile1.lastModified();
            while (modified == System.currentTimeMillis())
                Thread.sleep(10);
            // the filesystem is not too responsive
            while(tempFile1.lastModified() == modified) {
                writer = new FileWriter(tempFile1);
                writer.write(berkeley.getName(), 0, berkeley.getName().length());
                writer.flush();
                writer.close();
                Thread.sleep(10);
            }
        }

        final FileDesc beforeChanged = fm.getFileDescForFile(tempFile1);
        assertNotNull(beforeChanged);
        
        final CountDownLatch fileChangedLatch = new CountDownLatch(1);
        fm.addFileEventListener(new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                if (evt.getType() != FileManagerEvent.Type.CHANGE_FILE)
                    return;
                if (evt.getFileDescs() == null || evt.getFileDescs().length != 2)
                    return;
                if (evt.getFileDescs()[0] == beforeChanged)
                    fileChangedLatch.countDown();
            }
        });
        fm.fileChanged(tempFile1);
        assertTrue(fileChangedLatch.await(3, TimeUnit.SECONDS));
        FileDesc afterChanged = fm.getFileDescForFile(tempFile1);
        assertNotNull(afterChanged);
        assertNotSame(beforeChanged, afterChanged);
        
        assertNotNull(fm.getURNForFile(tempFile1));
        assertNotEquals(tempFile1URN, fm.getURNForFile(tempFile1));
        assertEquals(ctCache.getCreationTime(fm.getURNForFile(tempFile1)),
                     cTime);

        // now just send another What Is New query and make sure everything
        // is kosher - probably overkill but whatever....
        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(3, reply.getResultCount());
        boolean gotTempFile1 = false, gotTempFile2 = false;
        
        Iterator iter = reply.getResults();
        while (iter.hasNext()) {
            Response currResp = (Response) iter.next();
            if (currResp.getName().equals(tempFile1.getName()))
                gotTempFile1 = true;
            if (currResp.getName().equals(tempFile2.getName()))
                gotTempFile2 = true;
        }
        assertTrue("file 1? " + gotTempFile1 + ", file 2? " +
                   gotTempFile2, gotTempFile1 && gotTempFile2);
    }


    // test that the fileChanged method of FM does the right thing when you 
    // change the file to a existing URN.
    public void testFileChangedToExistingURN() throws Exception {
        testAddSharedFiles();
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;
        URN tempFile1URN = fm.getURNForFile(tempFile1);
//        URN tempFile2URN = fm.getURNForFile(tempFile2);
        // we are changing tempFile1 to become tempFile2 - but since we
        // call fileChanged(), then the common URN should get tempFile1's
        // cTime
        Long cTime = ctCache.getCreationTime(tempFile1URN);

        FileWriter writer = null;
        {
            long modified = tempFile1.lastModified();
            while (modified == System.currentTimeMillis())
                Thread.sleep(10);
            // the filesystem is not too responsive
            while(tempFile1.lastModified() == modified) {
                Thread.sleep(10);
                writer = new FileWriter(tempFile1);
                writer.write(tempFile2.getName(), 0, tempFile2.getName().length());
                writer.flush();
                writer.close();
            }
        }

        FileDesc beforeChanged = fm.getFileDescForFile(tempFile1);
        assertNotNull(beforeChanged);
        
        final CountDownLatch latch = new CountDownLatch(1);
        fm.addFileEventListener(new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                assertEquals(FileManagerEvent.Type.CHANGE_FILE, evt.getType());
                latch.countDown();
            }
        });
        fm.fileChanged(tempFile1);
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        FileDesc afterChanged = fm.getFileDescForFile(tempFile1);
        assertNotNull(afterChanged);
        assertNotSame(beforeChanged, afterChanged);
        assertNotNull(fm.getURNForFile(tempFile1));
        assertNotEquals(tempFile1URN, fm.getURNForFile(tempFile1));
        assertEquals(fm.getURNForFile(tempFile1), fm.getURNForFile(tempFile2));
        assertEquals(ctCache.getCreationTime(fm.getURNForFile(tempFile1)),
                     cTime);

        // now just send another What Is New query and make sure everything
        // is kosher - probbably overkill but whatever....
        BlockingConnection connection = testUP[0];
        BlockingConnectionUtils.drain(connection);

        QueryRequest whatIsNewQuery = 
            queryRequestFactory.createQueryRequest(GUID.makeGuid(), (byte)2,
                QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, null, false, Network.UNKNOWN, false,
                FeatureSearchData.WHAT_IS_NEW);
        connection.send(whatIsNewQuery);
        connection.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) BlockingConnectionUtils.getFirstInstanceOfMessageType(connection,
                                                       QueryReply.class);
        assertNotNull(reply);
        assertEquals(3, reply.getResultCount());
        boolean gotTempFile1 = false, gotTempFile2 = false;
        
        Iterator iter = reply.getResults();
        while (iter.hasNext()) {
            Response currResp = (Response) iter.next();
            if (currResp.getName().equals(tempFile1.getName()))
                gotTempFile1 = true;
            if (currResp.getName().equals(tempFile2.getName()))
                gotTempFile2 = true;
        }
        assertTrue("file 1? " + gotTempFile1 + ", file 2? " +
                   gotTempFile2, !gotTempFile1 && gotTempFile2);
    }

    // test that the FileManager.removeFileIfShared method works    
    public void testRemoveSharedFile() throws Exception {
        testAddSharedFiles();
        FileManager fm = fileManager;
        assertEquals(4,fm.getNumFiles());
        
        
        // 4 shared files
        assertEquals(4,fm.getNumFiles());
        
        // 4 different urns 
        {
            Map urnToLong = creationTimeCache.getUrnToTime();
            assertEquals(urnToLong.toString(), 4, urnToLong.size());
        }
        
        Map longToUrns = creationTimeCache.getTimeToUrn();
        int startTimeStamps = longToUrns.size();
        // 2,3 or 4 different creation times 
        {
            assertGreaterThan(1,longToUrns.size());
            assertLessThan(5, longToUrns.size());
        }
        
        // tempFile1 and 2 have the same URN
        fm.removeFileIfSharedOrStore(tempFile1);
        {
            Map urnToLong = creationTimeCache.getUrnToTime();  
            assertEquals(3, urnToLong.size());
        }
        // they may or may not have the same timestamp
        assertLessThanOrEquals(startTimeStamps, longToUrns.size());
        
        // tempFile2 should result in a removal of an URN
        // as well as a timestamp
        fm.removeFileIfSharedOrStore(tempFile2);

        {
            Map urnToLong = creationTimeCache.getUrnToTime();
            assertEquals(2, urnToLong.size());
        }
        assertLessThan(startTimeStamps, longToUrns.size());
    }


    // manually delete a file, make sure it isn't shared and that the CTC has
    // the correct sizes, etc...
    public void testManualFileDeleteLoadSettings() throws Exception {
        FileManager fm = fileManager;

        tempFile1.delete(); tempFile1 = null;
        tempFile2.delete(); tempFile2 = null;
        berkeley.delete(); berkeley = null;

        fm.loadSettings();
        Thread.sleep(2000);
        assertEquals("num shared files", 1, fileManager.getNumFiles());

        URN susheelURN = fm.getURNForFile(susheel);
        {
            Map urnToLong = creationTimeCache.getUrnToTime(); 
            assertEquals(""+urnToLong, 1, urnToLong.size());
            assertNotNull(""+urnToLong, urnToLong.get(susheelURN));
        }
        {
            Map longToUrns = creationTimeCache.getTimeToUrn();
            assertEquals(""+longToUrns, 1, longToUrns.size());
        }
    }
    

    // download a file and make sure the creation time given back is stored...
    public void testDownloadCapturesCreationTime() throws Exception {
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;
        for (FileDesc fd : fileManager.getAllSharedFileDescriptors()) {
            fileManager.removeFileIfSharedOrStore(fd.getFile());
            fd.getFile().delete();
        }
        
        final int UPLOADER_PORT = 10000;
        byte[] guid = GUID.makeGuid();
        TestUploader uploader = injector.getInstance(TestUploader.class);
        uploader.start("whatever.txt", UPLOADER_PORT, false);
        Long cTime = new Long(2);
        uploader.setCreationTime(cTime);
        Set urns = new HashSet();
        urns.add(TestFile.hash());
        RemoteFileDesc rfd = injector.getInstance(RemoteFileDescFactory.class)
                .createRemoteFileDesc("127.0.0.1", UPLOADER_PORT, 1, "whatever.txt", TestFile.length(), guid, 1, false, 3,
                        false, null, urns, false, false, "LIME", new HashSet(), -1, false);
        
        int sharedBefore = fileManager.getNumFiles();
        final CountDownLatch downloadedLatch = new CountDownLatch(2); //1 incomplete, 2 complete
        fileManager.addFileEventListener(new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                if (evt.isAddEvent())
                    downloadedLatch.countDown();
            }
        });
        downloadServices.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        assertTrue("download never completed",downloadedLatch.await(320,TimeUnit.SECONDS));
        
        assertEquals( sharedBefore + 1, fileManager.getNumFiles());

        File newFile = new File(_savedDir, "whatever.txt");
        assertTrue(newFile.getAbsolutePath()+" didn't exist", newFile.exists());
        URN newFileURN = fm.getURNForFile(newFile);
        assertEquals(TestFile.hash(), newFileURN);
        assertEquals(newFileURN.toString(), cTime, ctCache.getCreationTime(newFileURN));

        Map urnToLong = ctCache.getUrnToTime();
        assertEquals(""+urnToLong, sharedBefore + 1, urnToLong.size());
        Map longToUrns = ctCache.getTimeToUrn();
        assertEquals(""+longToUrns+"  vs "+urnToLong, sharedBefore + 1, longToUrns.size());

    }


    // download a file and make sure the creation time given back is stored...
    public void testSwarmDownloadCapturesOlderCreationTime() throws Exception {
        
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;
        
        for (FileDesc fd : fileManager.getAllSharedFileDescriptors()) {
            fileManager.removeFileIfSharedOrStore(fd.getFile());
            fd.getFile().delete();
        }

        final int UPLOADER_PORT = 20000;
        byte[] guid = GUID.makeGuid();
        TestUploader uploader[] = new TestUploader[4];
        Long cTime[] = new Long[uploader.length];
        RemoteFileDesc rfds[] = new RemoteFileDesc[uploader.length];
        for (int i = 0; i < uploader.length; i++) {
            uploader[i] = injector.getInstance(TestUploader.class);
            uploader[i].start("anita.txt", UPLOADER_PORT+i, false);
            uploader[i].setRate(50);
            cTime[i] = new Long(5+i);
            uploader[i].setCreationTime(cTime[i]);
            Set urns = new HashSet();
            urns.add(TestFile.hash());
            rfds[i] = injector.getInstance(RemoteFileDescFactory.class).createRemoteFileDesc("127.0.0.1", UPLOADER_PORT+i, 1, "anita.txt",
                    TestFile.length(), guid, 1, false, 3, false, null, urns, false, false, "LIME",
                    new HashSet(), -1, false);
        }

        // first we get a notification for sharing the incomplete file desc
        final Semaphore downloadState = new Semaphore(0); 
        fileManager.addFileEventListener(new FileEventListener() {
            public void handleFileEvent(FileManagerEvent evt) {
                if (evt.isAddEvent())
                    downloadState.release();
            }
        });
        downloadServices.download(rfds, false, new GUID(guid));
        assertTrue("download never started",downloadState.tryAcquire(120,TimeUnit.SECONDS));
        Thread.sleep(1000);
        // make sure that the partial file has a creation time - by now one of
        // the downloaders must have got X-Create-Time
        assertNotNull(ctCache.getCreationTime(TestFile.hash()));

        // second notification is for sharing the entire file
        assertTrue("download never finished",downloadState.tryAcquire(120,TimeUnit.SECONDS));
        
        assertEquals(1, fileManager.getNumFiles());

        {
            Map urnToLong = ctCache.getUrnToTime(); 
            assertEquals(""+urnToLong, 1, urnToLong.size());
        }
        {
            Map longToUrns = ctCache.getTimeToUrn();
            assertEquals(""+longToUrns, 1, longToUrns.size());
        }

        File newFile = new File(_savedDir, "anita.txt");
        assertTrue(newFile.exists());
        URN newFileURN = fm.getURNForFile(newFile);
        assertEquals(cTime[0], ctCache.getCreationTime(newFileURN));
    }

    public void testInstallersNotSharedInCache() throws Exception {
        //  Make sure that the modified times are not the same
        susheel.setLastModified(123456);
        berkeley.setLastModified(123457);
        
        FileManager fm = fileManager;
        CreationTimeCache ctCache = creationTimeCache;

        File winInstaller = TestUtils.getResourceFile("com/limegroup/gnutella/Backend.java");
        File linInstaller = TestUtils.getResourceFile("com/limegroup/gnutella/GUIDTest.java");
        File osxInstaller = TestUtils.getResourceFile("com/limegroup/gnutella/UrnTest.java");

        //  Gotta make use of the force-share folder for this test
        if( SharingUtils.PROGRAM_SHARE.exists() ) {
            File [] toDelete = SharingUtils.PROGRAM_SHARE.listFiles();
            for (int j = 0; j < toDelete.length; j++) {
                toDelete[j].delete();
            }
        } else {
            SharingUtils.PROGRAM_SHARE.mkdir();
        }

        File winDst = new File(SharingUtils.PROGRAM_SHARE, "LimeWireWin3.69.0010.exe");
        File linDst = new File(SharingUtils.PROGRAM_SHARE, "LimeWireLinux.bin");
        File osxDst = new File(SharingUtils.PROGRAM_SHARE, "LimeWireOSX.dmg");
        
        FileUtils.copy(winInstaller, winDst);
        FileUtils.copy(linInstaller, linDst);
        FileUtils.copy(osxInstaller, osxDst);
        
        winDst.deleteOnExit();
        linDst.deleteOnExit();
        osxDst.deleteOnExit();
        
        try {
            fileManager.loadSettings();
            int i = 0;
            for (; (i < 15) && (fileManager.getNumFiles()+fileManager.getNumForcedFiles() < 5); i++)
                Thread.sleep(1000);
            if (i == 15)
                fail("num shared files? " + fileManager.getNumFiles());
    
            // we should be sharing two files - two text files and three installers
            // but the creation time cache should only have the two text files
            // as entries....
            //
            //  NOTE: with forced folder sharing, there will be only 2 shared files (forced
            //      files don't count), and 3 forced shared files
            assertEquals(2, fileManager.getNumFiles());
            assertEquals(3, fileManager.getNumForcedFiles());
    
            {
                Map urnToLong = creationTimeCache.getUrnToTime();
                assertEquals(2, urnToLong.size());
            }
            {
                Map longToUrns = creationTimeCache.getTimeToUrn();
                assertEquals(2, longToUrns.size());
            }
    
            // make sure the installer urns are not in the cache
            {
                assertTrue(winInstaller.exists());
                URN installerURN = fm.getURNForFile(winInstaller);
                assertNull(ctCache.getCreationTime(installerURN));
            }
            {
                assertTrue(winInstaller.exists());
                URN installerURN = fm.getURNForFile(linInstaller);
                assertNull(ctCache.getCreationTime(installerURN));
            }
            {
                assertTrue(winInstaller.exists());
                URN installerURN = fm.getURNForFile(osxInstaller);
                assertNull(ctCache.getCreationTime(installerURN));
            }
            // make sure berkeley and susheel are in the cache.
            {
                assertTrue(berkeley.exists());
                assertNotNull(ctCache.getCreationTime(fm.getURNForFile(berkeley)));
            }
            {
                assertTrue(susheel.exists());
                assertNotNull(ctCache.getCreationTime(fm.getURNForFile(susheel)));
            }
        
        } finally {        
            File [] toDelete = SharingUtils.PROGRAM_SHARE.listFiles();
            for (int j = 0; j < toDelete.length; j++) {
                toDelete[j].delete();
            }
            SharingUtils.PROGRAM_SHARE.delete();

        }
    }

}

