package org.limewire.swarm.http;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.apache.http.HttpStatus;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.nio.ByteBufferCacheImpl;
import org.limewire.nio.NIODispatcher;
import org.limewire.swarm.file.FileChannelSwarmFile;
import org.limewire.swarm.file.FileCoordinator;
import org.limewire.swarm.file.FileCoordinatorImpl;
import org.limewire.swarm.file.SwarmFile;
import org.limewire.swarm.http.handler.ExecutionHandler;
import org.limewire.swarm.http.handler.OrderedExecutionHandler;
import org.limewire.swarm.http.handler.SwarmFileExecutionHandler;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.downloader.swarm.HashTreeSwarmVerifier;
import com.limegroup.gnutella.downloader.swarm.ThexExecutionHandler;
import com.limegroup.gnutella.tigertree.HashTreeFactory;
import com.limegroup.gnutella.tigertree.HashTreeFactoryImpl;
import com.limegroup.gnutella.tigertree.SimpleHashTreeNodeManager;

public class SwarmerImplTest extends BaseTestCase {

    public SwarmerImplTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(SwarmerImplTest.class);
    }
    
    public void testSimpleSwarm() throws Exception {
        HttpParams params = new BasicHttpParams();
        params
            .setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
            .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setParameter(CoreProtocolPNames.USER_AGENT, "LimeTest/1.1");        
        ConnectingIOReactor ioReactor = new LimeConnectingIOReactor(params, NIODispatcher.instance().getScheduledExecutorService(), new SocketsManagerImpl());
      //  ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(1, params);
        
        SwarmFile swarmFile = new FileChannelSwarmFile(new File("C:/TEST-LimeWireWin.exe"));
        HashTreeSwarmVerifier swarmFileVerifier = new HashTreeSwarmVerifier(new ByteBufferCacheImpl());
        FileCoordinator fileCoordinator = new FileCoordinatorImpl(4506256L, swarmFile, swarmFileVerifier, ExecutorsHelper.newFixedSizeThreadPool(1, "Writer"));
        SwarmFileExecutionHandler swarmFileExecutionHandler = new SwarmFileExecutionHandler(fileCoordinator);
        HashTreeFactory hashTreeFactory = new HashTreeFactoryImpl(new SimpleHashTreeNodeManager());
        ThexExecutionHandler thexExecutionHandler = new ThexExecutionHandler(swarmFileVerifier, fileCoordinator, hashTreeFactory);
        thexExecutionHandler.setSha1("urn:sha1:PLPRTPBOARBOSAKPAMGVS2SL57S3GDLQ");
        
        ExecutionHandler executionHandler = new OrderedExecutionHandler(thexExecutionHandler, swarmFileExecutionHandler);
        
        final Swarmer swarmer = new SwarmerImpl(executionHandler, ioReactor, params, new Listener(fileCoordinator));
        swarmer.addHeaderInterceptor(thexExecutionHandler);
        
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            public void run() {
                swarmer.start();
                latch.countDown();
            }
        }).start();
        
        latch.await(1, TimeUnit.SECONDS);
        
        // Sources: 74.13.59.177:37394, 72.133.44.248:30670, 70.113.88.170:45045, 71.234.25.182:6384, 68.146.114.161:51676, 24.184.117.129:3838, 76.26.89.172:38889, 96.225.153.61:49052, 10.254.0.243:38143, 70.49.213.96:19645, 24.63.139.197:17501, 75.167.162.137:43092
        swarmer.addSource(new SourceImpl(new InetSocketAddress("74.13.59.177", 37394), "/uri-res/n2r?urn:sha1:PLPRTPBOARBOSAKPAMGVS2SL57S3GDLQ", true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("72.133.44.248", 30670), "/uri-res/n2r?urn:sha1:PLPRTPBOARBOSAKPAMGVS2SL57S3GDLQ", true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("70.113.88.170", 45045), "/uri-res/n2r?urn:sha1:PLPRTPBOARBOSAKPAMGVS2SL57S3GDLQ", true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("71.234.25.182", 6384), "/uri-res/n2r?urn:sha1:PLPRTPBOARBOSAKPAMGVS2SL57S3GDLQ", true), null);        
        swarmer.addSource(new SourceImpl(new InetSocketAddress("68.146.114.161", 51676), "/uri-res/n2r?urn:sha1:PLPRTPBOARBOSAKPAMGVS2SL57S3GDLQ", true), null);
//        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
//        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
//        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
//        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
//        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
//        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
        
        Thread.sleep(20000);
        
        swarmFile.finish();
    }
    
    private static class Listener implements SourceEventListener {
        private final FileCoordinator fileCoordinator;
        private boolean retry;
        
        public Listener(FileCoordinator fileCoordinator) {
            this.fileCoordinator = fileCoordinator;
        }

        public void connected(Swarmer swarmer, SwarmSource source) {
            retry = false;
            System.out.println("Connected to: " + source);
        }
        
        public void connectFailed(Swarmer swarmer, SwarmSource source) {
            retry = false;
            System.out.println("Failed connecting to: " + source);
        }
        
        public void connectionClosed(Swarmer swarmer, SwarmSource source) {
            if(retry && fileCoordinator.isRangeAvailableForLease()) {
                swarmer.addSource(source, null);
            }
            System.out.println("Closed connection to: " + source);
        }
        
        public void responseProcessed(Swarmer swarmer, SwarmSource source, int statusCode) {
            if(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_PARTIAL_CONTENT)
                retry = true;
            else
                retry = false;
        }
        
    }

}
