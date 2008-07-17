package org.limewire.swarm.http;

import java.io.File;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.Test;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpStatus;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.nio.NIODispatcher;
import org.limewire.swarm.SwarmCoordinator;
import org.limewire.swarm.SwarmDownload;
import org.limewire.swarm.SwarmSelector;
import org.limewire.swarm.SwarmVerifier;
import org.limewire.swarm.file.FileChannelSwarmFile;
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

    public void testSimpleSwarm() throws Exception {
        String testURI = "http://www.limewire.org/lw-bt-testfiles";
        
        long fileSize = 44425;
        HttpParams params = new BasicHttpParams();
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000).setIntParameter(
                CoreConnectionPNames.CONNECTION_TIMEOUT, 2000).setIntParameter(
                CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024).setBooleanParameter(
                CoreConnectionPNames.STALE_CONNECTION_CHECK, false).setParameter(
                CoreProtocolPNames.USER_AGENT, "LimeTest/1.1");
        ConnectingIOReactor ioReactor = new LimeConnectingIOReactor(params, NIODispatcher
                .instance().getScheduledExecutorService(), new SocketsManagerImpl());
        // ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(1,
        // params);

        File file = new File(System.getProperty("java.io.tmpdir") + "/testSimpleSwarm.pdf");
        file.delete();
        
        SwarmDownload swarmDownload = new FileChannelSwarmFile(file);
        SwarmVerifier swarmFileVerifier = new NoOpFileVerifier();
        SwarmSelector selectionStrategy = new ContiguousSelectionStrategy();
        
        
        SwarmCoordinator swarmCoordinator = new FileCoordinatorImpl(fileSize, swarmDownload,
                swarmFileVerifier, ExecutorsHelper.newFixedSizeThreadPool(1, "Writer"),
                selectionStrategy);

        SwarmFileExecutionHandler executionHandler = new SwarmFileExecutionHandler(swarmCoordinator);
        ConnectionReuseStrategy connectionReuseStrategy = new DefaultConnectionReuseStrategy();
        final Swarmer swarmer = new SwarmerImpl(executionHandler, connectionReuseStrategy,
                ioReactor, params, null);

        swarmer.start();
        
        URI uri = new URI("http://localhost/~pvertenten/pub/gnutella_protocol_0.4.pdf");
        swarmer.addSource(new SourceImpl(uri, true), null);

        Thread.sleep(20000);

        swarmDownload.finish();
        
        
        Assert.assertTrue(file.exists());
        Assert.assertEquals(fileSize, file.length());
        
        String md5 = "8055d620ba0c507c1af957b43648c99f";
        String testmd5 = FileUtils.getMD5(file);
        Assert.assertEquals(md5, testmd5);
        
        
   
    }
}
