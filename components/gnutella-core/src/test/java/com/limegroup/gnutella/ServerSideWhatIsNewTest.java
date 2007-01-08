package com.limegroup.gnutella;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

import com.limegroup.gnutella.downloader.TestFile;
import com.limegroup.gnutella.downloader.TestUploader;
import com.limegroup.gnutella.messages.FeatureSearchData;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVM;
import com.limegroup.gnutella.messages.vendor.MessagesSupportedVendorMessage;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.SearchSettings;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

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
    private static File winInstaller = null;
    private static File linInstaller = null;
    private static File osxInstaller = null;

    public ServerSideWhatIsNewTest(String name) {
        super(name);
    }
    
    public static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    public static Integer numUPs() {
        return new Integer(1);
    }

    public static Test suite() {
        return buildTestSuite(ServerSideWhatIsNewTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void tearDown() { 
        //  Now that this class is ClientSideTestCase derived, it reuses connections.
        //      Therefore, since we are re-sending the same query string repeatedly, 
        //      we need to pause a bit longer between each test, or else DuplicateFilter.allow()
        //      will reject the subsequent queries as duplicates.
        try {
            Thread.sleep(3500);
        } catch (InterruptedException unused) {}
    }
    
    private static void doSettings() throws Exception {
        //Setup LimeWire backend.  For testing other vendors, you can skip all
        //this and manually configure a client in leaf mode to listen on port
        //6669, with no slots and no connections.  But you need to re-enable
        //the interactive prompts below.
        ConnectionSettings.PORT.setValue(PORT);
        ConnectionSettings.DO_NOT_BOOTSTRAP.setValue(true);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
		ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        
        //  Required so that the "swarmDownloadCatchesEarlyCreationTest" actually works  =)
        ConnectionSettings.CONNECTION_SPEED.setValue(SpeedConstants.T3_SPEED_INT);
		SharingSettings.EXTENSIONS_TO_SHARE.setValue("txt;exe;bin;dmg");
        setSharedDirectories( new File[] { _sharedDir, _savedDir } );
        // get the resource file for com/limegroup/gnutella
        berkeley = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/berkeley.txt");
        susheel = 
            CommonUtils.getResourceFile("com/limegroup/gnutella/susheel.txt");
        // now move them to the share dir
        FileUtils.copy(berkeley, new File(_sharedDir, "berkeley.txt"));
        FileUtils.copy(susheel, new File(_sharedDir, "susheel.txt"));
        
        berkeley = new File(_sharedDir, "berkeley.txt");
        susheel = new File(_sharedDir, "susheel.txt");
        
        winInstaller = new File(_sharedDir, "LimeWireWin3.69.0010.exe");
        linInstaller = new File(_sharedDir, "LimeWireLinux.bin");
        osxInstaller = new File(_sharedDir, "LimeWireOSX.dmg");
        // make sure results get through
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(-2);
    }        
    
    public void setUp() throws Exception  {
        doSettings();
    }
    
    public static boolean shouldRespondToPing() {
        return false;
    }


    ///////////////////////// Actual Tests ////////////////////////////
    
    // THIS TEST SHOULD BE RUN FIRST!!
    // just test that What Is New support is advertised
    public void testSendsCapabilitiesMessage() throws Exception {
        //testUP = connect(rs, 6355, true);
        Connection testUP = ClientSideTestCase.testUP[0];

        // send a MessagesSupportedMessage and capabilities VM
        testUP.send(MessagesSupportedVendorMessage.instance());
        testUP.send(CapabilitiesVM.instance());
        testUP.flush();

        Thread.sleep(100);
        
        // we expect to get a CVM back
        Message m = null;
        do {
            m = testUP.receive(TIMEOUT);
        } while (!(m instanceof CapabilitiesVM)) ;
        assertTrue(((CapabilitiesVM)m).supportsWhatIsNew());

        // client side seems to follow the setup process A-OK
    }

    // test that the CreationTimeCache is as expected
    public void testCreationTimeCacheInitialState() throws Exception {
        // wait for fm to finish
        int i = 0;
        for (; (i < 15) && (RouterService.getNumSharedFiles() < 2); i++)
            Thread.sleep(1000);
        if (i == 15) assertTrue(false);

        // we should be sharing two files - two text files.
        assertEquals(2, RouterService.getNumSharedFiles());

        CreationTimeCache ctCache = CreationTimeCache.instance();
        FileManager fm = RouterService.getFileManager();
        URN berkeleyURN = fm.getURNForFile(berkeley);
        URN susheelURN = fm.getURNForFile(susheel);

        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(2, urnToLong.size());
            assertNotNull(""+urnToLong, urnToLong.get(berkeleyURN));
            assertNotNull(""+urnToLong, urnToLong.get(susheelURN));
        }

        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
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
    }


    // make sure that a what is new query is answered correctly
    public void testWhatIsNewQueryBasic() throws Exception {
        Connection testUP = ClientSideTestCase.testUP[0];
        drain(testUP);

        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, null, false, Message.N_UNKNOWN, false,
                             FeatureSearchData.WHAT_IS_NEW);
        whatIsNewQuery.hop();
        testUP.send(whatIsNewQuery);
        testUP.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) getFirstInstanceOfMessageType(testUP,
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
        Connection testUP = ClientSideTestCase.testUP[0];
        drain(testUP);

        {
        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, null, false, Message.N_UNKNOWN, false,
                             FeatureSearchData.WHAT_IS_NEW, false,
                             0 | QueryRequest.AUDIO_MASK);
        whatIsNewQuery.hop();
        testUP.send(whatIsNewQuery);
        testUP.flush();

        // give time to process
        Thread.sleep(2000);

        QueryReply reply = 
            (QueryReply) getFirstInstanceOfMessageType(testUP,
                                                       QueryReply.class);
        assertNull(reply);
        }

        {
        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, null, false, Message.N_UNKNOWN, false,
                             FeatureSearchData.WHAT_IS_NEW, false,
                             0 | QueryRequest.DOC_MASK);
        whatIsNewQuery.hop();
        testUP.send(whatIsNewQuery);
        testUP.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) getFirstInstanceOfMessageType(testUP,
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
        CreationTimeCache ctCache = CreationTimeCache.instance();
        FileManager fm = RouterService.getFileManager();
        URN berkeleyURN = fm.getURNForFile(berkeley);
        URN susheelURN = fm.getURNForFile(susheel);

        tempFile1 = new File("tempFile1.txt");
        tempFile2 = new File("tempFile2.txt");
        tempFile1.deleteOnExit(); tempFile2.deleteOnExit();

        FileWriter writer = null;
        {
            writer = new FileWriter(tempFile1);
            writer.write(tempFile1.getName(), 0, tempFile1.getName().length());
            writer.flush();
            writer.close();
        }
        {
            writer = new FileWriter(tempFile2);
            writer.write(tempFile2.getName(), 0, tempFile2.getName().length());
            writer.flush();
            writer.close();
        }
        
        // now move them to the share dir
        FileUtils.copy(tempFile1, new File(_sharedDir, "tempFile1.txt"));
        FileUtils.copy(tempFile2, new File(_sharedDir, "tempFile2.txt"));
        tempFile1 = new File(_sharedDir, "tempFile1.txt");
        tempFile2 = new File(_sharedDir, "tempFile2.txt");
        assertTrue(tempFile1.exists());
        assertTrue(tempFile2.exists());

        RouterService.getFileManager().loadSettings();
        int i = 0;
        for (; (i < 15) && (RouterService.getNumSharedFiles() < 4); i++)
            Thread.sleep(1000);
        if (i == 15)
            fail("num shared files? " + RouterService.getNumSharedFiles());

        URN tempFile1URN = fm.getURNForFile(tempFile1);
        URN tempFile2URN = fm.getURNForFile(tempFile2);
        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(4, urnToLong.size());
            assertNotNull(""+urnToLong, urnToLong.get(berkeleyURN));
            assertNotNull(""+urnToLong, urnToLong.get(susheelURN));
            assertNotNull(""+urnToLong, urnToLong.get(tempFile1URN));
            assertNotNull(""+urnToLong, urnToLong.get(tempFile2URN));
        }
        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            assertTrue(""+longToUrns, 
                       (longToUrns.size() == 2) || (longToUrns.size() == 3) ||
                       (longToUrns.size() == 4));
        }
    }

    // test that after the sharing of additional files, the what is new query
    // results in something else
    public void testWhatIsNewQueryNewFiles() throws Exception {
        Connection testUP = ClientSideTestCase.testUP[0];
        drain(testUP);

        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, null, false, Message.N_UNKNOWN, false, 
                             FeatureSearchData.WHAT_IS_NEW);
        testUP.send(whatIsNewQuery);
        testUP.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) getFirstInstanceOfMessageType(testUP,
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
        FileManager fm = RouterService.getFileManager();
        CreationTimeCache ctCache = CreationTimeCache.instance();
        URN tempFile1URN = fm.getURNForFile(tempFile1);
        Long cTime = ctCache.getCreationTime(tempFile1URN);

        FileWriter writer = null;
        {
            writer = new FileWriter(tempFile1);
            writer.write(berkeley.getName(), 0, berkeley.getName().length());
            writer.flush();
            writer.close();
        }

        FileDesc beforeChanged = fm.getFileDescForFile(tempFile1);
        assertNotNull(beforeChanged);
        fm.fileChanged(tempFile1);
        Thread.sleep(3000);
        FileDesc afterChanged = fm.getFileDescForFile(tempFile1);
        assertNotNull(afterChanged);
        assertNotSame(beforeChanged, afterChanged);
        
        assertNotNull(fm.getURNForFile(tempFile1));
        assertNotEquals(tempFile1URN, fm.getURNForFile(tempFile1));
        assertEquals(ctCache.getCreationTime(fm.getURNForFile(tempFile1)),
                     cTime);

        // now just send another What Is New query and make sure everything
        // is kosher - probably overkill but whatever....
        Connection testUP = ClientSideTestCase.testUP[0];
        drain(testUP);

        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, null, false, Message.N_UNKNOWN, false, 
                             FeatureSearchData.WHAT_IS_NEW);
        testUP.send(whatIsNewQuery);
        testUP.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) getFirstInstanceOfMessageType(testUP,
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
        FileManager fm = RouterService.getFileManager();
        CreationTimeCache ctCache = CreationTimeCache.instance();
        URN tempFile1URN = fm.getURNForFile(tempFile1);
//        URN tempFile2URN = fm.getURNForFile(tempFile2);
        // we are changing tempFile1 to become tempFile2 - but since we
        // call fileChanged(), then the common URN should get tempFile1's
        // cTime
        Long cTime = ctCache.getCreationTime(tempFile1URN);

        FileWriter writer = null;
        {
            writer = new FileWriter(tempFile1);
            writer.write(tempFile2.getName(), 0, tempFile2.getName().length());
            writer.flush();
            writer.close();
        }

        FileDesc beforeChanged = fm.getFileDescForFile(tempFile1);
        assertNotNull(beforeChanged);
        fm.fileChanged(tempFile1);
        Thread.sleep(3000);
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
        Connection testUP = ClientSideTestCase.testUP[0];
        drain(testUP);

        QueryRequest whatIsNewQuery = 
            new QueryRequest(GUID.makeGuid(), (byte)2, 
                             QueryRequest.WHAT_IS_NEW_QUERY_STRING, "", null, 
                             null, null, false, Message.N_UNKNOWN, false, 
                             FeatureSearchData.WHAT_IS_NEW);
        testUP.send(whatIsNewQuery);
        testUP.flush();

        // give time to process
        Thread.sleep(1000);

        QueryReply reply = 
            (QueryReply) getFirstInstanceOfMessageType(testUP,
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
        FileManager fm = RouterService.getFileManager();
        CreationTimeCache ctCache = CreationTimeCache.instance();        
        
        int size = 0;
        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(3, urnToLong.size());
        }
        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            size = longToUrns.size();
            assertTrue((longToUrns.size() == 2) || (longToUrns.size() == 3));
        }
        
        // tempFile1 is the same URN as tempFile2
        fm.removeFileIfShared(tempFile1);

        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(3, urnToLong.size());
        }
        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            assertEquals(longToUrns.size(), size);
        }
        
        // tempFile2 should result in a delete
        fm.removeFileIfShared(tempFile2);

        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(2, urnToLong.size());
        }
        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            assertEquals(longToUrns.size(), (size-1));
        }
    }


    // manually delete a file, make sure it isn't shared and that the CTC has
    // the correct sizes, etc...
    public void testManualFileDeleteLoadSettings() throws Exception {
        FileManager fm = RouterService.getFileManager();
        CreationTimeCache ctCache = CreationTimeCache.instance();

        tempFile1.delete(); tempFile1 = null;
        tempFile2.delete(); tempFile2 = null;
        berkeley.delete(); berkeley = null;

        fm.loadSettings();
        Thread.sleep(2000);
        assertEquals("num shared files", 1, RouterService.getNumSharedFiles());

        URN susheelURN = fm.getURNForFile(susheel);
        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(""+urnToLong, 1, urnToLong.size());
            assertNotNull(""+urnToLong, urnToLong.get(susheelURN));
        }
        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            assertEquals(""+longToUrns, 1, longToUrns.size());
        }
    }
    

    // download a file and make sure the creation time given back is stored...
    public void testDownloadCapturesCreationTime() throws Exception {
        FileManager fm = RouterService.getFileManager();
        CreationTimeCache ctCache = CreationTimeCache.instance();
        
        final int UPLOADER_PORT = 10000;
        byte[] guid = GUID.makeGuid();
        TestUploader uploader = new TestUploader("whatever.txt", UPLOADER_PORT);
        Long cTime = new Long(2);
        uploader.setCreationTime(cTime);
        Set urns = new HashSet();
        urns.add(TestFile.hash());
        RemoteFileDesc rfd = new RemoteFileDesc("127.0.0.1", UPLOADER_PORT, 1, 
                                                "whatever.txt", 
                                                TestFile.length(), 
                                                guid, 1, false, 3,
                                                false, null, urns, false,
                                                false, "LIME", new HashSet(), -1);
        Downloader downloader = 
            RouterService.download(new RemoteFileDesc[] { rfd }, false, new GUID(guid));
        
        int sleeps = 0;
        while (downloader.getState() != Downloader.COMPLETE) {
            Thread.sleep(1000);
            sleeps++;
            if (sleeps > 320) assertTrue("download never completed", false);
        }
        
        assertEquals("num shared files? " + RouterService.getNumSharedFiles(), 2,
                RouterService.getNumSharedFiles());

        File newFile = new File(_savedDir, "whatever.txt");
        assertTrue(newFile.exists());
        URN newFileURN = fm.getURNForFile(newFile);
        assertEquals(TestFile.hash(), newFileURN);
        assertEquals(cTime, ctCache.getCreationTime(newFileURN));

        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(""+urnToLong, 2, urnToLong.size());
        }
        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            assertEquals(""+longToUrns, 2, longToUrns.size());
        }

    }


    // download a file and make sure the creation time given back is stored...
    public void testSwarmDownloadCapturesOlderCreationTime() throws Exception {
        
        FileManager fm = RouterService.getFileManager();
        CreationTimeCache ctCache = CreationTimeCache.instance();

        // get rid of the old shared file
        File newFile = new File(_savedDir, "whatever.txt");
        fm.removeFileIfShared(newFile);
        newFile.delete();
        assertEquals("num shared files? " + RouterService.getNumSharedFiles(), 1,
                RouterService.getNumSharedFiles());
        
        final int UPLOADER_PORT = 20000;
        byte[] guid = GUID.makeGuid();
        TestUploader uploader[] = new TestUploader[4];
        Long cTime[] = new Long[uploader.length];
        RemoteFileDesc rfds[] = new RemoteFileDesc[uploader.length];
        for (int i = 0; i < uploader.length; i++) {
            uploader[i] = new TestUploader("anita.txt", UPLOADER_PORT+i);
            uploader[i].setRate(50);
            cTime[i] = new Long(5+i);
            uploader[i].setCreationTime(cTime[i]);
            Set urns = new HashSet();
            urns.add(TestFile.hash());
            rfds[i] = new RemoteFileDesc("127.0.0.1", UPLOADER_PORT+i, 1, 
                                         "anita.txt", TestFile.length(), 
                                         guid, 1, false, 3,
                                         false, null, urns, false,
                                         false, "LIME", new HashSet(), -1);
        }

        Downloader downloader = RouterService.download(rfds, false, new GUID(guid));
        
        Thread.sleep(2000);
        assertTrue(downloader.getState() != Downloader.COMPLETE);
        // make sure that the partial file has a creation time - by now one of
        // the downloaders must have got X-Create-Time
        assertNotNull(ctCache.getCreationTime(TestFile.hash()));

        int sleeps = 0;
        while (downloader.getState() != Downloader.COMPLETE) {
            Thread.sleep(1000);
            sleeps++;
            if (sleeps > 120) assertTrue("download never completed", false);
        }
        
        assertEquals("num shared files? " + RouterService.getNumSharedFiles(), 2,
                RouterService.getNumSharedFiles());

        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(""+urnToLong, 2, urnToLong.size());
        }
        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            assertEquals(""+longToUrns, 2, longToUrns.size());
        }

        newFile = new File(_savedDir, "anita.txt");
        assertTrue(newFile.exists());
        URN newFileURN = fm.getURNForFile(newFile);
        assertEquals(cTime[0], ctCache.getCreationTime(newFileURN));
    }

    public void testInstallersNotSharedInCache() throws Exception {
        //  Make sure that the modified times are not the same
        susheel.setLastModified(123456);
        berkeley.setLastModified(123457);
        
        FileManager fm = RouterService.getFileManager();
        CreationTimeCache ctCache = CreationTimeCache.instance();
        
        winInstaller =
            CommonUtils.getResourceFile("com/limegroup/gnutella/Backend.java");
        linInstaller =
            CommonUtils.getResourceFile("com/limegroup/gnutella/GUIDTest.java");
        osxInstaller =
            CommonUtils.getResourceFile("com/limegroup/gnutella/UrnTest.java");

        //  Gotta make use of the force-share folder for this test
        if( FileManager.PROGRAM_SHARE.exists() ) {
            File [] toDelete = FileManager.PROGRAM_SHARE.listFiles();
            for (int j = 0; j < toDelete.length; j++) {
                toDelete[j].delete();
            }
        } else {
            FileManager.PROGRAM_SHARE.mkdir();
        }

        FileUtils.copy(winInstaller, 
                         new File(FileManager.PROGRAM_SHARE, "LimeWireWin3.69.0010.exe"));
        FileUtils.copy(linInstaller, 
                         new File(FileManager.PROGRAM_SHARE, "LimeWireLinux.bin"));
        FileUtils.copy(osxInstaller, 
                         new File(FileManager.PROGRAM_SHARE, "LimeWireOSX.dmg"));

        RouterService.getFileManager().loadSettings();
        int i = 0;
        for (; (i < 15) && (RouterService.getNumSharedFiles()+RouterService.getFileManager().getNumForcedFiles() < 5); i++)
            Thread.sleep(1000);
        if (i == 15)
            fail("num shared files? " + RouterService.getNumSharedFiles());

        // we should be sharing two files - two text files and three installers
        // but the creation time cache should only have the two text files
        // as entries....
        //
        //  NOTE: with forced folder sharing, there will be only 2 shared files (forced
        //      files don't count), and 3 forced shared files
        assertEquals(2, RouterService.getNumSharedFiles());
        assertEquals(3, RouterService.getFileManager().getNumForcedFiles());

        {
            Map urnToLong = 
                (Map)PrivilegedAccessor.getValue(ctCache, "URN_TO_TIME_MAP");
            assertEquals(""+urnToLong, 2, urnToLong.size());
        }
        {
            Map longToUrns =
                (Map)PrivilegedAccessor.getValue(ctCache, "TIME_TO_URNSET_MAP");
            assertEquals(""+longToUrns, 2, longToUrns.size());
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
        
        File [] toDelete = FileManager.PROGRAM_SHARE.listFiles();
        for (int j = 0; j < toDelete.length; j++) {
            toDelete[j].delete();
        }
        FileManager.PROGRAM_SHARE.delete();

    }

}

