package org.limewire.swarm.http;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;

import junit.framework.Assert;
import junit.framework.Test;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.limewire.collection.Range;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.nio.NIODispatcher;
import org.limewire.swarm.LoggingSwarmCoordinatorListener;
import org.limewire.swarm.SwarmBlockSelector;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.Swarmer;
import org.limewire.swarm.SwarmerImpl;
import org.limewire.swarm.file.FileCoordinatorImpl;
import org.limewire.swarm.file.SwarmFileImpl;
import org.limewire.swarm.file.SwarmFileSystemImpl;
import org.limewire.swarm.file.selection.ContiguousSelectionStrategy;
import org.limewire.swarm.file.verifier.MD5SumFileVerifier;
import org.limewire.swarm.file.verifier.RandomFailFileVerifier;
import org.limewire.swarm.http.handler.SwarmFileExecutionHandler;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.BTMetaInfoTest;
import com.limegroup.gnutella.util.FileServer;

/**
 * 
 * 
 */
public class SwarmerImplTest extends BaseTestCase {
    private static final int TEST_PORT = 8080;

    private FileServer fileServer = null;

    public SwarmerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SwarmerImplTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        fileServer = new FileServer(TEST_PORT, new File(BTMetaInfoTest.TEST_DATA_DIR
                + "/public_html"));
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

    /**
     * Tests fully downloading a small file from 1 source.
     * 
     * @throws Exception
     */
    public void testBasic() throws Exception {

        File file = createTestFile("gnutella_protocol_0.4.pdf");
        String md5 = "8055d620ba0c507c1af957b43648c99f";

        URI uri = new URI("http://localhost:" + TEST_PORT + "/pub/gnutella_protocol_0.4.pdf");
        int lowByte = 0;
        int highByte = 44425 - 1;
        long fileSize = 44425;
        Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);
        swarmer.addSource(new SwarmHttpSource(uri, Range.createRange(lowByte, highByte)));
        assertDownload(md5, file, fileSize);

        file.delete();

    }

    private File createTestFile(String fileName) {
        File file = new File(System.getProperty("java.io.tmpdir") + "/limetests/" + fileName);
        file.delete();
        file.mkdirs();
        return file;
    }

    /**
     * Tests downloading only the beginning of a file from 1 source.
     * 
     * @throws Exception
     */
    public void testRangesStart() throws Exception {
        File file = createTestFile("testRangesStart.pdf");

        String md5 = "cea47a73ebb7b0da41feef1d030a4c7a";
        URI uri = new URI("http://localhost:" + TEST_PORT + "/pub/");
        int lowByte = 0;
        int highByte = (16 * 1024) - 1;
        long fileSize = highByte + 1;
        Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);
        swarmer.addSource(new SwarmHttpSource(uri, Range.createRange(lowByte, highByte)));
        assertDownload(md5, file, fileSize);

        file.delete();
    }

    /**
     * Tests downloading only the middle of a file from 1 source.
     * 
     * @throws Exception
     */
    public void testRangesMiddle() throws Exception {
        File file = createTestFile("testRangesMiddle.pdf");

        String md5 = "bff2db0947dabf7978f55eebd7e3b2b4";
        URI uri = new URI("http://localhost:" + TEST_PORT + "/pub/");
        int lowByte = (16 * 1024);
        int highByte = (2 * 16 * 1024) - 1;
        long fileSize = highByte + 1;
        Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);
        swarmer.addSource(new SwarmHttpSource(uri, Range.createRange(lowByte, highByte)));
        assertDownload(md5, file, fileSize);

        file.delete();

    }

    /**
     * Tests downloading only the end of a file from 1 source.
     * 
     * @throws Exception
     */
    public void testRangesEnd() throws Exception {
        File file = createTestFile("testRangesEnd.pdf");

        String md5 = "c68ab8fbc3f712207774b33367d10f03";
        URI uri = new URI("http://localhost:" + TEST_PORT + "/pub/");
        int lowByte = (2 * 16 * 1024);
        int highByte = 44425 - 1;
        long fileSize = highByte + 1;
        Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);
        swarmer.addSource(new SwarmHttpSource(uri, Range.createRange(lowByte, highByte)));
        assertDownload(md5, file, fileSize);

        file.delete();

    }

    /**
     * Tests downloading 1 file from multiple sources with differant ranges of
     * bytes.
     * 
     * @throws Exception
     */
    public void testMultipleRanges() throws Exception {
        File file = createTestFile("testMultipleRanges.pdf");

        String md5 = "8055d620ba0c507c1af957b43648c99f";
        URI uri = new URI("http://localhost:" + TEST_PORT + "/pub/");
        URI uri2 = new URI("http://www9.limewire.com/developer/");
        int lowByte1 = 0;
        int highByte1 = (2 * 16 * 1024) - 1;
        int lowByte2 = highByte1 + 1;
        int highByte2 = 44425 - 1;
        long fileSize = highByte2 + 1;
        Range range1 = Range.createRange(lowByte1, highByte1);
        Range range2 = Range.createRange(lowByte2, highByte2);

        Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);

        swarmer.addSource(new SwarmHttpSource(uri, range2));
        swarmer.addSource(new SwarmHttpSource(uri2, range1));

        assertDownload(md5, file, fileSize);

        file.delete();

    }

    /**
     * Tests downloading 1 file from multiple sources with differant ranges of
     * bytes.
     * 
     * @throws Exception
     */
    public void testMultipleRanges2() throws Exception {
        File file = createTestFile("testMultipleRanges2.pdf");

        String md5 = "8055d620ba0c507c1af957b43648c99f";

        URI uri1 = new URI("http://localhost:" + TEST_PORT + "/pub/");
        URI uri2 = new URI("http://localhost:" + TEST_PORT + "/pub2/");
        URI uri3 = new URI("http://www9.limewire.com/developer/");

        int lowByte1 = 0;
        int highByte1 = (1 * 16 * 1024) - 1;
        int lowByte2 = highByte1 + 1;
        int highByte2 = (2 * 16 * 1024) - 1;
        int lowByte3 = highByte2 + 1;
        int highByte3 = 44425 - 1;

        long fileSize = highByte3 + 1;
        Range range1 = Range.createRange(lowByte1, highByte1);
        Range range2 = Range.createRange(lowByte2, highByte2);
        Range range3 = Range.createRange(lowByte3, highByte3);
        Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);

        swarmer.addSource(new SwarmHttpSource(uri2, range2));
        swarmer.addSource(new SwarmHttpSource(uri1, range1));
        swarmer.addSource(new SwarmHttpSource(uri3, range3));
        assertDownload(md5, file, fileSize);

        file.delete();

    }

    /**
     * Tests fully downloading a small file from 1 source.
     * 
     * @throws Exception
     */
    public void testSimpleSmallFileSwarm() throws Exception {

        File file = createTestFile("testSimpleSmallFileSwarm.pdf");
        String md5 = "8055d620ba0c507c1af957b43648c99f";

        URI uri1 = new URI("http://localhost:" + TEST_PORT + "/pub/");
        int lowByte = 0;
        int highByte = 44425 - 1;
        long fileSize = highByte + 1;
        Range range = Range.createRange(lowByte, highByte);
        Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize,
                new MD5SumFileVerifier(range, md5));
        swarmer.addSource(new SwarmHttpSource(uri1, range));
        assertDownload(md5, file, fileSize);

        file.delete();
    }

    /**
     * Asserts that the given file has the correct size, and matches the given
     * md5sum.
     * 
     * @param md5
     * @param file
     * @param fileSize
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static void assertDownload(String md5, File file, long fileSize)
            throws InterruptedException, NoSuchAlgorithmException, IOException {
        long sleepTime = (long) ((fileSize * 0.0001) + 3000);
        Thread.sleep(sleepTime);
        Assert.assertTrue(file.exists());
        Assert.assertEquals(fileSize, file.length());
        String testmd5 = FileUtils.getMD5(file);
        Assert.assertEquals(md5, testmd5);
    }

    /**
     * Creates a swarmerImpl for the given file using a default block verifier.
     * 
     * @param file
     * @param path
     * @param fileSize
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private Swarmer createSwarmer(File file, String path, long fileSize)
            throws InterruptedException, IOException {
        return createSwarmer(file, path, fileSize, new RandomFailFileVerifier());
    }

    /**
     * Creates a swarmerImpl for the given file and block verifier.
     * 
     * @param file
     * @param path
     * @param fileSize
     * @param swarmFileVerifier
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private Swarmer createSwarmer(File file, String path, long fileSize,
            SwarmBlockVerifier swarmFileVerifier) throws InterruptedException, IOException {
        System.out.println("-----------------------------------");
        file.delete();

        HttpParams params = createHttpParams();
        ConnectingIOReactor ioReactor = createIOReactor(params);
        SwarmCoordinator swarmCoordinator = createSwarmCoordinator(file, path, fileSize,
                swarmFileVerifier);

        ConnectionReuseStrategy connectionReuseStrategy = new DefaultConnectionReuseStrategy();
        final Swarmer swarmer = new SwarmerImpl();

        SwarmHttpSourceHandler httpSourceHandler = new SwarmHttpSourceHandler(swarmCoordinator,
                params, ioReactor, connectionReuseStrategy, null);
        swarmer.register(SwarmHttpSource.class, httpSourceHandler);

        swarmCoordinator.addListener(new LoggingSwarmCoordinatorListener());

        swarmer.start();
        return swarmer;
    }

    public static ConnectingIOReactor createIOReactor(HttpParams params) {
        ConnectingIOReactor ioReactor = new LimeConnectingIOReactor(params, NIODispatcher
                .instance().getScheduledExecutorService(), new SocketsManagerImpl());
        return ioReactor;
    }

    public static HttpParams createHttpParams() {
        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(
                CoreConnectionPNames.CONNECTION_TIMEOUT, 2000).setIntParameter(
                CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024).setBooleanParameter(
                CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setParameter(
                CoreProtocolPNames.USER_AGENT, "LimeTest/1.1");
        return params;
    }

    private SwarmCoordinator createSwarmCoordinator(File file, String path, long fileSize,
            SwarmBlockVerifier swarmFileVerifier) {
        SwarmFileSystem swarmfilesystem = new SwarmFileSystemImpl(new SwarmFileImpl(file, path,
                fileSize));

        SwarmBlockSelector selectionStrategy = new ContiguousSelectionStrategy();

        SwarmCoordinator swarmCoordinator = new FileCoordinatorImpl(swarmfilesystem,
                swarmFileVerifier, ExecutorsHelper.newFixedSizeThreadPool(1, "Writer"),
                selectionStrategy);
        return swarmCoordinator;
    }

    // TODO test larger files
    // TODO test better variety of files.
}
