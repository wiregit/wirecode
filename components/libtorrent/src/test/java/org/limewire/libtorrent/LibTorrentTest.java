package org.limewire.libtorrent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentEventType;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.bittorrent.TorrentScrapeData;
import org.limewire.bittorrent.TorrentState;
import org.limewire.bittorrent.TorrentTrackerScraper.ScrapeCallback;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;

public class LibTorrentTest extends TestCase {

    /**
     * A directory containing the torrent data for this unit test.
     */
    private File torrentDir = null;

    /**
     * A temporary directory to download to.
     */
    private File tempDir = null;

    private LibTorrentSession libtorrentSession;
    private LibTorrentWrapper libTorrentWrapper;
    private ScheduledExecutorService scheduledExecutorService;

    @Override
    protected void setUp() throws Exception {
        torrentDir = TestUtils
                .extractResourceDirectory("org/limewire/swarm/bittorrent/public_html/torrents");

        tempDir = createTempDirectory();

        libTorrentWrapper = new LibTorrentWrapper();
        scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
        libtorrentSession = new LibTorrentSession(libTorrentWrapper, scheduledExecutorService,
                new TestTorrentSettings());
        libtorrentSession.initialize();
        libtorrentSession.start();
    }

    @Override
    protected void tearDown() throws Exception {
        libtorrentSession.stop();
        if (tempDir != null) {
            FileUtils.deleteRecursive(tempDir);
        }
    }

    /**
     * This test tries to download a single file torrent from a tracker/peer
     * setup on the www.limewire.org server.
     */
    public void testSingleFilePeerDownload() throws Exception {
        File torrentFile = getTorrentFile("test-peer-dl-single-file.torrent");

        TorrentParams params = new LibTorrentParams(tempDir, torrentFile);
        params.fill();
        Torrent torrent = libtorrentSession.addTorrent(params);
        torrent.start();
        finishTorrentDownload(torrent);
        assertDownload("8055d620ba0c507c1af957b43648c99f", torrent.getTorrentDataFile(), 44425);
    }
    
    public void testTrackerScraper() throws Exception {
        BlockingScrapeCallback callback = new BlockingScrapeCallback();
        // testing a ubuntu torrent
        libtorrentSession.queueTrackerScrapeRequest("8241bdf1f1214014297f87c93ee58a57a983ccc4",
                URI.create("udp://tracker.openbittorrent.com:80/"), callback);
        assertTrue(callback.latch.await(3, TimeUnit.SECONDS));
        assertNotNull(callback.data);
        assertTrue(callback.data.getDownloaded() > 0);
    }

    /**
     * Gets the specified torrent file from the torrent resource directory.
     */
    private File getTorrentFile(String fileName) {
        String torrentfilePath = torrentDir.getAbsolutePath() + "/" + fileName;
        File torrentFile = new File(torrentfilePath);
        return torrentFile;
    }

    /**
     * Awaits a torrent Completed download event. Or times out after 100
     * seconds.
     */
    private void finishTorrentDownload(Torrent torrent) throws InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        torrent.addListener(new EventListener<TorrentEvent>() {
            @Override
            public void handleEvent(TorrentEvent event) {
                if (TorrentEventType.COMPLETED == event.getType()) {
                    countDownLatch.countDown();
                }
            }
        });

        if (torrent.getStatus().getState() != TorrentState.SEEDING) {
            assertTrue("Time ran out before download completed.", countDownLatch.await(100,
                    TimeUnit.SECONDS));
        }

        assertNotNull(torrent.getStatus());
        assertEquals(TorrentState.SEEDING, torrent.getStatus().getState());
    }

    /**
     * Asserts that the given file has the correct size, and matches the given
     * md5sum.
     */
    private void assertDownload(String md5, File file, long fileSize) throws InterruptedException,
            NoSuchAlgorithmException, IOException {
        assertTrue(file.exists());
        assertEquals(fileSize, file.length());
        String testmd5 = FileUtils.getMD5(file);
        assertEquals(md5, testmd5);
    }

    /**
     * Creates a temporary directory.
     */
    private File createTempDirectory() throws IOException {
        File tempDir = File.createTempFile("tmp", "TorrentTest");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        return tempDir;
    }
    
    private class BlockingScrapeCallback implements ScrapeCallback {
        
        private final CountDownLatch latch = new CountDownLatch(1);
        
        volatile TorrentScrapeData data;
        
        @Override
        public void failure(String reason) {
            latch.countDown();
        }

        @Override
        public void success(TorrentScrapeData data) {
            this.data = data;
            latch.countDown();
        }

        @Override
        public String toString() {
            return StringUtils.toStringBlacklist(this, latch);
        }
    }
}
