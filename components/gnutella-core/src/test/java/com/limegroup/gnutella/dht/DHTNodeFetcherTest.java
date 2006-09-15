package com.limegroup.gnutella.dht;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.dht.DHTManager.DHTMode;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.settings.DHTSettings;
import com.limegroup.gnutella.stubs.ConnectionManagerStub;
import com.limegroup.gnutella.util.FixedSizeLIFOSet;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.IpPortImpl;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.PrivilegedAccessor;

public class DHTNodeFetcherTest extends DHTTestCase {
    
    /**
     * Ultrapeer 1 UDP connection.
     */
    private static DatagramSocket[] UDP_ACCESS;
    
    private DHTBootstrapperStub dhtBootstrapper;

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
        setSettings();
        dhtBootstrapper = new DHTBootstrapperStub();
        
        UDP_ACCESS = new DatagramSocket[10];
        for (int i = 0; i < UDP_ACCESS.length; i++) {
            UDP_ACCESS[i] = new DatagramSocket();
            UDP_ACCESS[i].setSoTimeout(10000);
        }
        
        //fake a connection to the network
        ConnectionManagerStub cmStub = new ConnectionManagerStub() {
            public boolean isConnected() {
                return true;
            }
        };
        PrivilegedAccessor.setValue(RouterService.class,"manager",cmStub);
        assertTrue(RouterService.isConnected());
    }

    public void testRequestDHTHostsFromSingleHost() throws Exception {
        DHTNodeFetcher nodeFetcher = new DHTNodeFetcher(dhtBootstrapper);
        
        //request hosts
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", UDP_ACCESS[0].getLocalPort());
        nodeFetcher.requestDHTHosts(addr);
        byte[] datagramBytes = new byte[1000];
        DatagramPacket pack = new DatagramPacket(datagramBytes, 1000);
        UDP_ACCESS[0].receive(pack);
        InputStream in = new ByteArrayInputStream(pack.getData());
        Message m = MessageFactory.read(in);
        m.hop();
        assertInstanceof(PingRequest.class, m);
        PingRequest ping = (PingRequest)m;
        assertTrue(ping.requestsDHTIPP());
        
        //should not send another request until we get the pong
        nodeFetcher.requestDHTHosts(addr);
        try{
            datagramBytes = new byte[1000];
            pack = new DatagramPacket(datagramBytes, 1000);
            UDP_ACCESS[0].receive(pack);
            fail("Shouldn't have received a packet");
        }catch(SocketTimeoutException ste) {}
        
        //send the pong now
        IpPortImpl ipp = new IpPortImpl("213.0.0.1", 1111);
        PingReply reply = PingReply.create(ping.getGUID(), (byte)1, IpPort.EMPTY_LIST,
                Arrays.asList(ipp));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reply.write(baos);
        NetworkUtils.getByAddress(RouterService.getAddress());
        pack = new DatagramPacket(baos.toByteArray(), 
                baos.toByteArray().length,
                NetworkUtils.getByAddress(RouterService.getAddress()),
                RouterService.getPort());
        UDP_ACCESS[0].send(pack);
        //test the processing
        Thread.sleep(1000);
        Set<SocketAddress> hosts = dhtBootstrapper.getBootstrapHosts();
        assertEquals(ipp.getInetAddress(), ((InetSocketAddress)hosts.iterator().next()).getAddress());
        
        //now we should be able to send a ping again
        datagramBytes = new byte[1000];
        pack = new DatagramPacket(datagramBytes, 1000);
        addr = new InetSocketAddress("127.0.0.1", UDP_ACCESS[1].getLocalPort());
        nodeFetcher.requestDHTHosts(addr);
        UDP_ACCESS[1].receive(pack);
        in = new ByteArrayInputStream(pack.getData());
        m = MessageFactory.read(in);
        m.hop();
        assertInstanceof(PingRequest.class, m);
        ping = (PingRequest)m;
        assertTrue(ping.requestsDHTIPP());
    }
    
    public void testAddHostCatcherActiveNodes() throws Exception {
        DHTSettings.DHT_NODE_FETCHER_TIME.setValue(100);
        dhtBootstrapper.setWaitingForNodes(true);
        DHTNodeFetcher nodeFetcher = new DHTNodeFetcher(dhtBootstrapper);
        
        RouterService.getHostCatcher().clear();
        for(int i=0; i < UDP_ACCESS.length; i++) {
            ExtendedEndpoint ep = new ExtendedEndpoint(
                    "127.0.0.1",
                    UDP_ACCESS[i].getLocalPort());
            ep.setDHTVersion(0);
            ep.setDHTMode(DHTMode.ACTIVE);
            RouterService.getHostCatcher().add(ep, false);
        }
        
        nodeFetcher.startTimerTask();
        Thread.sleep(500);
        Set<SocketAddress> hosts = dhtBootstrapper.getBootstrapHosts();
        assertEquals(UDP_ACCESS.length, hosts.size());
    }
    
    public void testRequestDHTHostsFromHostCatcher() throws Exception{
        DHTSettings.DHT_NODE_FETCHER_TIME.setValue(100);
        dhtBootstrapper.setWaitingForNodes(true);
        DHTNodeFetcher nodeFetcher = new DHTNodeFetcher(dhtBootstrapper);
        
        RouterService.getHostCatcher().clear();
        for(int i=0; i < UDP_ACCESS.length; i++) {
            ExtendedEndpoint ep = new ExtendedEndpoint(
                    "127.0.0.1",
                    UDP_ACCESS[i].getLocalPort());
            ep.setDHTVersion(0);
            ep.setDHTMode(DHTMode.PASSIVE);
            RouterService.getHostCatcher().add(ep, false);
        }
        nodeFetcher.startTimerTask();
        Thread.sleep(1000);
        byte[] datagramBytes;
        DatagramPacket pack;
        InputStream in;
        Message m;
        PingRequest ping;
        for(int i=0; i < UDP_ACCESS.length; i++) {
            System.out.println("test");
          datagramBytes = new byte[1000];
          pack = new DatagramPacket(datagramBytes, 1000);
          UDP_ACCESS[i].receive(pack);
          in = new ByteArrayInputStream(pack.getData());
          m = MessageFactory.read(in);
          m.hop();
          assertInstanceof(PingRequest.class, m);
          ping = (PingRequest)m;
          assertTrue(ping.requestsDHTIPP());
        }
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    private class DHTBootstrapperStub implements DHTBootstrapper {
        
        private boolean isWaitingForNodes;
        
        private Set<SocketAddress> bootstrapHosts;
        
        public DHTBootstrapperStub() {
            bootstrapHosts = new FixedSizeLIFOSet<SocketAddress>(10);
        }

        public void addBootstrapHost(SocketAddress hostAddress) {
            bootstrapHosts.add(hostAddress);
        }

        public void addPassiveNode(SocketAddress hostAddress) {
        }

        public void bootstrap() {
        }

        public boolean isBootstrappingFromRT() {
            return false;
        }

        public boolean isWaitingForNodes() {
            return isWaitingForNodes;
        }

        public void stop() {
        }
        
        public void setWaitingForNodes(boolean waiting) {
            isWaitingForNodes = waiting;
        }
        
        public Set<SocketAddress> getBootstrapHosts() {
            return bootstrapHosts;
        }
    }
}
