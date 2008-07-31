package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;

import org.limewire.swarm.http.SwarmerImplTest;
import org.limewire.util.BaseTestCase;

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

        // tracker = new Tracker(3456);
        // tracker.start();
        // Runtime.getRuntime().addShutdownHook(new Thread() {
        // @Override
        // public void run() {
        // shutDownTracker();
        // }
        // });
        super.setUp();
    }

    // private void shutDownTracker() {
    // if (tracker != null) {
    // System.out.println("shutting down tracker.");
    // tracker.shutdown();
    // tracker = null;
    // }
    // }

    @Override
    protected void tearDown() throws Exception {
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(localIsPrivateBackup);
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(forceIPAddressBackup);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue(forceIPAddressStringBackup);
        // shutDownTracker();
        super.tearDown();
    }

    public void testBasic() throws Exception {
        String torrentfilePath = "/home/pvertenten/workspace/limewire/tests/test-data/gnutella_protocol_0.4.pdf.torrent";
        // Seeder seeder = new Seeder(torrentfilePath);
        try {
            // seeder.start();

            File torrentFile = new File(torrentfilePath);
            BTDownloader downloader = createBTDownloader(torrentFile);
            TorrentManager torrentManager = downloader.getTorrentManager();

            TorrentContext torrentContext = downloader.getTorrentContext();
            TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

            File incompleteFile = torrentFileSystem.getIncompleteFiles().get(0);
            incompleteFile.delete();
            File completeFile = torrentFileSystem.getCompleteFile();
            completeFile.delete();
            downloader.startDownload();
            Thread.sleep(5000);
            SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile, 44425);

        } finally {
            // seeder.shutdown();
        }
    }

    public void testMultipleFile() throws Exception {
        // TODO
        String torrentfilePath = "/home/pvertenten/workspace/limewire/tests/test-data/gnutella_protocol_0.4.pdf.torrent";
        // Seeder seeder = new Seeder(torrentfilePath);
        try {
            // seeder.start();

            File torrentFile = new File(torrentfilePath);
            BTDownloader downloader = createBTDownloader(torrentFile);
            TorrentManager torrentManager = downloader.getTorrentManager();

            TorrentContext torrentContext = downloader.getTorrentContext();
            TorrentFileSystem torrentFileSystem = torrentContext.getFileSystem();

            File incompleteFile = torrentFileSystem.getIncompleteFiles().get(0);
            incompleteFile.delete();
            File completeFile = torrentFileSystem.getCompleteFile();
            completeFile.delete();
            downloader.startDownload();
            Thread.sleep(5000);
            SwarmerImplTest.assertDownload("8055d620ba0c507c1af957b43648c99f", completeFile, 44425);

        } finally {
            // seeder.shutdown();
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

    // private static class Tracker extends Thread {
    // private static final String tempDir =
    // System.getProperty("java.io.tmpdir");
    //
    // private static final String trackerCommand = "/usr/bin/bttrack";
    //
    // private final int port;
    //
    // private final File trackerFile;
    //
    // private Process process = null;
    //
    // public Tracker(int port) {
    // this(port, tempDir + "/limetest/dfile.txt");
    // }
    //
    // public Tracker(int port, String trackerFilePath) {
    // this.port = port;
    // this.trackerFile = new File(trackerFilePath);
    // }
    //
    // public void run() {
    // Runtime runtime = Runtime.getRuntime();
    // trackerFile.mkdirs();
    // String trackerFilePath = trackerFile.getAbsolutePath();
    // String[] command = new String[] { trackerCommand, "--port", port + "",
    // "--dfile",
    // trackerFilePath };
    // try {
    // process = runtime.exec(command);
    // InputStream inputStream = process.getInputStream();
    // BufferedReader reader = new BufferedReader(new
    // InputStreamReader(inputStream));
    //
    // while (true) {
    // System.out.println("tracker: " + reader.readLine());
    // }
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }
    //
    // public void shutdown() {
    // try {
    // process.getOutputStream().write(CTRL_C);
    // Thread.sleep(1000);
    // process.destroy();
    // process.waitFor();
    // } catch (Exception e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }
    // }
    //
    // private static class Seeder extends Thread {
    // private static final String seederCommand = "/usr/bin/btdownloadcurses";
    //
    // private Process process = null;
    //
    // private final String torrentFilePath;
    //
    // public Seeder(String torrentFilePath) {
    // this.torrentFilePath = torrentFilePath;
    // }
    //
    // public void run() {
    // Runtime runtime = Runtime.getRuntime();
    // String[] command = new String[] { seederCommand, torrentFilePath,
    // "--spew", "1", "&>&1" };
    // try {
    // process = runtime.exec(command);
    // process.getErrorStream();
    // InputStream inputStream = process.getErrorStream();
    // BufferedReader reader = new BufferedReader(new
    // InputStreamReader(inputStream));
    //
    // while (true) {
    // System.out.println("seeder: " + reader.readLine());
    // }
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // }
    //
    // public void shutdown() {
    // process.destroy();
    // }
    //
    // }

}
