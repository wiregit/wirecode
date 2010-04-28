package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.TorrentScrapeData;
import org.limewire.bittorrent.TorrentTrackerScraper.ScrapeCallback;
import org.limewire.bittorrent.bencoding.Token;
//import org.limewire.core.impl.XMLTorrent;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.IOUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.xml.LimeXMLDocument;

public class TorrentTrackerScraperImplTest extends LimeTestCase {
    
    @Inject
    private MetaDataReader metaDataReader;
    
    @Inject
    private TorrentTrackerScraperImpl torrentTrackerScraperImpl;


    public static Test suite() {
        return buildTestSuite(TorrentTrackerScraperImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        LimeTestUtils.createInjectorNonEagerly(LimeTestUtils.createModule(this));
    }
    
    //TODO: fix this
//    public void testScrapeTorrent() throws Exception {
//        File torrentFile = TestUtils.getResourceFile("org/limewire/swarm/bittorrent/public_html/torrents/test-peer-dl-single-file.torrent");
//        assertTrue(torrentFile.exists());
//        LimeXMLDocument xmlDocument = metaDataReader.readDocument(torrentFile);
//        XMLTorrent xmlTorrent = new XMLTorrent(xmlDocument);
//        BlockingScrapeCallback callback = new BlockingScrapeCallback();
//        torrentTrackerScraperImpl.submitScrape(xmlTorrent.getTrackerURIS().get(0), xmlTorrent.getSha1(), callback);
//        TorrentScrapeData data = callback.getData(3, TimeUnit.SECONDS);
//        assertEquals(2, data.getComplete());
//        assertEquals(0, data.getIncomplete());
//        assertGreaterThan(2000, data.getDownloaded());
//    }
    
    public static BTData parseTorrentFile(File torrentFile) throws Exception {
        FileInputStream fis = null;
        FileChannel fileChannel = null;
        try {
            fis = new FileInputStream(torrentFile);
            fileChannel = fis.getChannel();
            Object obj = Token.parse(fileChannel);
            BTDataImpl torrentData = new BTDataImpl((Map)obj);
            torrentData.clearPieces();
            return torrentData;
        } finally {
            IOUtils.close(fis);
            IOUtils.close(fileChannel);
        }
    }
    
    private class BlockingScrapeCallback implements ScrapeCallback {
        
        private final CountDownLatch latch = new CountDownLatch(1);
        
        private volatile TorrentScrapeData data;

        @Override
        public void failure(String reason) {
            latch.countDown();
        }

        @Override
        public void success(TorrentScrapeData data) {
            this.data = data;
            latch.countDown();
        }
        
        TorrentScrapeData getData(long timeout, TimeUnit unit) throws Exception {
            assertTrue(latch.await(timeout, unit));
            return data;
        }
        
    }
}
