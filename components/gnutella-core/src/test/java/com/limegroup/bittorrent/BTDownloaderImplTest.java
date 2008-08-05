package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.limewire.swarm.http.SwarmerImplTest;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.bittorrent.swarm.BTSwarmCoordinatorTest;
import com.limegroup.gnutella.ActivityCallbackAdapter;
import com.limegroup.gnutella.LimeWireCoreModule;
import com.limegroup.gnutella.downloader.CoreDownloaderFactory;
import com.limegroup.gnutella.settings.ConnectionSettings;

/**
 * Test cases for the BTDownloader.
 * 
 * It takes a torrent file and downloads it, then checks the files contents
 * against an MD5.
 * 
 * Currently it assumes there is a tracker running on the localhost at port
 * 3456.
 * 
 * There needs to be a peer seeding the file at that tracker for the test to
 * work.
 * 
 */
public class BTDownloaderImplTest extends BaseTestCase {
    // private Tracker tracker;

    private boolean localIsPrivateBackup = false;

    private boolean forceIPAddressBackup = false;

    private String forceIPAddressStringBackup = null;

    public BTDownloaderImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        localIsPrivateBackup = ConnectionSettings.LOCAL_IS_PRIVATE.getValue();
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false);
        forceIPAddressBackup = ConnectionSettings.FORCE_IP_ADDRESS.getValue();
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        forceIPAddressStringBackup = ConnectionSettings.FORCED_IP_ADDRESS_STRING.getValue();
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue("127.0.0.1");
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(localIsPrivateBackup);
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(forceIPAddressBackup);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue(forceIPAddressStringBackup);
        super.tearDown();
    }

    public void testBasic() throws Exception {
        String torrentfilePath = "/home/pvertenten/workspace/limewire/tests/test-data/gnutella_protocol_0.4.pdf.torrent";
        File torrentFile = new File(torrentfilePath);
        BTDownloader downloader = createBTDownloader(torrentFile);

        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File completeFile = torrentFileSystem.getCompleteFile();
        try {

            File incompleteFile = torrentFileSystem.getIncompleteFiles().get(0);
            incompleteFile.delete();

            completeFile.delete();
            downloader.startDownload();
            finishDownload(downloader);
            SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile, 44425);

        } finally {
            if (completeFile != null) {
                completeFile.delete();
            }
        }
    }

    private void finishDownload(BTDownloader downloader) throws InterruptedException {
        int maxIterations = 30;
        int index = 0;
        while (!downloader.isCompleted()) {
            if (index++ > maxIterations) {
                Assert.fail("Failure downloading the file. Taking too long.");
            }
            Thread.sleep(1000);
        }
    }

    public void testMultipleFile() throws Exception {
        String torrentfilePath = "/home/pvertenten/workspace/limewire/tests/test-data/test.torrent";
        File torrentFile = new File(torrentfilePath);
        BTDownloader downloader = createBTDownloader(torrentFile);

        TorrentContext torrentContext = downloader.getTorrentContext();
        TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();
        File rootFile = torrentFileSystem.getCompleteFile();
        try {

            FileUtils.deleteRecursive(rootFile);
            File incompleteFile1 = torrentFileSystem.getIncompleteFiles().get(0);
            incompleteFile1.delete();
            File completeFile1 = torrentFileSystem.getFiles().get(0);
            completeFile1.delete();

            File incompleteFile2 = torrentFileSystem.getIncompleteFiles().get(1);
            incompleteFile2.delete();
            File completeFile2 = torrentFileSystem.getFiles().get(1);
            completeFile2.delete();
            downloader.startDownload();
            finishDownload(downloader);
            SwarmerImplTest
                    .assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile1, 44425);
            SwarmerImplTest.assertDownload("db1dc452e77d30ce14acca6bac8c66bc", completeFile2,
                    411090);
        } finally {
            if (rootFile != null) {
                rootFile.delete();
            }
        }
    }

    private BTDownloader createBTDownloader(File torrentFile) throws IOException {
        final BTMetaInfo metaInfo = BTSwarmCoordinatorTest.createMetaInfo(torrentFile);
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new LimeWireCoreModule(
                ActivityCallbackAdapter.class));

        CoreDownloaderFactory coreDownloaderFactory = injector
                .getInstance(CoreDownloaderFactory.class);
        BTDownloader downloader = coreDownloaderFactory.createBTDownloader(metaInfo);
        downloader.initBtMetaInfo(metaInfo);
        return downloader;
    }
}
