package org.limewire.swarm.http;

import java.io.File;
import java.net.InetSocketAddress;

import junit.framework.Test;

import org.apache.http.HttpStatus;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.limewire.http.reactor.LimeConnectingIOReactor;
import org.limewire.net.SocketsManagerImpl;
import org.limewire.nio.NIODispatcher;
import org.limewire.swarm.file.FileChannelSwarmFileWriter;
import org.limewire.swarm.file.FileCoordinator;
import org.limewire.swarm.file.FileCoordinatorImpl;
import org.limewire.swarm.file.SwarmFileWriter;
import org.limewire.util.BaseTestCase;

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
            .setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 10000)
            .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
            .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
            .setParameter(CoreProtocolPNames.USER_AGENT, "LimeTest/1.1");        
        ConnectingIOReactor ioReactor = new LimeConnectingIOReactor(params, NIODispatcher.instance().getScheduledExecutorService(), new SocketsManagerImpl());
      //  ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(1, params);
        SwarmFileWriter swarmFileWriter = new FileChannelSwarmFileWriter(new File("C:/TEST-LimeWireWin.exe"));
        FileCoordinator fileCoordinator = new FileCoordinatorImpl(4898448L, swarmFileWriter);
        
        
        final Swarmer swarmer = new SwarmerImpl(fileCoordinator, ioReactor, params, new Listener(fileCoordinator));
        new Thread(new Runnable() {
            public void run() {
                swarmer.start();
            }
        }).start();
        
        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
        swarmer.addSource(new SourceImpl(new InetSocketAddress("www9.limewire.com", 80), "/download/LimeWireWin.exe", true), null);
        
        Thread.sleep(5000);
        
        swarmFileWriter.finish();
    }
    
    private static class Listener implements SourceEventListener {
        private final FileCoordinator fileCoordinator;
        private boolean retry;
        
        public Listener(FileCoordinator fileCoordinator) {
            this.fileCoordinator = fileCoordinator;
        }

        public void connected(Swarmer swarmer, Source source) {
            retry = false;
        }
        
        public void connectFailed(Swarmer swarmer, Source source) {
            retry = false;
        }
        
        public void connectionClosed(Swarmer swarmer, Source source) {
            if(retry && fileCoordinator.isRangeAvailableForLease()) {
                swarmer.addSource(source, null);
            }
        }
        
        public void responseProcessed(Swarmer swarmer, Source source, int statusCode) {
            if(statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_PARTIAL_CONTENT)
                retry = true;
            else
                retry = false;
        }
        
    }

}
