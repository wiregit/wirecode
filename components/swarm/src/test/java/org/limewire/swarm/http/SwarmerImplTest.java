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
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.SwarmBlockSelector;
import org.limewire.swarm.SwarmVerifier;
import org.limewire.swarm.file.FileChannelSwarmFileSystem;
import org.limewire.swarm.file.FileCoordinatorImpl;
import org.limewire.swarm.file.selection.ContiguousSelectionStrategy;
import org.limewire.swarm.file.verifier.NoOpFileVerifier;
import org.limewire.swarm.http.handler.SwarmFileExecutionHandler;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

public class SwarmerImplTest extends BaseTestCase {

    public SwarmerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SwarmerImplTest.class);
    }

    public void testRangesStart() throws Exception {
        String md5 = "cea47a73ebb7b0da41feef1d030a4c7a";
        File file = new File(System.getProperty("java.io.tmpdir") + "/testRangesStart.pdf");
        URI uri = new URI("http://localhost/~pvertenten/pub/gnutella_protocol_0.4.pdf");
        int lowByte = 0;
        int highByte = (16 * 1024) - 1;
        long fileSize = highByte + 1;
        Swarmer swarmer = createSwarmer(file, fileSize);
        swarmer.addSource(new SourceImpl(uri, Range.createRange(lowByte, highByte)));
        assertSwarmer(md5, file, fileSize);
    }

    public void testRangesMiddle() throws Exception {
        String md5 = "bff2db0947dabf7978f55eebd7e3b2b4";
        File file = new File(System.getProperty("java.io.tmpdir") + "/testRangesMiddle.pdf");
        URI uri = new URI("http://localhost/~pvertenten/pub/gnutella_protocol_0.4.pdf");
        int lowByte = (16 * 1024);
        int highByte = (2 * 16 * 1024) - 1;
        long fileSize = highByte + 1;
        Swarmer swarmer = createSwarmer(file, fileSize);
        swarmer.addSource(new SourceImpl(uri, Range.createRange(lowByte, highByte)));
        assertSwarmer(md5, file, fileSize);
    }

    public void testRangesEnd() throws Exception {
        String md5 = "c68ab8fbc3f712207774b33367d10f03";
        File file = new File(System.getProperty("java.io.tmpdir") + "/testRangesEnd.pdf");
        URI uri = new URI("http://localhost/~pvertenten/pub/gnutella_protocol_0.4.pdf");
        int lowByte = (2 * 16 * 1024);
        int highByte = 44425 - 1;
        long fileSize = highByte + 1;
        Swarmer swarmer = createSwarmer(file, fileSize);
        swarmer.addSource(new SourceImpl(uri, Range.createRange(lowByte, highByte)));
        assertSwarmer(md5, file, fileSize);
    }

    public void testMultipleRanges() throws Exception {
        String md5 = "8055d620ba0c507c1af957b43648c99f";
        File file = new File(System.getProperty("java.io.tmpdir") + "/testMultipleRanges.pdf");
        URI uri = new URI("http://localhost/~pvertenten/pub/gnutella_protocol_0.4.pdf");
        int lowByte1 = 0;
        int highByte1 = (2 * 16 * 1024) - 1;
        int lowByte2 = highByte1+1;
        int highByte2 = 44425 - 1;
        long fileSize = highByte2 + 1;
        Range range1 = Range.createRange(lowByte1, highByte1);
        Range range2 = Range.createRange(lowByte2, highByte2);
        
        Swarmer swarmer = createSwarmer(file, fileSize);

        
        swarmer.addSource(new SourceImpl(uri, range2));
        swarmer.addSource(new SourceImpl(uri, range1));
        assertSwarmer(md5, file, fileSize);
    }
    
    public void testMultipleRanges2() throws Exception {
        String md5 = "8055d620ba0c507c1af957b43648c99f";
        File file = new File(System.getProperty("java.io.tmpdir") + "/testMultipleRanges2.pdf");
        URI uri = new URI("http://localhost/~pvertenten/pub/gnutella_protocol_0.4.pdf");
        URI uri2 = new URI("http://localhost/~pvertenten/pub2/gnutella_protocol_0.4.pdf");
        URI uri3 = new URI("http://www9.limewire.com/developer/gnutella_protocol_0.4.pdf");
        
        int lowByte1 = 0;
        int highByte1 = (1 * 16 * 1024) - 1;
        int lowByte2 = highByte1+1;
        int highByte2 = (2 * 16 * 1024) - 1;
        int lowByte3 = highByte2+1;
        int highByte3 = 44425 - 1;
        
        long fileSize = highByte3 + 1;
        Range range1 = Range.createRange(lowByte1, highByte1);
        Range range2 = Range.createRange(lowByte2, highByte2);
        Range range3 = Range.createRange(lowByte3, highByte3);
        Swarmer swarmer = createSwarmer(file, fileSize);

        
        swarmer.addSource(new SourceImpl(uri2, range2));
        swarmer.addSource(new SourceImpl(uri, range1));
        swarmer.addSource(new SourceImpl(uri3, range3));
        assertSwarmer(md5, file, fileSize);
    }

    public void testSimpleSmallFileSwarm() throws Exception {
        String md5 = "8055d620ba0c507c1af957b43648c99f";
        File file = new File(System.getProperty("java.io.tmpdir") + "/testSimpleSmallFileSwarm.pdf");
        URI uri = new URI("http://localhost/~pvertenten/pub/gnutella_protocol_0.4.pdf");
        int lowByte = 0;
        int highByte = 44425 - 1;
        long fileSize = highByte + 1;
        Swarmer swarmer = createSwarmer(file, fileSize);
        swarmer.addSource(new SourceImpl(uri, Range.createRange(lowByte, highByte)));
        assertSwarmer(md5, file, fileSize);
    }

    public void testSimpleLargeFileSwarm() throws Exception {
        String md5 = "1ba0b5a6e992d6d0461c1e34cb405e34";
        File file = new File(System.getProperty("java.io.tmpdir") + "/testSimpleLargeFileSwarm.iso");
        URI uri = new URI("http://localhost/~pvertenten/pub/debian-40r3-ia64-CD-1.iso");
        int lowByte = 0;
        int highByte = 679000064 - 1;
        long fileSize = highByte + 1;
        Swarmer swarmer = createSwarmer(file, fileSize);
        swarmer.addSource(new SourceImpl(uri, Range.createRange(lowByte, highByte)));
        assertSwarmer(md5, file, fileSize);
    }

    private void assertSwarmer(String md5, File file, long fileSize) throws InterruptedException,
            NoSuchAlgorithmException, IOException {
        long sleepTime = (long) ((fileSize * 0.0001) + 5000);
        Thread.sleep(sleepTime);
        Assert.assertTrue(file.exists());
        Assert.assertEquals(fileSize, file.length());
        String testmd5 = FileUtils.getMD5(file);
        Assert.assertEquals(md5, testmd5);
    }

    private Swarmer createSwarmer(File file, long fileSize) throws InterruptedException,
            IOException {
        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(
                CoreConnectionPNames.CONNECTION_TIMEOUT, 2000).setIntParameter(
                CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024).setBooleanParameter(
                CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setParameter(
                CoreProtocolPNames.USER_AGENT, "LimeTest/1.1");
        ConnectingIOReactor ioReactor = new LimeConnectingIOReactor(params, NIODispatcher
                .instance().getScheduledExecutorService(), new SocketsManagerImpl());

        file.delete();
        SwarmFileSystem swarmDownload = new FileChannelSwarmFileSystem(fileSize, file);
        SwarmVerifier swarmFileVerifier = new NoOpFileVerifier();
        SwarmBlockSelector selectionStrategy = new ContiguousSelectionStrategy();

        SwarmCoordinator swarmCoordinator = new FileCoordinatorImpl(swarmDownload,
                swarmFileVerifier, ExecutorsHelper.newFixedSizeThreadPool(1, "Writer"),
                selectionStrategy);

        SwarmFileExecutionHandler executionHandler = new SwarmFileExecutionHandler(swarmCoordinator);
        ConnectionReuseStrategy connectionReuseStrategy = new DefaultConnectionReuseStrategy();
        final Swarmer swarmer = new SwarmerImpl(executionHandler, connectionReuseStrategy,
                ioReactor, params, null);
        swarmer.start();
        return swarmer;
    }
}
