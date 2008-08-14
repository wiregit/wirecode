package org.limewire.swarm.http;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.collection.Range;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.http.util.FileServer;
import org.limewire.swarm.SwarmBlockSelector;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.SwarmSource;
import org.limewire.swarm.SwarmSourceType;
import org.limewire.swarm.Swarmer;
import org.limewire.swarm.file.FileCoordinatorImpl;
import org.limewire.swarm.file.SwarmFileImpl;
import org.limewire.swarm.file.SwarmFileSystemImpl;
import org.limewire.swarm.file.selection.ContiguousSelectionStrategy;
import org.limewire.swarm.file.verifier.MD5SumFileVerifier;
import org.limewire.swarm.file.verifier.RandomFailFileVerifier;
import org.limewire.swarm.impl.EchoSwarmCoordinatorListener;
import org.limewire.swarm.impl.SwarmerImpl;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

/**
 * 
 * 
 */
public class SwarmerImplTest extends BaseTestCase {
    private static final int TEST_PORT = 8080;

    /**
     * A directory containing the download data for this unit tests.
     */
    public static final File FILE_DIR = TestUtils
            .getResourceFile("test-data/bittorrent/public_html");

    private FileServer fileServer = null;

    public SwarmerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SwarmerImplTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        fileServer = new FileServer(TEST_PORT, FILE_DIR);
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

    public void runTest(TestRun testRun) throws Exception {
        for (int i = 0; i < 1; i++) {
            testRun.run();
        }

    }

    /**
     * Tests fully downloading a small file from 1 source.
     * 
     * @throws Exception
     */
    public void testBasic() throws Exception {
        runTest(new TestRun() {
            public void run() throws Exception {
                File file = createTestFile("gnutella_protocol_0.4.pdf");
                try {
                    String md5 = "8055d620ba0c507c1af957b43648c99f";

                    URI uri = new URI("http://localhost:" + TEST_PORT
                            + "/pub/gnutella_protocol_0.4.pdf");
                    int lowByte = 0;
                    int highByte = 44425 - 1;
                    long fileSize = 44425;
                    Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);
                    swarmer
                            .addSource(new SwarmHttpSource(uri, Range
                                    .createRange(lowByte, highByte)));
                    assertDownload(md5, file, fileSize);

                    System.out.println("downstream " + swarmer.getMeasuredBandwidth(true));
                    System.out.println("upstream " + swarmer.getMeasuredBandwidth(false));

                } finally {
                    file.delete();
                }
            }
        });

    }

    private File createTestFile(String fileName) {
        File file = new File(System.getProperty("java.io.tmpdir") + "/limetests/" + fileName);
        file.delete();
        file.deleteOnExit();
        file.mkdirs();
        return file;
    }

    /**
     * Tests downloading only the beginning of a file from 1 source.
     * 
     * @throws Exception
     */
    public void testRangesStart() throws Exception {
        runTest(new TestRun() {
            public void run() throws Exception {
                File file = createTestFile("testRangesStart.pdf");
                try {
                    String md5 = "cea47a73ebb7b0da41feef1d030a4c7a";
                    URI uri = new URI("http://localhost:" + TEST_PORT + "/pub/");
                    int lowByte = 0;
                    int highByte = (16 * 1024) - 1;
                    long fileSize = highByte + 1;
                    Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);
                    swarmer
                            .addSource(new SwarmHttpSource(uri, Range
                                    .createRange(lowByte, highByte)));

                    assertDownload(md5, file, fileSize);
                } finally {
                    file.delete();
                }
            }
        });
    }

    /**
     * Tests downloading only the middle of a file from 1 source.
     * 
     * @throws Exception
     */
    public void testRangesMiddle() throws Exception {
        runTest(new TestRun() {
            public void run() throws Exception {
                File file = createTestFile("testRangesMiddle.pdf");
                try {
                    String md5 = "bff2db0947dabf7978f55eebd7e3b2b4";
                    URI uri = new URI("http://localhost:" + TEST_PORT + "/pub/");
                    int lowByte = (16 * 1024);
                    int highByte = (2 * 16 * 1024) - 1;
                    long fileSize = highByte + 1;
                    Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);
                    swarmer
                            .addSource(new SwarmHttpSource(uri, Range
                                    .createRange(lowByte, highByte)));
                    assertDownload(md5, file, fileSize);
                } finally {
                    file.delete();
                }
            }
        });
    }

    /**
     * Tests downloading only the end of a file from 1 source.
     * 
     * @throws Exception
     */
    public void testRangesEnd() throws Exception {
        runTest(new TestRun() {
            public void run() throws Exception {
                File file = createTestFile("testRangesEnd.pdf");
                try {
                    String md5 = "c68ab8fbc3f712207774b33367d10f03";
                    URI uri = new URI("http://localhost:" + TEST_PORT + "/pub/");
                    int lowByte = (2 * 16 * 1024);
                    int highByte = 44425 - 1;
                    long fileSize = highByte + 1;
                    Swarmer swarmer = createSwarmer(file, "gnutella_protocol_0.4.pdf", fileSize);
                    swarmer
                            .addSource(new SwarmHttpSource(uri, Range
                                    .createRange(lowByte, highByte)));
                    assertDownload(md5, file, fileSize);
                } finally {
                    file.delete();
                }
            }
        });
    }

    /**
     * Tests downloading 1 file from multiple sources with differant ranges of
     * bytes.
     * 
     * @throws Exception
     */
    public void testMultipleRanges() throws Exception {
        runTest(new TestRun() {
            public void run() throws Exception {
                File file = createTestFile("testMultipleRanges.pdf");
                try {
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
                } finally {
                    file.delete();
                }
            }
        });
    }

    /**
     * Tests downloading 1 file from multiple sources with differant ranges of
     * bytes.
     * 
     * @throws Exception
     */
    public void testMultipleRanges2() throws Exception {
        runTest(new TestRun() {
            public void run() throws Exception {
                File file = createTestFile("testMultipleRanges2.pdf");
                try {
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
                } finally {
                    file.delete();
                }
            }
        });
    }

    public void testMultipleFiles() throws Exception {
        long fileSize1 = 44425;
        long fileSize2 = 18;
        File file1 = createTestFile("/testMultipleFiles.pdf");
        File file2 = createTestFile("/testMultipleFiles.txt");
        String md51 = "8055d620ba0c507c1af957b43648c99f";
        String md52 = "b534f966248fde84b5a7bbb399db1c3d";
        URI uri = new URI("http://localhost:" + TEST_PORT + "/pub/");

        Range range1 = Range.createRange(0, fileSize1 - 1);
        Range range2 = Range.createRange(fileSize1, fileSize1 + fileSize2 - 1);
        final SwarmSource swarmSource = new SwarmHttpSource(uri, (fileSize1 + fileSize2));
        MD5SumFileVerifier swarmBlockVerifier = new MD5SumFileVerifier();
        swarmBlockVerifier.addMD5Check(range1, md51);
        swarmBlockVerifier.addMD5Check(range2, md52);
        file1.delete();
        file2.delete();

        SwarmFileSystemImpl swarmfilesystem = new SwarmFileSystemImpl();
        SwarmFileImpl swarmFile1 = new SwarmFileImpl(file1, "gnutella_protocol_0.4.pdf", fileSize1);
        swarmfilesystem.addSwarmFile(swarmFile1);
        SwarmFileImpl swarmFile2 = new SwarmFileImpl(file2, "hi.txt", fileSize2);
        swarmfilesystem.addSwarmFile(swarmFile2);
        SwarmBlockSelector selectionStrategy = new ContiguousSelectionStrategy();
        SwarmCoordinator swarmCoordinator = new FileCoordinatorImpl(swarmfilesystem,
                swarmBlockVerifier, ExecutorsHelper.newFixedSizeThreadPool(1, "Writer"),
                selectionStrategy, 32 * 1024);
        swarmCoordinator.addListener(new EchoSwarmCoordinatorListener());

        Swarmer swarmer = new SwarmerImpl(swarmCoordinator);
        swarmer.register(SwarmSourceType.HTTP, new SwarmHttpSourceHandler(swarmCoordinator,
                "LimeTest/1.1"));
        swarmer.start();

        swarmer.addSource(swarmSource);

        Thread.sleep(3000);

        Assert.assertTrue(file1.exists());
        Assert.assertEquals(fileSize1, file1.length());
        String testmd5 = FileUtils.getMD5(file1);

        Assert.assertEquals(md51, testmd5);

        Assert.assertTrue(file2.exists());
        Assert.assertEquals(fileSize2, file2.length());
        testmd5 = FileUtils.getMD5(file2);
        Assert.assertEquals(md52, testmd5);
    }

    /**
     * Tests fully downloading a small file from 1 source.
     * 
     * @throws Exception
     */
    public void testSimpleSmallFileSwarm() throws Exception {
        runTest(new TestRun() {
            public void run() throws Exception {
                File file = createTestFile("testSimpleSmallFileSwarm.pdf");
                try {
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
                } finally {
                    file.delete();
                }
            }
        });
    }

    // public void testSimpleSmallFileSwarm2() throws Exception {
    // runTest(new TestRun() {
    // public void run() throws Exception {
    // File file = createTestFile("testSimpleSmallFileSwarm2.pdf");
    // try {
    // String md5 = "4eb024f612ab7391d90dcf294807ad4b";
    //
    // URI uri1 = new URI("http://localhost:" + TEST_PORT + "/pub2/");
    // int lowByte = 0;
    // int highByte = 10252982 - 1;
    // long fileSize = highByte + 1;
    // Range range = Range.createRange(lowByte, highByte);
    // Swarmer swarmer = createSwarmer(file, "LimeWireLinux.deb", fileSize,
    // new MD5SumFileVerifier(range, md5));
    // swarmer.addSource(new SwarmHttpSource(uri1, range));
    // Thread.sleep(20000);
    // assertDownload(md5, file, fileSize);
    // } finally {
    // file.delete();
    // }
    // }
    // });
    // }

    // public void testMediumFileSwarm() throws Exception {
    // runTest(new TestRun() {
    // public void run() throws Exception {
    // File file = createTestFile("testMediumFileSwarm.pdf");
    // try {
    // String md5 = "9efd0799bec290d89745f3b479e3fec8";
    //
    // URI uri1 = new URI(
    // "http://ftp.belnet.be/packages/damnsmalllinux/current/dsl-4.4.3-initrd.iso"
    // );
    // int lowByte = 0;
    // int highByte = 10252982 - 1;
    // long fileSize = 100000000;
    // Range range = Range.createRange(lowByte, highByte);
    // Swarmer swarmer = createSwarmer(file, "dsl-4.4.3-initrd.iso", fileSize,
    // new MD5SumFileVerifier(range, md5));
    // swarmer.addSource(new SwarmHttpSource(uri1, range));
    // assertDownload(md5, file, fileSize);
    // } finally {
    // file.delete();
    // }
    // }
    // });
    // }

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

        SwarmCoordinator swarmCoordinator = createSwarmCoordinator(file, path, fileSize,
                swarmFileVerifier);

        swarmCoordinator.addListener(new EchoSwarmCoordinatorListener());

        Swarmer swarmer = new SwarmerImpl(swarmCoordinator);
        swarmer.register(SwarmSourceType.HTTP, new SwarmHttpSourceHandler(swarmCoordinator,
                "LimeTest/1.1"));
        swarmer.getMeasuredBandwidth(true);
        swarmer.getMeasuredBandwidth(false);

        swarmer.start();
        return swarmer;
    }

    private SwarmCoordinator createSwarmCoordinator(File file, String path, long fileSize,
            SwarmBlockVerifier swarmFileVerifier) {
        SwarmFileSystem swarmfilesystem = new SwarmFileSystemImpl(new SwarmFileImpl(file, path,
                fileSize));

        SwarmBlockSelector selectionStrategy = new ContiguousSelectionStrategy();

        SwarmCoordinator swarmCoordinator = new FileCoordinatorImpl(swarmfilesystem,
                swarmFileVerifier, ExecutorsHelper.newFixedSizeThreadPool(1, "Writer"),
                selectionStrategy, 10000000);
        return swarmCoordinator;
    }

    // TODO test larger files
    // TODO test better variety of files.

    private abstract class TestRun {
        public abstract void run() throws Exception;
    }
}
