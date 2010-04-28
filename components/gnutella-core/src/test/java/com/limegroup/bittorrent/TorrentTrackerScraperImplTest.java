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
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.IOUtils;
import org.limewire.util.Base32;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;
import org.limewire.util.URIUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.metadata.MetaDataReader;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

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
    
    public void testScrapeTorrent() throws Exception {
        File torrentFile = TestUtils.getResourceFile("org/limewire/swarm/bittorrent/public_html/torrents/test-peer-dl-single-file.torrent");
        assertTrue(torrentFile.exists());
        LimeXMLDocument xmlDocument = metaDataReader.readDocument(torrentFile);
        String tracker = xmlDocument.getValue(LimeXMLNames.TORRENT_TRACKERS);
        String hexSha1 = StringUtils.toHexString(Base32.decode(xmlDocument.getValue(LimeXMLNames.TORRENT_INFO_HASH)));
        BlockingScrapeCallback callback = new BlockingScrapeCallback();
        torrentTrackerScraperImpl.submitScrape(URIUtils.toURI(tracker), hexSha1, callback);
        TorrentScrapeData data = callback.getData(3, TimeUnit.SECONDS);
        assertEquals(1, data.getComplete());
        assertEquals(0, data.getIncomplete());
        assertGreaterThan(2000, data.getDownloaded());
    }
    
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
