package com.limegroup.gnutella;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.limewire.util.PrivilegedAccessor;

import junit.framework.Test;

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
    
    protected static Integer numUPs() {
        return new Integer(1);
    }
    
    public void testSolicited() throws Exception {
        drainAll();
        assertFalse(RouterService.canReceiveSolicited());
        assertFalse(RouterService.canReceiveUnsolicited());
        assertTrue(RouterService.getUdpService().isListening());
        
        DatagramSocket s = new DatagramSocket(6346);
        s.setSoTimeout(1000);
        
        try {
            PingReply pong = PingReply.create(RouterService.getUdpService().getSolicitedGUID().bytes(),
                    (byte)1,6346,InetAddress.getLocalHost().getAddress());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            pong.write(baos);
            byte []buf = baos.toByteArray();
            s.send(new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),
                    RouterService.getUdpService().getStableUDPPort()));
            Thread.sleep(100);
            assertTrue(RouterService.canReceiveSolicited());
            assertFalse(RouterService.canReceiveUnsolicited());
        } catch (IOException bad) {
            fail(bad);
        } finally {
            s.close();
        }
        PrivilegedAccessor.setValue(RouterService.getUdpService(),"_acceptedSolicitedIncoming",Boolean.FALSE);
        assertFalse(RouterService.canReceiveSolicited());
    }
    
    public void testUnsolicited() throws Exception {
        drainAll();
        assertFalse(RouterService.canReceiveSolicited());
        assertFalse(RouterService.canReceiveUnsolicited());
        assertTrue(RouterService.getUdpService().isListening());
        
        DatagramSocket s = new DatagramSocket(6346);
        s.setSoTimeout(1000);
        
        try {
            PingRequest ping = new PingRequest(RouterService.getUdpService().getConnectBackGUID().bytes(),
                    (byte)1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ping.write(baos);
            byte []buf = baos.toByteArray();
            s.send(new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),
                    RouterService.getUdpService().getStableUDPPort()));
            Thread.sleep(100);
            assertFalse(RouterService.canReceiveSolicited());
            assertTrue(RouterService.canReceiveUnsolicited());
        } catch (IOException bad) {
            fail(bad);
        } finally {
            s.close();
        }
        PrivilegedAccessor.setValue(RouterService.getUdpService(),"_acceptedUnsolicitedIncoming",Boolean.FALSE);
        assertFalse(RouterService.canReceiveUnsolicited());
    }
    
    public void testUnsolicitedConnected() throws Exception {
        drainAll();
        assertFalse(RouterService.canReceiveSolicited());
        assertFalse(RouterService.canReceiveUnsolicited());
        assertTrue(RouterService.getUdpService().isListening());
        
        DatagramSocket s = new DatagramSocket(testUP[0].getPort(),testUP[0].getInetAddress());
        s.setSoTimeout(1000);
        
        try {
            PingRequest ping = new PingRequest(RouterService.getUdpService().getConnectBackGUID().bytes(),
                    (byte)1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ping.write(baos);
            byte []buf = baos.toByteArray();
            s.send(new DatagramPacket(buf,buf.length,InetAddress.getLocalHost(),
                    RouterService.getUdpService().getStableUDPPort()));
            Thread.sleep(100);
            assertFalse(RouterService.canReceiveSolicited());
            assertFalse(RouterService.canReceiveUnsolicited());
        } catch (IOException bad) {
            fail(bad);
        } finally {
            s.close();
        }
    }
}
