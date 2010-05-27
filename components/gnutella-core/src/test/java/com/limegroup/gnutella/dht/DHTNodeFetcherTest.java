package com.limegroup.gnutella.dht;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DHTSettings;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.mojito.MojitoDHT;
import org.limewire.nio.NIOTestUtils;
import org.limewire.util.PrivilegedAccessor;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.HostCatcher;
import com.limegroup.gnutella.LifecycleManager;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.dht.NodeFetcher.NodeFetcherListener;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.Message.Network;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;

public class DHTNodeFetcherTest extends DHTTestCase {
    
    /**
     * Ultrapeer 1 UDP connection.
     */
    private DatagramSocket[] UDP_ACCESS;
    
    private Injector injector;
    
    private MessageFactory messageFactory;

    private MojitoDHT bootstrapDHT;

    public DHTNodeFetcherTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(DHTNodeFetcherTest.class);
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        DHTTestUtils.setSettings(PORT);
                
        ConnectionSettings.FILTER_CLASS_C.setValue(false);
        
        UDP_ACCESS = new DatagramSocket[10];
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            UDP_ACCESS[i] = new DatagramSocket();
            UDP_ACCESS[i].setSoTimeout(10000);
        }
        
        // fake a connection to the network
        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ConnectionManager.class).to(ConnectionManagerStub.class);
                bind(NodeFetcher.class);
            }            
        });

        ConnectionManagerStub connectionManager 
            = (ConnectionManagerStub) injector.getInstance(ConnectionManager.class);
        connectionManager.setConnected(true);
        
        messageFactory = injector.getInstance(MessageFactory.class);
        
        bootstrapDHT = startBootstrapDHT(
                injector.getInstance(LifecycleManager.class));
    }

    @Override
    protected void tearDown() throws IOException, InterruptedException {
        bootstrapDHT.close();
        NIOTestUtils.waitForNIO();
        
        for (DatagramSocket s : UDP_ACCESS) {
            if (s != null) {
                s.close();
            }
        }
        
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    public void testRequestDHTHostsFromSingleHost() throws Exception {
        NodeFetcher nodeFetcher = injector.getInstance(NodeFetcher.class);
        try {
            nodeFetcher.start();
            //request hosts
            InetSocketAddress addr 
                = new InetSocketAddress("127.0.0.1", 
                        UDP_ACCESS[0].getLocalPort());
            nodeFetcher.ping(addr);
            
            byte[] datagramBytes = new byte[1000];
            DatagramPacket pack = new DatagramPacket(datagramBytes, 1000);
            UDP_ACCESS[0].receive(pack);
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = messageFactory.read(in, Network.TCP);
            m.hop();
            assertInstanceof(PingRequest.class, m);
            PingRequest ping = (PingRequest)m;
            assertTrue(ping.requestsDHTIPP());
            
            //should not send another request until we get the pong
            nodeFetcher.ping(addr);
            try{
                datagramBytes = new byte[1000];
                pack = new DatagramPacket(datagramBytes, 1000);
                UDP_ACCESS[0].receive(pack);
                fail("Shouldn't have received a packet");
            } catch(SocketTimeoutException ste) {}
            
            final AtomicReference<InetSocketAddress> addressRef 
                = new AtomicReference<InetSocketAddress>();
            final CountDownLatch latch = new CountDownLatch(1);
            nodeFetcher.addNodeFetcherListener(new NodeFetcherListener() {
                @Override
                public void handleActiveNode(SocketAddress address) {
                    addressRef.set((InetSocketAddress)address);
                    latch.countDown();
                }
            });
            
            //send the pong now
            IpPortImpl ipp = new IpPortImpl("213.0.0.1", 1111);
            PingReply reply = injector.getInstance(PingReplyFactory.class).create(ping.getGUID(), (byte)1, IpPort.EMPTY_LIST,
                    Arrays.asList(ipp));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            reply.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                    baos.toByteArray().length,
                    new InetSocketAddress("localhost", injector.getInstance(NetworkManager.class).getPort()));
            UDP_ACCESS[0].send(pack);
            
            if (!latch.await(1, TimeUnit.SECONDS)) {
                fail("Didn't receive an address!");
            }
            
            assertEquals(ipp.getInetAddress(), addressRef.get().getAddress());
            
            //now we should be able to send a ping again
            datagramBytes = new byte[1000];
            pack = new DatagramPacket(datagramBytes, 1000);
            addr = new InetSocketAddress("127.0.0.1", UDP_ACCESS[1].getLocalPort());
            nodeFetcher.ping(addr);
            UDP_ACCESS[1].receive(pack);
            in = new ByteArrayInputStream(pack.getData());
            m = messageFactory.read(in, Network.TCP);
            m.hop();
            assertInstanceof(PingRequest.class, m);
            ping = (PingRequest)m;
            assertTrue(ping.requestsDHTIPP());
        } finally {
            nodeFetcher.close();
        }
    }
    
    public void testRequestMultipleTimesFromSingleHost() throws Exception {
        NodeFetcher nodeFetcher = injector.getInstance(NodeFetcher.class);
        
        try {
            // try to send a ping, unregister it and send another ping
            nodeFetcher.setExpireTime(100, TimeUnit.MILLISECONDS);
            
            //wait: LISTEN_EXPIRE_TIME is not volatile and a wrong value may be read otherwise
            Thread.sleep(1000);  
            byte[] datagramBytes = new byte[1000];
            DatagramPacket pack = new DatagramPacket(datagramBytes, 1000);
            InetSocketAddress addr = new InetSocketAddress("127.0.0.1", UDP_ACCESS[2].getLocalPort());
            nodeFetcher.ping(addr);
            Thread.sleep(500);
            datagramBytes = new byte[1000];
            pack = new DatagramPacket(datagramBytes, 1000);
            addr = new InetSocketAddress("127.0.0.1", UDP_ACCESS[2].getLocalPort());
            nodeFetcher.ping(addr);
            UDP_ACCESS[2].receive(pack);
            InputStream in = new ByteArrayInputStream(pack.getData());
            Message m = messageFactory.read(in, Network.TCP);
            m.hop();
            assertInstanceof(PingRequest.class, m);
            PingRequest ping = (PingRequest)m;
            assertTrue(ping.requestsDHTIPP());
        } finally {
            nodeFetcher.close();
        }
    }
    
    public void testAddHostCatcherActiveNodes() throws Exception {
        PrivilegedAccessor.setValue(DHTSettings.DHT_NODE_FETCHER_TIME, "value", 100L);
        
        NodeFetcher nodeFetcher = injector.getInstance(NodeFetcher.class);
        try {
            HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);
            for(int i=0; i < UDP_ACCESS.length; i++) {
                ExtendedEndpoint ep = new ExtendedEndpoint(
                        "127.0.0.1",
                        UDP_ACCESS[i].getLocalPort());
                ep.setDHTVersion(0);
                ep.setDHTMode(DHTMode.ACTIVE);
                hostCatcher.add(ep, false);
            }
            
            final CountDownLatch latch = new CountDownLatch(UDP_ACCESS.length);
            nodeFetcher.addNodeFetcherListener(new NodeFetcherListener() {
                @Override
                public void handleActiveNode(SocketAddress address) {
                    latch.countDown();
                }
            });
            
            nodeFetcher.start();
            
            if (!latch.await(1L, TimeUnit.SECONDS)) {
                fail("Did not receive all addresses!");
            }
        } finally {
            nodeFetcher.close();
        }
    }
    
    public void testRequestDHTHostsFromHostCatcher() throws Exception{
        PrivilegedAccessor.setValue(DHTSettings.DHT_NODE_FETCHER_TIME, "value", 100L);
        
        NodeFetcher nodeFetcher = injector.getInstance(NodeFetcher.class);
        try {
            HostCatcher hostCatcher = injector.getInstance(HostCatcher.class);
            for(int i=0; i < UDP_ACCESS.length; i++) {
                ExtendedEndpoint ep = new ExtendedEndpoint(
                        "127.0.0.1",
                        UDP_ACCESS[i].getLocalPort());
                ep.setDHTVersion(0);
                ep.setDHTMode(DHTMode.PASSIVE);
                hostCatcher.add(ep, false);
            }
            nodeFetcher.start();
            Thread.sleep(1000);
            byte[] datagramBytes;
            DatagramPacket pack;
            InputStream in;
            Message m;
            PingRequest ping;
            for(int i=0; i < UDP_ACCESS.length; i++) {
                datagramBytes = new byte[1000];
                pack = new DatagramPacket(datagramBytes, 1000);
                UDP_ACCESS[i].receive(pack);
                in = new ByteArrayInputStream(pack.getData());
                m = messageFactory.read(in, Network.TCP);
                m.hop();
                assertInstanceof(PingRequest.class, m);
                ping = (PingRequest)m;
                assertTrue(ping.requestsDHTIPP());
            }
        } finally {
            nodeFetcher.close();
        }
    }
}