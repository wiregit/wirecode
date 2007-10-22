package com.limegroup.gnutella;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import junit.framework.Test;

import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

public class UDPStatusTest extends ClientSideTestCase {

    public UDPStatusTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UDPStatusTest.class);
    }    
    
    protected static ActivityCallback getActivityCallback() {
        return new ActivityCallbackStub();
    }
    
    @Override
    public int getNumberOfPeers() {
        return 1;
    }
    
    public void testSolicited() throws Exception {
        drainAll();
        assertFalse(ProviderHacks.getNetworkManager().canReceiveSolicited());
        assertFalse(ProviderHacks.getNetworkManager().canReceiveUnsolicited());
        assertTrue(ProviderHacks.getUdpService().isListening());
        
        DatagramSocket s = new DatagramSocket(6346);
        s.setSoTimeout(1000);
        
        try {
            PingReply pong = ProviderHacks.getPingReplyFactory().create(ProviderHacks.getUdpService().getSolicitedGUID().bytes().clone(),
                    (byte)1,6346,InetAddress.getLocalHost().getAddress());
            UDPService.mutateGUID(pong.getGUID(), InetAddress.getLocalHost(), 6346);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pong.write(baos);
            byte []buf = baos.toByteArray();
            s.send(new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),
                    ProviderHacks.getUdpService().getStableUDPPort()));
            Thread.sleep(100);
            assertTrue(ProviderHacks.getNetworkManager().canReceiveSolicited());
            assertFalse(ProviderHacks.getNetworkManager().canReceiveUnsolicited());
        } catch (IOException bad) {
            fail(bad);
        } finally {
            s.close();
        }
        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),"_acceptedSolicitedIncoming",Boolean.FALSE);
        assertFalse(ProviderHacks.getNetworkManager().canReceiveSolicited());
    }
    
    public void testUnsolicited() throws Exception {
        drainAll();
        assertFalse(ProviderHacks.getNetworkManager().canReceiveSolicited());
        assertFalse(ProviderHacks.getNetworkManager().canReceiveUnsolicited());
        assertTrue(ProviderHacks.getUdpService().isListening());
        
        DatagramSocket s = new DatagramSocket(6346);
        s.setSoTimeout(1000);
        
        try {
            PingRequest ping = ProviderHacks.getPingRequestFactory().createPingRequest(ProviderHacks.getUdpService().getConnectBackGUID().bytes(), (byte)1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ping.write(baos);
            byte []buf = baos.toByteArray();
            s.send(new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),
                    ProviderHacks.getUdpService().getStableUDPPort()));
            Thread.sleep(100);
            assertFalse(ProviderHacks.getNetworkManager().canReceiveSolicited());
            assertTrue(ProviderHacks.getNetworkManager().canReceiveUnsolicited());
        } catch (IOException bad) {
            fail(bad);
        } finally {
            s.close();
        }
        PrivilegedAccessor.setValue(ProviderHacks.getUdpService(),"_acceptedUnsolicitedIncoming",Boolean.FALSE);
        assertFalse(ProviderHacks.getNetworkManager().canReceiveUnsolicited());
    }
    
    public void testUnsolicitedConnected() throws Exception {
        drainAll();
        assertFalse(ProviderHacks.getNetworkManager().canReceiveSolicited());
        assertFalse(ProviderHacks.getNetworkManager().canReceiveUnsolicited());
        assertTrue(ProviderHacks.getUdpService().isListening());
        
        DatagramSocket s = new DatagramSocket(testUP[0].getPort(),testUP[0].getInetAddress());
        s.setSoTimeout(1000);
        
        try {
            PingRequest ping = ProviderHacks.getPingRequestFactory().createPingRequest(ProviderHacks.getUdpService().getConnectBackGUID().bytes(), (byte)1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ping.write(baos);
            byte []buf = baos.toByteArray();
            s.send(new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),
                    ProviderHacks.getUdpService().getStableUDPPort()));
            Thread.sleep(100);
            assertFalse(ProviderHacks.getNetworkManager().canReceiveSolicited());
            assertFalse(ProviderHacks.getNetworkManager().canReceiveUnsolicited());
        } catch (IOException bad) {
            fail(bad);
        } finally {
            s.close();
        }
    }
}
