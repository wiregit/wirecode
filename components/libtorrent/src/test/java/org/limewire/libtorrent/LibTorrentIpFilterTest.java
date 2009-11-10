package org.limewire.libtorrent;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.net.InetAddress;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentEvent;
import org.limewire.bittorrent.TorrentEventType;
import org.limewire.bittorrent.TorrentParams;
import org.limewire.bittorrent.TorrentState;
import org.limewire.bittorrent.TorrentIpFilter;
import org.limewire.listener.EventListener;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;
import org.limewire.util.BaseTestCase;
import org.limewire.util.ByteUtils;

/**
 * Test integration with LW IP blacklists.
 */
public class LibTorrentIpFilterTest extends BaseTestCase {

    /**
     * A directory containing the torrent data for this unit test.
     */
    private File torrentDir = null;

    private LibTorrentSession torrentManager;

    private LibTorrentWrapper libTorrentWrapper;

    private ScheduledExecutorService scheduledExecutorService;
    
    private File tempDir;

    @Override
    protected void setUp() throws Exception {
        torrentDir = TestUtils
                .extractResourceDirectory("org/limewire/swarm/bittorrent/public_html/torrents");

        libTorrentWrapper = new LibTorrentWrapper();
        scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
        torrentManager = new LibTorrentSession(libTorrentWrapper, scheduledExecutorService,
                new TestTorrentSettings());
        torrentManager.initialize();
        torrentManager.start();
    }

    @Override
    protected void tearDown() throws Exception {
        torrentManager.stop();
        FileUtils.deleteRecursive(tempDir);
    }

    /**
     * This test tries to download a torrent from a tracker/peer
     * setup on the www.limewire.org server.  In this test, the peer's address
     * is on the IP blacklist. 
     */
    public void testPeerAddressInBlackList() throws Exception {
        Integer addrAsInt = ByteUtils.beb2int(InetAddress.getByName("www.limewire.org").getAddress(), 0);
        TestIPFilter ipFilter = 
                new TestIPFilter(Collections.singletonList(addrAsInt));
        torrentManager.setIpFilter(ipFilter);
       Torrent torrent = initializeTorrent();

        torrent.start();
        
        // download should not finish.
        boolean downloadFinished = finishDownload(torrent);
        assertFalse(downloadFinished);
        
        // check to make sure that the peer's address was blocked.
        Collection<Integer> blockedAddresses = ipFilter.getActualBlockedAddresses();
        assertEquals(1, blockedAddresses.size());
        assertEquals(addrAsInt, blockedAddresses.iterator().next());
    }
    
    /**
     * This test tries to download a torrent from a tracker/peer
     * setup on the www.limewire.org server.  In this test, the peer's address
     * is NOT on the IP blacklist. 
     */
    public void testPeerAddressNotInBlackList() throws Exception {
        Integer addrAsInt = ByteUtils.beb2int(InetAddress.getByName("127.0.0.1").getAddress(), 0);
        TestIPFilter ipFilter = 
            new TestIPFilter(Collections.singletonList(addrAsInt));
        torrentManager.setIpFilter(ipFilter);
        Torrent torrent = initializeTorrent();
        torrent.start();
        
        // download should finish - verify download when done.
        boolean downloadFinished = finishDownload(torrent);
        assertTrue(downloadFinished);
        assertDownload("8055d620ba0c507c1af957b43648c99f", torrent.getTorrentDataFile(), 44425);
        
        // check to make sure that the peer's address was not blocked
        Collection<Integer> blockedAddresses = ipFilter.getActualBlockedAddresses();
        assertEquals(0, blockedAddresses.size());
    }

    /**
     * Test torrent download with no ip blacklist configured
     */
    public void testNoBlackList() throws Exception {
        Torrent torrent = initializeTorrent();
        torrent.start();
        
        // download should finish - verify download when done.
        boolean downloadFinished = finishDownload(torrent);
        assertTrue(downloadFinished);
        assertDownload("8055d620ba0c507c1af957b43648c99f", torrent.getTorrentDataFile(), 44425);
    }
    
    private Torrent initializeTorrent() throws Exception {
        File torrentFile = createFile("test-peer-dl-single-file.torrent");
        Torrent torrent = new TorrentImpl(libTorrentWrapper, scheduledExecutorService);
        tempDir = createTempDirectory();

        TorrentParams params = new TorrentParams(tempDir, torrentFile);
        torrent.init(params);

        torrentManager.addTorrent(torrent);
        return torrent;
    }

    private File createFile(String fileName) {
        String torrentfilePath = torrentDir.getAbsolutePath() + "/" + fileName;
        return new File(torrentfilePath);
    }

    private boolean finishDownload(Torrent torrent) throws InterruptedException {
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
            return countDownLatch.await(20, TimeUnit.SECONDS);
        }
        return true;
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

    private File createTempDirectory() throws IOException {
        File tempDir = File.createTempFile("tmp", "TorrentTest");
        tempDir.delete();
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        return tempDir;
    }
    
    private class TestIPFilter implements TorrentIpFilter {
        private Collection<Integer> notAllowed;

        private Collection<Integer> iPAddressesBlocked = new HashSet<Integer>();

        TestIPFilter(Collection<Integer> notAllowed) {
            this.notAllowed = notAllowed;    
        }
        
        public Collection<Integer> getActualBlockedAddresses() {
            return iPAddressesBlocked;
        }

        @Override
        public boolean allow(int ipAddress) {
            if (notAllowed.contains(ipAddress)) {
                iPAddressesBlocked.add(ipAddress);
                return false;
            }
            return true;
        }
    }

}
