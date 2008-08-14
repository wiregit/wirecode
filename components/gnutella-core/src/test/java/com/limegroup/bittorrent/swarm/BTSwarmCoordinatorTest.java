package com.limegroup.bittorrent.swarm;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.limewire.io.DiskException;
import org.limewire.swarm.Swarmer;
import org.limewire.swarm.http.SwarmHttpSource;
import org.limewire.swarm.http.SwarmerImplTest;
import org.limewire.swarm.impl.EchoSwarmCoordinatorListener;
import org.limewire.swarm.impl.SwarmerImpl;
import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.BTContext;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.BTMetaInfoFactory;
import com.limegroup.bittorrent.BTMetaInfoFactoryImpl;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.bittorrent.disk.DiskManagerFactory;
import com.limegroup.bittorrent.disk.DiskManagerListener;
import com.limegroup.bittorrent.disk.TorrentDiskManager;
import com.limegroup.bittorrent.handshaking.piecestrategy.LargestGapStartPieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.PieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.RandomGapStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.RandomPieceStrategy;
import com.limegroup.gnutella.util.FileServer;
import com.limegroup.gnutella.util.LimeTestCase;

public class BTSwarmCoordinatorTest extends LimeTestCase {
    private static final int TEST_PORT = 8080;

    /**
     * A directory containing the torrent data for this unit test.
     */
    public static final String TORRENT_DIR = System.getProperty("user.dir")
            + "/test-data/bittorrent/torrents";

    /**
     * A directory containing the torrent data for this unit test.
     */
    public static final String FILE_DIR = System.getProperty("user.dir")
            + "/test-data/bittorrent/public_html";

    private FileServer fileServer = null;

    public BTSwarmCoordinatorTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        fileServer = new FileServer(TEST_PORT, new File(FILE_DIR));
        fileServer.start();
        Thread.sleep(1000);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        System.out.println("===================================");
        fileServer.stop();
        fileServer.destroy();
        super.tearDown();
    }

    public void testSingleFileTorret() throws Exception {

        File torrentFile = createFile("test-single-webseed-single-file-no-peer.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile = torrentFileSystem.getIncompleteFiles().get(0);
        downloadedFile.delete();
        completeFile.deleteOnExit();
        downloadedFile.deleteOnExit();
        final Swarmer swarmer = createSwarmer(torrentContext, null);

        swarmer.start();
        long totalSize = torrentFileSystem.getTotalSize();
        URI uri = metaInfo.getWebSeeds()[0];
        swarmer.addSource(new SwarmHttpSource(uri, totalSize));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile, 44425);

    }

    public void testMultiFileTorret() throws Exception {

        File torrentFile = createFile("test-single-webseed-multiple-file-no-peer.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        File downloadedFile2 = torrentFileSystem.getIncompleteFiles().get(1);
        downloadedFile1.delete();
        downloadedFile2.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();
        downloadedFile2.deleteOnExit();
        final Swarmer swarmer = createSwarmer(torrentContext, null);

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri = metaInfo.getWebSeeds()[0];
        swarmer.addSource(new SwarmHttpSource(uri, totalSize));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);
        SwarmerImplTest.assertDownload("db1dc452e77d30ce14acca6bac8c66bc", downloadedFile2, 411090);

    }

    public void testMultiFileTorret2() throws Exception {

        File torrentFile = createFile("test-single-webseed-multiple-file-no-peer.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        File downloadedFile2 = torrentFileSystem.getIncompleteFiles().get(1);
        downloadedFile1.delete();
        downloadedFile2.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();
        downloadedFile2.deleteOnExit();

        final Swarmer swarmer = createSwarmer(torrentContext, new RandomGapStrategy(metaInfo));

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri = metaInfo.getWebSeeds()[0];
        swarmer.addSource(new SwarmHttpSource(uri, totalSize));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);
        SwarmerImplTest.assertDownload("db1dc452e77d30ce14acca6bac8c66bc", downloadedFile2, 411090);

    }

    public void testMultiFileTorret3() throws Exception {

        File torrentFile = createFile("test-single-webseed-multiple-file-no-peer.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        File downloadedFile2 = torrentFileSystem.getIncompleteFiles().get(1);
        downloadedFile1.delete();
        downloadedFile2.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();
        downloadedFile2.deleteOnExit();

        final Swarmer swarmer = createSwarmer(torrentContext, new LargestGapStartPieceStrategy(
                metaInfo));

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri = metaInfo.getWebSeeds()[0];
        swarmer.addSource(new SwarmHttpSource(uri, totalSize));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);
        SwarmerImplTest.assertDownload("db1dc452e77d30ce14acca6bac8c66bc", downloadedFile2, 411090);

    }

    public void testVuzeCreatedTorrent() throws Exception {

        File torrentFile = createFile("test_vuze_getright.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);

        downloadedFile1.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();

        final Swarmer swarmer = createSwarmer(torrentContext, new LargestGapStartPieceStrategy(
                metaInfo));

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri1 = metaInfo.getWebSeeds()[0];
        URI uri2 = metaInfo.getWebSeeds()[1];
        swarmer.addSource(new SwarmHttpSource(uri1, totalSize));
        swarmer.addSource(new SwarmHttpSource(uri2, totalSize));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);

    }
    
    public void testProblemTorrent() throws Exception {

        File torrentFile = createFile("test-multiple-webseed-single-file-no-peer.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        completeFile.delete();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);

        downloadedFile1.delete();
        completeFile.deleteOnExit();
        downloadedFile1.deleteOnExit();

        final Swarmer swarmer = createSwarmer(torrentContext, new LargestGapStartPieceStrategy(
                metaInfo));

        swarmer.start();

        long totalSize = torrentFileSystem.getTotalSize();
        URI uri1 = metaInfo.getWebSeeds()[0];
        URI uri2 = metaInfo.getWebSeeds()[1];
        URI uri3 = metaInfo.getWebSeeds()[2];
        swarmer.addSource(new SwarmHttpSource(uri1, totalSize));
        swarmer.addSource(new SwarmHttpSource(uri2, totalSize));
        swarmer.addSource(new SwarmHttpSource(uri3, totalSize));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);

    }
    

    private File createFile(String fileName) {
        File torrentFile = new File(TORRENT_DIR + "/" + fileName);
        return torrentFile;
    }

    public static BTMetaInfo createMetaInfo(File torrentFile) throws IOException {
        byte[] torrentBytes = FileUtils.readFileFully(torrentFile);
        BTMetaInfoFactory btm = new BTMetaInfoFactoryImpl();
        final BTMetaInfo metaInfo = btm.createBTMetaInfoFromBytes(torrentBytes);
        return metaInfo;
    }

    private Swarmer createSwarmer(TorrentContext torrentContext, PieceStrategy pieceStrategy)
            throws IOException {

        if (pieceStrategy == null) {
            pieceStrategy = new RandomPieceStrategy(torrentContext.getMetaInfo());
        }

        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        TorrentDiskManager torrentDiskManager = torrentContext.getDiskManager();
        torrentDiskManager.open(new DiskManagerListener() {

            public void chunkVerified(int id) {
                System.out.println("chunkVerified: " + id);

            }

            public void diskExceptionHappened(DiskException e) {
                System.out.println("diskExceptionHappened: " + e.getMessage());

            }

            public void verificationComplete() {
                System.out.println("verificationComplete");

            }

        });

        final BTSwarmCoordinator btCoordinator = new BTSwarmCoordinator(torrentContext
                .getMetaInfo(), torrentFileSystem, torrentDiskManager, pieceStrategy);
        btCoordinator.addListener(new EchoSwarmCoordinatorListener());

        Swarmer swarmer = new SwarmerImpl(btCoordinator);

        return swarmer;
    }

    // TODO test better variety of torrent files.
    // TODO test larger torrent files.
}