package com.limegroup.gnutella;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import junit.framework.Test;

import com.google.inject.Injector;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingReplyFactory;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;

public class UDPStatusTest extends ClientSideTestCase {

    private NetworkManager networkManager;
    private UDPService udpService;
    private PingReplyFactory pingReplyFactory;
    private PingRequestFactory pingRequestFactory;

    public UDPStatusTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UDPStatusTest.class);
    }    
    
    @Override
    public int getNumberOfPeers() {
        return 1;
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        super.setUp(injector);
        networkManager = injector.getInstance(NetworkManager.class);
        udpService = injector.getInstance(UDPService.class);
        pingReplyFactory = injector.getInstance(PingReplyFactory.class);
        pingRequestFactory = injector.getInstance(PingRequestFactory.class);
    }
    
    public void testSolicited() throws Exception {
        drainAll();
        assertFalse(networkManager.canReceiveSolicited());
        assertFalse(networkManager.canReceiveUnsolicited());
        assertTrue(udpService.isListening());
        
        DatagramSocket s = new DatagramSocket(6346);
        s.setSoTimeout(1000);
        
        try {
            PingReply pong = pingReplyFactory.create(udpService.getSolicitedGUID().bytes().clone(),
                    (byte)1,6346,InetAddress.getLocalHost().getAddress());
            UDPService.mutateGUID(pong.getGUID(), InetAddress.getLocalHost(), 6346);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pong.write(baos);
            byte []buf = baos.toByteArray();
            s.send(new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),
                    udpService.getStableUDPPort()));
            Thread.sleep(100);
            assertTrue(networkManager.canReceiveSolicited());
            assertFalse(networkManager.canReceiveUnsolicited());
        } catch (IOException bad) {
            fail(bad);
        } finally {
            s.close();
        }
    }
    
    public void testUnsolicited() throws Exception {
        drainAll();
        assertFalse(networkManager.canReceiveSolicited());
        assertFalse(networkManager.canReceiveUnsolicited());
        assertTrue(udpService.isListening());
        
        DatagramSocket s = new DatagramSocket(6346);
        s.setSoTimeout(1000);
        
        try {
            PingRequest ping = pingRequestFactory.createPingRequest(udpService.getConnectBackGUID().bytes(), (byte)1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ping.write(baos);
            byte []buf = baos.toByteArray();
            s.send(new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),
                    udpService.getStableUDPPort()));
            Thread.sleep(100);
            assertFalse(networkManager.canReceiveSolicited());
            assertTrue(networkManager.canReceiveUnsolicited());
        } catch (IOException bad) {
            fail(bad);
        } finally {
            s.close();
        }
    }
    
    public void testUnsolicitedConnected() throws Exception {
        drainAll();
        assertFalse(networkManager.canReceiveSolicited());
        assertFalse(networkManager.canReceiveUnsolicited());
        assertTrue(udpService.isListening());
        
        DatagramSocket s = new DatagramSocket(testUP[0].getPort(),testUP[0].getInetAddress());
        s.setSoTimeout(1000);
        
        try {
            PingRequest ping = pingRequestFactory.createPingRequest(udpService.getConnectBackGUID().bytes(), (byte)1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ping.write(baos);
            byte []buf = baos.toByteArray();
            s.send(new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),
                    udpService.getStableUDPPort()));
            Thread.sleep(100);
            assertFalse(networkManager.canReceiveSolicited());
            assertFalse(networkManager.canReceiveUnsolicited());
        } catch (IOException bad) {
            fail(bad);
        } finally {
            s.close();
        }
    }
}
