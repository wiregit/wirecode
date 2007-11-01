package com.limegroup.gnutella.downloader;



import java.io.File;
import java.io.IOException;
import java.util.List;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Downloader.DownloadStatus;
import com.limegroup.gnutella.messages.vendor.ContentResponse;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.ContentSettings;
import com.limegroup.gnutella.settings.DownloadSettings;
import com.limegroup.gnutella.tigertree.HashTree;

/**
 * Comprehensive test of downloads -- one of the most important tests in
 * LimeWire.
 */
public class DownloadTest extends DownloadTestCase {
    
    private static final Log LOG = LogFactory.getLog(DownloadTest.class);
        
    public DownloadTest(String name) {
        super(name);
    }

    public static Test suite() { 
        return buildTestSuite(DownloadTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    /**
     * Tests a basic download that does not swarm.
     */
    public void testSimpleDownload10() throws Exception {
        LOG.info("-Testing non-swarmed download...");
        
        RemoteFileDesc rfd=newRFD(PORTS[0], false);
        RemoteFileDesc[] rfds = {rfd};
        tGeneric(rfds);
    }
    
    public void testSimpleDownload11() throws Exception {
        LOG.info("-Testing non-swarmed download...");
        
        RemoteFileDesc rfd=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc[] rfds = {rfd};
        tGeneric(rfds);
    }
    
    /**
     * tests http11 downloads and the gray area allocation.
     */
    public void testTHEXDownload11() throws Exception {
        LOG.info("-Testing chunk allocation in a thex download...");
        
        
        final RemoteFileDesc rfd=newRFDWithURN(PORTS[0], false);
        final IncompleteFileManager ifm=downloadManager.getIncompleteFileManager();
        RemoteFileDesc[] rfds = {rfd};
        
        HTTP11Listener grayVerifier = new HTTP11Listener() {
            //private int requestNo;
            //public void requestHandled(){}
            //public void thexRequestStarted() {}

            /** The only lease that is DEFAULT_CHUNK_SIZE large */
       	    private Range firstLease = null;
            
            public void requestHandled(){}
            public void thexRequestStarted() {}
            public void thexRequestHandled() {}
            
       	    // checks whether we request chunks at the proper offset, etc.
            public void requestStarted(TestUploader uploader) {
                long fileSize = 0;
                
       	        Range i = null;
                try {
       	            IntervalSet leased = null;
       	            File incomplete = null;
                    incomplete = ifm.getFile(rfd);
                    assertNotNull(incomplete);
                    VerifyingFile vf = ifm.getEntry(incomplete);
                    fileSize = ((Long)PrivilegedAccessor.getValue(vf, "completedSize")
                                  ).longValue();
                    assertNotNull(vf);
            	    leased = (IntervalSet)
                                    PrivilegedAccessor.getValue(vf,"leasedBlocks");
            	    assertNotNull(leased);
                    List l = leased.getAllIntervalsAsList();
                    assertEquals(1,l.size());
                    i = (Range)l.get(0);
                } catch (Throwable bad) {
                  fail(bad);
                }
                
                assert i != null;
        		
                if (firstLease == null) {
                    // first request, we should have the chunk aligned to
                    // a DEFAULT_CHUNK_SIZE boundary
                    assertEquals("First chunk has improperly aligned low byte.",
                            0, i.getLow() % VerifyingFile.DEFAULT_CHUNK_SIZE);
                    if (i.getHigh() != fileSize-1 &&
                            i.getHigh() % VerifyingFile.DEFAULT_CHUNK_SIZE != 
                                VerifyingFile.DEFAULT_CHUNK_SIZE-1) {
                        assertTrue("First chunk has improperly aligned high byte.",
                                false);
                    }
                    firstLease = i;
                } else {
                    // on all other requests, we have 256k blocks
                    // Check that the low byte is aligned    
                    if (i.getLow() % (256 * 1024) != 0 &&
                            i.getLow() != firstLease.getHigh() + 1) {
                        assertTrue("Un-aligned low byte on chunk that is "+
                                "not adjascent to the DEFAULT_CHUNK_SIZE chunk.",
                                false);
                    }
                    // Check that the high byte is aligned    
                    if (i.getHigh() % (256 * 1024) != 256*1024-1 &&
                            i.getHigh() != firstLease.getLow() - 1 &&
                            i.getHigh() != fileSize-1) {
                        assertTrue("Un-aligned high byte on chunk that is "+
                                "not adjascent to the DEFAULT_CHUNK_SIZE chunk "+
                                "and is not the last chunk of the file",
                                false);
                    }
                } // close of if-else
            } // close of method
        }; // close of inner class
        
        testUploaders[0].setHTTPListener(grayVerifier);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        
        tigerTreeCache.purgeTree(rfd.getSHA1Urn());
        downloadServices.download(rfds, RemoteFileDesc.EMPTY_LIST, null, false);
        
        waitForComplete();
        assertEquals(6,testUploaders[0].getRequestsReceived());
        if (isComplete())
            LOG.debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        
        for (int i=0; i<rfds.length; i++) {
            File incomplete=ifm.getFile(rfds[i]);
            VerifyingFile vf=ifm.getEntry(incomplete);
            assertNull("verifying file should be null", vf);
        }
        
        assertEquals(1, testUploaders[0].getConnections());
    }
    
    public void testSimpleSwarm() throws Exception {
        LOG.info("-Testing swarming from two sources...");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);
        
        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }

    public void testUnbalancedSwarm() throws Exception  {
        LOG.info("-Testing swarming from two unbalanced sources...");
        
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE/10);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        assertLessThan("u1 did all the work", 9*TestFile.length()/10+FUDGE_FACTOR*10, u1);
        assertLessThan("u2 did all the work", TestFile.length()/10+FUDGE_FACTOR, u2);
    }


    public void testSwarmWithInterrupt() throws Exception {
        LOG.info("-Testing swarming from two sources (one broken)...");
        
        final int RATE=100;
        final int STOP_AFTER = TestFile.length()/4;
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[1].stopAfter(STOP_AFTER);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);

        // Download first from rfd2 so we get its stall
        // and then add in rfd1.
        tGeneric(new RemoteFileDesc[] { rfd2 },
                 new RemoteFileDesc[] { rfd1 });

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThanOrEquals("u2 did too much work", STOP_AFTER, u2);
        assertGreaterThan(0,u2);
    }
    
    /**
     * tests a swarm from a 1.0 and 1.1 source - designed to test stealing.
     */
    public void testSwarmWithTheft() throws Exception {
        LOG.info("-Testing swarming from two sources, one 1.0 and one 1.1");
        
        //Throttle rate at 10KB/s to give opportunities for swarming.
        final int RATE=500;
        //The first uploader got a range of 0-100%.  After the download receives
        //50%, it will close the socket.  But the uploader will send some data
        //between the time it sent byte 50% and the time it receives the FIN
        //segment from the downloader.  Half a second latency is tolerable.  
        final int FUDGE_FACTOR=RATE*1024;  
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=newRFD(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1,rfd2};
        
        tGeneric(rfds);
        
        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");
        
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        assertLessThan("u1 did all the work", TestFile.length()/2+FUDGE_FACTOR, u1);
        assertLessThan("u2 did all the work", TestFile.length()/2+FUDGE_FACTOR, u2);
    }

    public void testAddDownload() throws Exception {
        LOG.info("-Testing addDownload (increases swarming)...");
        
        final int RATE=500;
        final int FUDGE_FACTOR=RATE*1024;  
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);

        Downloader download=null;

        //Start one location, wait a bit, then add another.
        download=downloadServices.download(new RemoteFileDesc[] {rfd1}, false, null);
        ((ManagedDownloader)download).addDownload(rfd2,true);

        waitForComplete();
        if (isComplete())
            LOG.debug("pass"+"\n");
        else
            fail("FAILED: complete corrupt");

        //Make sure there weren't too many overlapping regions. Each upload
        //should do roughly half the work.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        //Note: The amount downloaded from each uploader will not 
        //be equal, because the uploaders are stated at different times.
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        assertLessThan("u1 did all the work", (TestFile.length()/2+FUDGE_FACTOR), u1);
        assertLessThan("u2 did all the work", (TestFile.length()/2+FUDGE_FACTOR), u2);
    }

    public void testStallingUploaderReplaced() throws Exception  {
        LOG.info("-Testing download completion with stalling downloader...");
        
        //Throttle rate at 100KB/s to give opportunities for swarming.
        final int RATE=100;
        testUploaders[0].setRate(0.1f);//stalling uploader
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1};

        ManagedDownloader downloader = (ManagedDownloader) 
            downloadServices.download(rfds,RemoteFileDesc.EMPTY_LIST, null, false);
        
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+1000);
        
        downloader.addDownload(rfd2,false);

        waitForComplete();

        //Make sure there weren't too many overlapping regions.
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        LOG.debug("\tu1: "+u1+"\n");
        LOG.debug("\tu2: "+u2+"\n");
        LOG.debug("\tTotal: "+(u1+u2)+"\n");

        //Note: The amount downloaded from each uploader will not 
        
        LOG.debug("passed"+"\n");//file downloaded? passed
    }
    
    
    public void testStallingHeaderUploader() throws Exception  {
        LOG.info("-Testing download completion with stalling downloader...");
        
        //Throttle rate at 100KB/s to give opportunities for swarming.
        final int RATE=300;
        testUploaders[0].setStallHeaders(true); //stalling uploader
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1};

        ManagedDownloader downloader = (ManagedDownloader) 
            downloadServices.download(rfds,RemoteFileDesc.EMPTY_LIST, null, false);
        
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()/2);
        
        downloader.addDownload(rfd2,false);

        waitForComplete();

         
        // the stalled uploader should not have uploaded anything
        assertEquals(0,testUploaders[0].getAmountUploaded());
        
        LOG.debug("passed"+"\n");//file downloaded? passed
    }
    
    public void testAcceptableSpeedStallIsReplaced() throws Exception {
        LOG.info("-Testing a download that is an acceptable speed but slower" +
                  " is replaced by another download that is faster");
        
        final int SLOW_RATE = 5;
        final int FAST_RATE = 50;
        testUploaders[0].setRate(SLOW_RATE);
        testUploaders[1].setRate(FAST_RATE);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1,rfd2};

        tGeneric(rfds);
        
        Thread.sleep(8000);
        int u1 = testUploaders[0].fullRequestsUploaded();
        int u2 = testUploaders[1].fullRequestsUploaded();
        int c1 = testUploaders[0].getConnections();
        int c2 = testUploaders[1].getConnections();
        
        assertEquals("u1 served full request", 0, u1);
        assertGreaterThan("u1 didn't upload anything",0,testUploaders[0].getAmountUploaded());
        assertGreaterThan("u2 not used", 0, u2);
        assertEquals("extra connection attempts", 1, c1);
        assertEquals("extra connection attempts", 1, c2);
        assertTrue("slower uploader not replaced",testUploaders[0].getKilledByDownloader());
        assertFalse("faster uploader killed",testUploaders[1].getKilledByDownloader());
    }
    
    public void testUploaderLowHigherRange()  throws Exception {
        LOG.info("-Testing that a download can handle an uploader giving low+higher ranges");
        testUploaders[0].setRate(25);
        testUploaders[0].setLowChunkOffset(50);
        testUploaders[4].setRate(100); // control, to finish the test.
        RemoteFileDesc rfd5 = newRFDWithURN(PORTS[4], false);
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc[] rfds = {rfd1};
        ManagedDownloader md = (ManagedDownloader)
            downloadServices.download(rfds,true,null);
        
        Thread.sleep(5000);
        // at this point we should stall since we'll never get our 50 bytes
        md.addDownloadForced(rfd5,false);
        
        waitForComplete();
        assertGreaterThanOrEquals(50,testUploaders[4].fullRequestsUploaded());
        assertGreaterThanOrEquals(100000-50,testUploaders[0].fullRequestsUploaded());
    }
    
    public void testUploaderLowLowerRange() throws Exception {
        LOG.info("-Testing that a download can handle an uploader giving low+lower ranges");
        testUploaders[0].setRate(25);
        testUploaders[0].setLowChunkOffset(-10);
        testUploaders[4].setRate(100); // control, to finish the test.
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd5 = newRFDWithURN(PORTS[4], false);
        
        RemoteFileDesc[] rfds = {rfd1};
        ManagedDownloader md = (ManagedDownloader)
            downloadServices.download(rfds,true,null);
        
        Thread.sleep(5000);
        md.addDownloadForced(rfd5,false);
        // the first downloader should have failed after downloading a complete chunk
        
        assertLessThan(100001,testUploaders[0].fullRequestsUploaded());
        waitForComplete();
        
    }
    
    public void testUploaderHighHigherRange() throws Exception {
        LOG.info("-Testing that a download can handle an uploader giving high+higher ranges");
        testUploaders[0].setRate(25);
        testUploaders[0].setHighChunkOffset(50);
        testUploaders[4].setRate(100); // control, to finish the test.
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd5 = newRFDWithURN(PORTS[4], false);
        
        RemoteFileDesc[] rfds = {rfd1};
        ManagedDownloader md = (ManagedDownloader)
            downloadServices.download(rfds,true,null);
        
        Thread.sleep(5000);
        md.addDownloadForced(rfd5,false);
        
        // the first downloader should have failed without downloading a complete chunk
        
        assertEquals(0,testUploaders[0].fullRequestsUploaded());
        waitForComplete();
    }
    
    public void testUploaderHighLowerRange() throws Exception {
        LOG.info("-Testing that a download can handle an uploader giving high+lower ranges");
        testUploaders[0].setRate(25);
        testUploaders[0].setHighChunkOffset(-10);
        testUploaders[4].setRate(100); // control, to finish the test.
        RemoteFileDesc rfd5 = newRFDWithURN(PORTS[4], false);
        
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc[] rfds = {rfd1};
        ManagedDownloader md = (ManagedDownloader)
            downloadServices.download(rfds,true,null);
        
        Thread.sleep(5000);
        // at this point we should stall since we'll never get our 10 bytes
        md.addDownloadForced(rfd5,false);
        
        waitForComplete();
        assertGreaterThanOrEquals(50,testUploaders[4].fullRequestsUploaded());
        assertGreaterThanOrEquals(100000-50,testUploaders[0].fullRequestsUploaded());
    }
       
    public void testReuseHostWithBadTree() throws Exception {
        LOG.info("-Testing that a host with a bad tree will be used");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(false);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        tigerTreeCache.purgeTree(TestFile.hash());
        
        // the tree will fail, but it'll pick up the content-length
        // and discard the rest of the bad data.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());        
    }
    
    public void testReuseHostWithBadTreeAndNoContentLength() throws Exception {
        setDownloadWaitTime(2 * DOWNLOAD_WAIT_TIME);
        LOG.info("-Testing that a host with a bad tree will be used");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(false);
        testUploaders[0].setSendContentLength(false);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        tigerTreeCache.purgeTree(TestFile.hash());
        
        // should pass after a bit because it retries the host
        // who gave it the bad length.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(2, testUploaders[0].getConnections());
    }
    
    public void testGetsThex() throws Exception {
        LOG.info("test that a host gets thex");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        tigerTreeCache.purgeTree(TestFile.hash());
        
        // it will fail the first time, then re-use the host after
        // a little waiting and not request thex.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNotNull(tree);
        assertEquals(TestFile.tree().getRootHash(), tree.getRootHash());
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());        
    }
    
    public void testQueuedOnThexContinues() throws Exception {
        LOG.info("test that queued on thex continues");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setQueueOnThex(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        
        // it will fail the first time, then re-use the host after
        // a little waiting and not request thex.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());
    }
    
    public void testBadHeaderOnThexContinues() throws Exception {
        LOG.info("test bad header on thex continues");
        final int RATE=500;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setUseBadThexResponseHeader(true);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        tigerTreeCache.purgeTree(TestFile.hash());
        
        // it will fail the first time, then re-use the host after
        // a little waiting and not request thex.
        tGeneric(new RemoteFileDesc[] { rfd1 } );
        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        
        assertTrue(testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());
    }    
    
    public void testKeepCorrupt() throws Exception {
        LOG.info("-Testing that if the user chooses to keep a corrupt download the download" +
                "will eventually finish");
        final int RATE = 100;
        testUploaders[0].setCorruption(true);
        testUploaders[0].setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        testUploaders[0].setRate(RATE);
        
        testUploaders[1].setCorruption(true);
        testUploaders[1].setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        testUploaders[1].setRate(RATE);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        
        downloadServices.download(new RemoteFileDesc[]{rfd1, rfd2}, RemoteFileDesc.EMPTY_LIST, null, false);
        waitForComplete();

        
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        assertTrue(activityCallback.corruptChecked);
        
        // tried once or if twice then failed the second time.
        assertLessThanOrEquals(2, testUploaders[0].getConnections());
        // tried once or twice.
        assertLessThanOrEquals(2, testUploaders[1].getConnections());
        
        assertGreaterThanOrEquals(TestFile.length(), 
                testUploaders[0].getAmountUploaded()+testUploaders[1].getAmountUploaded());
    }
    
    public void testDiscardCorrupt() throws Exception {
        LOG.info("-Testing that if the user chooses to discard a corrupt download it will terminate" +
                "immediately");
        
        final int RATE = 100;
        activityCallback.delCorrupt = true;
        activityCallback.corruptChecked = false;
        testUploaders[0].setCorruption(true);
        testUploaders[0].setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        testUploaders[0].setSendThexTreeHeader(true);
        testUploaders[0].setSendThexTree(true);
        testUploaders[0].setRate(RATE);
        
        testUploaders[1].setCorruption(true);
        testUploaders[1].setCorruptPercentage(VerifyingFile.MAX_CORRUPTION);
        testUploaders[1].setRate(RATE);
        
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        
        tGenericCorrupt( new RemoteFileDesc[] { rfd1}, new RemoteFileDesc[] {rfd2} );
        HashTree tree = tigerTreeCache.getHashTree(TestFile.hash());
        assertNull(tree);
        assertTrue(activityCallback.corruptChecked);
        
        // tried once or if twice then failed the second time.
        assertLessThanOrEquals(2, testUploaders[0].getConnections());
        // tried once or twice.
        assertLessThanOrEquals(2, testUploaders[1].getConnections());
        
        assertGreaterThanOrEquals(TestFile.length(), 
                testUploaders[0].getAmountUploaded()+testUploaders[1].getAmountUploaded());
    }
    
    
    public void testMismatchedVerifyHashNoStopOnCorrupt() throws Exception {
        tMismatchedVerifyHash(false, false);
    }
    
    public void testMismatchedVerifyHashStopOnCorrupt() throws Exception {
        activityCallback.delCorrupt = true;        
        tMismatchedVerifyHash(true, false);
    }
    
    public void testMismatchedVerifyHashWithThexNoStopOnCorrupt()
      throws Exception {
        tMismatchedVerifyHash(false, true);
    }
    
    public void testMismatchedVerifyHashWithThexStopOnCorrupt() throws Exception{
        activityCallback.delCorrupt = true;
        tMismatchedVerifyHash(true, true);
    }

    // note that this test ONLY works because the TestUploader does NOT SEND
    // a Content-Urn header.  if it did, the download would immediately fail
    // when reading the header. 
    private void tMismatchedVerifyHash(boolean deleteCorrupt, boolean getThex )
      throws Exception {
        LOG.info("-Testing file declared corrupt, when hash of "+
                         "downloaded file mismatches bucket hash" +
                         "stop when corrupt "+ deleteCorrupt+" ");
        String badSha1 = "urn:sha1:PLSTHIPQGSSZTS5FJUPAKUZWUGYQYPFB";

        final int RATE=100;
        testUploaders[0].setRate(RATE);
        testUploaders[0].setSendThexTreeHeader(getThex);
        testUploaders[0].setSendThexTree(getThex);
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0],badSha1, false);
        
        URN badURN = URN.createSHA1Urn(badSha1);
        tigerTreeCache.purgeTree(TestFile.hash());
        tigerTreeCache.purgeTree(badURN);
        
        downloadServices.download(new RemoteFileDesc[] {rfd1}, false, null);
        // even though the download completed, we ignore the tree 'cause the
        // URNs didn't match.
        assertNull(tigerTreeCache.getHashTree(TestFile.hash()));
        assertNull(tigerTreeCache.getHashTree(badURN));

        waitForComplete(deleteCorrupt);
        assertTrue(activityCallback.corruptChecked);
        assertEquals(getThex, testUploaders[0].thexWasRequested());
        assertEquals(1, testUploaders[0].getConnections());
    }

    public void testQueuedDownloader() throws Exception {
        LOG.info("-Testing queued downloader. \n");
        
        testUploaders[0].setQueue(true);
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc[] rfds = {rfd1};
        //the queued downloader will resend the query after sleeping,
        //and then it shold complete the download, because TestUploader
        //resets queue after sending 503
        tGeneric(rfds);
    }
    
    public void testBusyHostIsUsed() throws Exception {
        setDownloadWaitTime(2 * DEFAULT_WAIT_TIME);
        LOG.info("-Testing a once-busy host is reused.");
        
        //Throttle rate to give opportunities for swarming.
        final int SLOW_RATE=5;
        final int FAST_RATE=100;
        testUploaders[0].setBusy(true);
        testUploaders[0].setTimesBusy(1);
        testUploaders[0].setRate(FAST_RATE);
        testUploaders[1].setRate(SLOW_RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1}; // see note below about why only rfd1
        
        // Interesting odd factoid about the test:
        // Whether or not RFD1 or RFD2 is tried first is a BIG DEAL.
        // This test is making sure that RFD1 is reused even though
        // RFD2 is actively downloading.  However, because ManagedDownloader
        // sets the RetryAfter time differently depending on if
        // someone is already downloading (and this test will fail
        // if it sets the time to be the longer 10 minute wait),
        // we must ensure that RFD1 is tried first, so the wait
        // is only set to 1 minute.
        
        ManagedDownloader download= (ManagedDownloader) downloadServices.download(rfds, RemoteFileDesc.EMPTY_LIST, null, false);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+1000);
        download.addDownload(rfd2,true);
        
        waitForComplete();
        int u1 = testUploaders[0].getAmountUploaded();
        int u2 = testUploaders[1].getAmountUploaded();
        
        LOG.debug("u1: " + u1);
        LOG.debug("u2: " + u2);
        
        assertGreaterThan("u1 did no work", 0, u1);
        assertGreaterThan("u2 did no work", 0, u2);
        
        // This should ideally be an equals ( not >= ) but timing
        // conditions can cause assignGrey to fail too early,
        // causing more connection attempts.
        assertGreaterThanOrEquals("wrong connection attempts",
            2, testUploaders[0].getConnections());
        assertEquals("wrong connection attempts",
            1, testUploaders[1].getConnections());
    }


    /**
     * Tests that if the downloader has two sources, adding a third does not
     * cause it to drop either of the others -- important to test since we have
     * added logic that tries to knock off queued download and replace with good
     * downloaders
     */
    public void testFullSwarmDownloadsNotDropped() throws Exception {
        LOG.info("-testing that a good source does not dislodge other good ones"+
              " when swarming at capacity");
       int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 30;
        testUploaders[0].setRate(RATE);
        testUploaders[2].setRate(RATE);
        testUploaders[1].setRate(RATE);
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1, rfd2};
        
        RemoteFileDesc rfd3 = newRFDWithURN(PORTS[2], false);
        
        ManagedDownloader downloader = null;        
        downloader=(ManagedDownloader)downloadServices.download(rfds, false, null);
        Thread.sleep(2 * DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        int swarm = downloader.getActiveWorkers().size();
        int queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",0, queued);

        //try to add a third
        downloader.addDownloadForced(rfd3, true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        
        //make sure we did not kill anybody
        swarm = downloader.getActiveWorkers().size();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not replaced ",0, queued);

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");
        int u1 = testUploaders[0].getAmountUploaded();
        int u2 = testUploaders[1].getAmountUploaded();
        int u3 = testUploaders[2].getAmountUploaded();

        // we only care that the 3rd downloader doesn't download anything -
        // how the other two downloaders split the file between themselves 
        // doesn't matter.
        assertGreaterThan("u1 did not do any work",0,u1);
        assertGreaterThan("u2 did not do any work",0,u2);
        assertGreaterThanOrEquals("u3 did some work",TestFile.length(),u1+u2);
        assertEquals("u3 replaced a good downloader",0,u3);

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }

    /**
     * Tests that an uploader offering the file, replaces a queued uploader
     * when even at swarm capacity
     */
    public void testDownloadAtCapacityReplaceQueued() throws Exception {
        LOG.info("-testing that if max threads are queued or downloading, and a "+
              "good location comes along, the queued downloader is dislodged");
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 30;
        testUploaders[0].setRate(RATE);
        testUploaders[2].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[1].setQueue(true);
        testUploaders[1].setUnqueue(false); //never unqueue this uploader.
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc[] rfds = {rfd1,rfd2};//one good and one queued
        
        RemoteFileDesc rfd3 = newRFDWithURN(PORTS[2], false);
        
        ManagedDownloader downloader = null;
        
        downloader=(ManagedDownloader)downloadServices.download(rfds, false, null);
        //Thread.sleep(1000);
        //downloader.addDownloadForced(rfd2,false);
        Thread.sleep(2 * DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        LOG.debug("about to check swarming");
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1, queued);

        downloader.addDownload(rfd3, true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        
        //make sure we killed the queued
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not replaced ",0, queued);

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");
        
        int u1 = testUploaders[0].getAmountUploaded();
        int u2 = testUploaders[1].getAmountUploaded();
        int u3 = testUploaders[2].getAmountUploaded();
        
        assertEquals("queued uploader uploaded",0,u2);
        assertGreaterThan("u3 not given a chance to run", 0, u3);
        assertLessThan("u1 did all the work",TestFile.length(),u1);  
        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }

    /**
     * Tests that when we have max download threads, and there is a queued
     * downloader, it does not get replaced by another queued downloader with a
     * worse position, but does get replaced by a queued downloader that has a
     * better position
     */
    public void testDownloadAtCapacityGetsBetterQueued() throws Exception {
        LOG.info("-testing that if max threads are queued or downloading, and a "+
              "queued downloader gets by a queued downloader only if the new "+
              "one has a better queue position");
        
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 50;
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[1].setQueue(true);
        testUploaders[1].setUnqueue(false); //never unqueue this uploader.
        testUploaders[1].setQueuePos(3);

        testUploaders[2].setRate(RATE);
        testUploaders[2].setQueue(true);
        testUploaders[2].setUnqueue(false); //never unqueue this uploader.
        testUploaders[2].setQueuePos(5);

        testUploaders[3].setRate(RATE);
        testUploaders[3].setQueue(true);
        testUploaders[3].setUnqueue(false); //never unqueue this uploader.
        testUploaders[3].setQueuePos(1);

        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc rfd3=newRFDWithURN(PORTS[2], false);
        RemoteFileDesc rfd4=newRFDWithURN(PORTS[3], false);
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        ManagedDownloader downloader = null;
        downloader = (ManagedDownloader)downloadServices.download(rfds, false, null);
        Thread.sleep(2 * DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        int qPos=downloader.getQueuePosition();
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 3 which is worse, nothing should change
        downloader.addDownload(rfd3,true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1500);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = downloader.getQueuePosition();
        LOG.debug("queued workers: "+downloader.getQueuedWorkers());
        LOG.debug("active workers: "+downloader.getActiveWorkers());
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 2 not queued ",1,queued);
        assertEquals("incorrect queue pos ",3,qPos);

        //now try adding uploader 4 which is better, we should drop uploader2
        downloader.addDownload(rfd4,true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1500);
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        qPos = downloader.getQueuePosition();
        
        assertEquals("incorrect swarming",2,swarm);
        assertEquals("uploader 4 not queued ",1,queued);
        assertEquals("incorrect queue pos ",1,qPos);        

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }

    /**
     *  Tests that queued downloads advance on the downloader, this is important
     *  because we use the queue position to decide which downloader to get rid
     *  off when a good uploader shows up
     */
    public void testQueueAdvancementWorks() throws Exception {
        LOG.info("-testing that if queued downloaders advance we downloaders "+
              "register that they did, so that the choice of which downloader"+
              " to replace is made correctly");
        int capacity=ConnectionSettings.CONNECTION_SPEED.getValue();
        ConnectionSettings.CONNECTION_SPEED.setValue(
                                            SpeedConstants.MODEM_SPEED_INT);
        final int RATE = 50;
        testUploaders[0].setRate(RATE);
        testUploaders[1].setRate(RATE);
        testUploaders[2].setRate(RATE);

        testUploaders[0].setQueue(true);
        testUploaders[0].setUnqueue(false); //never unqueue this uploader.
        testUploaders[0].setQueuePos(5);//the worse one
        testUploaders[1].setQueue(true);
        testUploaders[1].setUnqueue(false); //never unqueue this uploader.
        testUploaders[1].setQueuePos(3);//the better one
        
        RemoteFileDesc rfd1=newRFDWithURN(PORTS[0], false);
        RemoteFileDesc rfd2=newRFDWithURN(PORTS[1], false);
        RemoteFileDesc rfd3=newRFDWithURN(PORTS[2], false);
        
        RemoteFileDesc[] rfds = {rfd1, rfd2};//one good and one queued
        
        ManagedDownloader downloader = null;
        downloader = (ManagedDownloader)downloadServices.download(rfds,false,null);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()*2 + 1000);
        int swarm = downloader.getNumDownloaders();
        int queued = downloader.getQueuedHostCount();
        
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("both uploaders should be queued",2,queued);
        
        testUploaders[0].setQueuePos(1);//make uploader1 become better
        //wait for the downloader to make the next requests to uploaders.
        Thread.sleep(testUploaders[0].MIN_POLL+2000);

        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("both uploaders should still be queued",2,queued);
        
        downloader.addDownload(rfd3,true);
        Thread.sleep(DownloadSettings.WORKER_INTERVAL.getValue()+ 1000);
        //now uploader 2 should have been removed.
        swarm = downloader.getNumDownloaders();
        queued = downloader.getQueuedHostCount();
        int qPos = downloader.getQueuePosition();
        
        assertEquals("incorrect swarming ",2,swarm);
        assertEquals("queued uploader not dropped",1,queued);
        assertEquals("wrong uploader removed",1,qPos);

        waitForComplete();
        if(isComplete())
            LOG.debug("pass \n");
        else
            fail("FAILED: complete corrupt");

        ConnectionSettings.CONNECTION_SPEED.setValue(capacity);      
    }
    
    public void testPartialDownloads() throws IOException {
        LOG.info("-Testing partial downloads...");
        testUploaders[0].setPartial(true);
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc[] rfds = {rfd1};
        Downloader downloader = downloadServices.download(rfds,false,null);
        waitForBusy(downloader);
        assertEquals("Downloader did not go to busy after getting ranges",
                DownloadStatus.BUSY, downloader.getState());
    }
    
    /** Tests what happens if the content authority says no. 
     * LEAVE AS LAST -- (it does weird things otherwise) */
    public void testContentInvalid() throws Exception {
        LOG.info("-Testing partial downloads...");
        RemoteFileDesc rfd1 = newRFDWithURN(PORTS[0], false);
        RemoteFileDesc[] rfds = {rfd1};
        testUploaders[0].setRate(50);
        
        ContentSettings.CONTENT_MANAGEMENT_ACTIVE.setValue(true);
        ContentSettings.USER_WANTS_MANAGEMENTS.setValue(true);        
        downloadServices.download(rfds,false,null);
        Thread.sleep(1000);
        synchronized(COMPLETE_LOCK) {
        	contentManager.handleContentResponse(new ContentResponse(TestFile.hash(), false));
        	waitForInvalid();       
        }
    }

}
