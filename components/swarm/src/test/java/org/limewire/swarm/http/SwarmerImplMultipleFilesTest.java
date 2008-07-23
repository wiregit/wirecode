package org.limewire.swarm.http;

import java.io.File;
import java.net.URI;

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
import org.limewire.swarm.SwarmBlockSelector;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmFileSystem;
import org.limewire.swarm.SwarmListener;
import org.limewire.swarm.SwarmBlockVerifier;
import org.limewire.swarm.file.FileCoordinatorImpl;
import org.limewire.swarm.file.SwarmFile;
import org.limewire.swarm.file.SwarmFileImpl;
import org.limewire.swarm.file.SwarmFileSystemImpl;
import org.limewire.swarm.file.selection.ContiguousSelectionStrategy;
import org.limewire.swarm.file.verifier.NoOpFileVerifier;
import org.limewire.swarm.http.handler.SwarmFileExecutionHandler;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

public class SwarmerImplMultipleFilesTest extends BaseTestCase {

    public SwarmerImplMultipleFilesTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SwarmerImplMultipleFilesTest.class);
    }

    public void test2() throws Exception {
        long fileSize1 = 44425;
        long fileSize2 = 18;
        File file1 = new File(System.getProperty("java.io.tmpdir") + "/test2.pdf");
        File file2 = new File(System.getProperty("java.io.tmpdir") + "/test2.txt");
        
        URI uri = new URI("http://localhost/~pvertenten/pub/");
        final  SwarmSource swarmSource = new SourceImpl(uri, (fileSize1 + fileSize2));
        final Swarmer swarmer = createSwarmer(fileSize1, fileSize2, file1, file2, swarmSource);

        swarmer.start();

        swarmer.addSource(swarmSource);

        Thread.sleep(3000);

        Assert.assertTrue(file1.exists());
        Assert.assertEquals(fileSize1, file1.length());
        String testmd5 = FileUtils.getMD5(file1);
        String md5 = "8055d620ba0c507c1af957b43648c99f";
        Assert.assertEquals(md5, testmd5);

        Assert.assertTrue(file2.exists());
        Assert.assertEquals(fileSize2, file2.length());
        testmd5 = FileUtils.getMD5(file2);
        Assert.assertEquals("b534f966248fde84b5a7bbb399db1c3d", testmd5);
    }

    private Swarmer createSwarmer(long fileSize1, long fileSize2, File file1, File file2,
            final SwarmSource swarmSource) {
        file1.delete();

        

        
        file2.delete();

        

        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(
                CoreConnectionPNames.CONNECTION_TIMEOUT, 2000).setIntParameter(
                CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024).setBooleanParameter(
                CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setParameter(
                CoreProtocolPNames.USER_AGENT, "LimeTest/1.1");
        ConnectingIOReactor ioReactor = new LimeConnectingIOReactor(params, NIODispatcher
                .instance().getScheduledExecutorService(), new SocketsManagerImpl());

        SwarmFileSystemImpl swarmfilesystem = new SwarmFileSystemImpl();

        SwarmFileImpl swarmFile1 = new SwarmFileImpl(file1, "gnutella_protocol_0.4.pdf", fileSize1);
        swarmfilesystem.add(swarmFile1);

        SwarmFileImpl swarmFile2 = new SwarmFileImpl(file2, "hi.txt", fileSize2);
        swarmfilesystem.add(swarmFile2);

        SwarmBlockVerifier swarmFileVerifier = new NoOpFileVerifier();
        SwarmBlockSelector selectionStrategy = new ContiguousSelectionStrategy();

        SwarmCoordinator swarmCoordinator = new FileCoordinatorImpl(swarmfilesystem,
                swarmFileVerifier, ExecutorsHelper.newFixedSizeThreadPool(1, "Writer"),
                selectionStrategy, 32 * 1024);

        SwarmFileExecutionHandler executionHandler = new SwarmFileExecutionHandler(swarmCoordinator);
        ConnectionReuseStrategy connectionReuseStrategy = new DefaultConnectionReuseStrategy();
        
       
        final Swarmer swarmer = new SwarmerImpl(executionHandler, connectionReuseStrategy,
                ioReactor, params, new SourceEventListener()
        {

            public void connectFailed(Swarmer swarmer, SwarmSource source) {
                System.out.println("connect failed");
                
                
            }

            public void connected(Swarmer swarmer, SwarmSource source) {
                System.out.println("connectted");
                
            }

            public void connectionClosed(Swarmer swarmer, SwarmSource source) {
                System.out.println("connectionClosed");
              //  swarmer.addSource(swarmSource);
            }

            public void responseProcessed(Swarmer swarmer, SwarmSource source, int statusCode) {
                System.out.println("responseProcessed");
                
            }
            
        
        });

        swarmCoordinator.addListener(new SwarmListener() {

            public void blockLeased(SwarmCoordinator swarmCoordinator, Range block) {
                System.out.println("block leased: " + block.toString());

            }

            public void blockVerificationFailed(SwarmCoordinator swarmCoordinator, Range block) {
                System.out.println("block verification failed: " + block.toString());

            }

            public void blockVerified(SwarmCoordinator swarmCoordinator, Range block) {
                System.out.println("block verified: " + block.toString());

            }

            public void blockWritten(SwarmCoordinator swarmCoordinator, Range block) {
                System.out.println("block written: " + block.toString());
            }

            public void blockUnleased(SwarmCoordinator swarmCoordinator, Range block) {
                System.out.println("block unleased: " + block.toString());

            }

            public void downloadCompleted(SwarmCoordinator fileCoordinator,
                    SwarmFileSystem swarmDownload) {
                System.out.println("download complete");
            }

            public void blockPending(SwarmCoordinator swarmCoordinator, Range block) {
                System.out.println("block pending: " + block.toString());

            }

            public void blockUnpending(SwarmCoordinator swarmCoordinator, Range block) {
                System.out.println("block unpending: " + block.toString());

            }

        });
        return swarmer;
    }
}
