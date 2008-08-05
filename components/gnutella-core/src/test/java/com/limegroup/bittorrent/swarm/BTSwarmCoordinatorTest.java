package com.limegroup.bittorrent.swarm;

import java.io.File;
import java.io.IOException;

import junit.framework.Test;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.params.HttpParams;
import org.limewire.io.DiskException;
import org.limewire.swarm.SourceEventListener;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceHandler;
import org.limewire.swarm.SwarmStatus;
import org.limewire.swarm.LoggingSwarmCoordinatorListener;
import org.limewire.swarm.Swarmer;
import org.limewire.swarm.SwarmerImpl;
import org.limewire.swarm.http.SwarmHttpSource;
import org.limewire.swarm.http.SwarmHttpSourceHandler;
import org.limewire.swarm.http.SwarmerImplTest;
import org.limewire.swarm.http.handler.SwarmFileExecutionHandler;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.BTMetaInfoFactory;
import com.limegroup.bittorrent.BTMetaInfoFactoryImpl;
import com.limegroup.bittorrent.BTMetaInfoTest;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.BTContext;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.bittorrent.disk.DiskManagerFactory;
import com.limegroup.bittorrent.disk.DiskManagerListener;
import com.limegroup.bittorrent.disk.TorrentDiskManager;
import com.limegroup.bittorrent.handshaking.piecestrategy.LargestGapStartPieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.PieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.RandomGapStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.RandomPieceStrategy;

public class BTSwarmCoordinatorTest extends BaseTestCase {

    public BTSwarmCoordinatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BTSwarmCoordinatorTest.class);
    }

    public void testSingleFileTorrent() throws Exception {

        File torrentFile = new File(BTMetaInfoTest.TEST_DATA_DIR
                + "/test-single-webseed-single-file-no-peer.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File downloadedFile = torrentFileSystem.getIncompleteFiles().get(0);
        downloadedFile.delete();
        final Swarmer swarmer = createSwarmer(torrentContext, null);

        swarmer.start();

        swarmer.addSource(new BTSwarmHttpSource(metaInfo));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile, 44425);

    }

    public void testMultiFileTorrent() throws Exception {

        File torrentFile = new File(BTMetaInfoTest.TEST_DATA_DIR
                + "/test-single-webseed-multiple-file-no-peer.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        File downloadedFile2 = torrentFileSystem.getIncompleteFiles().get(1);
        downloadedFile1.delete();
        downloadedFile2.delete();
        final Swarmer swarmer = createSwarmer(torrentContext, null);

        swarmer.start();

        swarmer.addSource(new BTSwarmHttpSource(metaInfo));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);
        SwarmerImplTest.assertDownload("db1dc452e77d30ce14acca6bac8c66bc", downloadedFile2, 411090);

    }

    public void testMultiFileTorrent2() throws Exception {

        File torrentFile = new File(BTMetaInfoTest.TEST_DATA_DIR
                + "/test-single-webseed-multiple-file-no-peer.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        File downloadedFile2 = torrentFileSystem.getIncompleteFiles().get(1);
        downloadedFile1.delete();
        downloadedFile2.delete();
        final Swarmer swarmer = createSwarmer(torrentContext, new RandomGapStrategy(metaInfo));

        swarmer.start();

        swarmer.addSource(new BTSwarmHttpSource(metaInfo));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);
        SwarmerImplTest.assertDownload("db1dc452e77d30ce14acca6bac8c66bc", downloadedFile2, 411090);

    }

    public void testMultiFileTorrent3() throws Exception {

        File torrentFile = new File(BTMetaInfoTest.TEST_DATA_DIR
                + "/test-single-webseed-multiple-file-no-peer.torrent");

        final BTMetaInfo metaInfo = createMetaInfo(torrentFile);
        final TorrentContext torrentContext = new BTContext(metaInfo, new DiskManagerFactory());
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File downloadedFile1 = torrentFileSystem.getIncompleteFiles().get(0);
        File downloadedFile2 = torrentFileSystem.getIncompleteFiles().get(1);
        downloadedFile1.delete();
        downloadedFile2.delete();
        final Swarmer swarmer = createSwarmer(torrentContext, new LargestGapStartPieceStrategy(
                metaInfo));

        swarmer.start();

        swarmer.addSource(new BTSwarmHttpSource(metaInfo));

        SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", downloadedFile1, 44425);
        SwarmerImplTest.assertDownload("db1dc452e77d30ce14acca6bac8c66bc", downloadedFile2, 411090);

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

        HttpParams params = SwarmerImplTest.createHttpParams();
        ConnectingIOReactor ioReactor = SwarmerImplTest.createIOReactor(params);
        final BTMetaInfo btMetaInfo = torrentContext.getMetaInfo();
        final BTSwarmCoordinator btCoordinator = new BTSwarmCoordinator(torrentContext
                .getMetaInfo(), torrentFileSystem, torrentDiskManager, pieceStrategy);
        btCoordinator.addListener(new LoggingSwarmCoordinatorListener());

        ConnectionReuseStrategy connectionReuseStrategy = new DefaultConnectionReuseStrategy();
        final Swarmer swarmer = new SwarmerImpl(new SourceEventListener() {

            public void connectFailed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
                System.out.println("connectFailed");

            }

            public void connected(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {
                System.out.println("connected");

            }

            public void connectionClosed(SwarmSourceHandler swarmSourceHandler, SwarmSource source) {

                System.out.println("connectionClosed");
                if (!btCoordinator.isComplete()) {
                    System.out.println("Adding swarm Source");
                    swarmSourceHandler.addSource(new BTSwarmHttpSource(btMetaInfo));
                }

            }

            public void responseProcessed(SwarmSourceHandler swarmSourceHandler,
                    SwarmSource source, SwarmStatus status) {
                System.out.println("responseProcessed");

            }

        });
        SwarmHttpSourceHandler httpSourceHandler = new SwarmHttpSourceHandler(btCoordinator,
                params, ioReactor, connectionReuseStrategy, null);
        swarmer.register(SwarmHttpSource.class, httpSourceHandler);
        swarmer.register(BTSwarmHttpSource.class, httpSourceHandler);
        return swarmer;
    }

    // TODO test better variety of torrent files.
    // TODO test larger torrent files.
}