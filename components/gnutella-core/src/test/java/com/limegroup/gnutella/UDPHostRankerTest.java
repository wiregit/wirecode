package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.IpPort;
import com.limegroup.gnutella.util.PrivilegedAccessor;

/**
 * Tests that UDP host ranker works.
 */
public class UDPHostRankerTest extends ClientSideTestCase {
    protected static final int PORT=6669;
    protected static int TIMEOUT=1000; // should override super

    public UDPHostRankerTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(UDPHostRankerTest.class);
    }    

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    ///////////////////////// Actual Tests ////////////////////////////

    public void testRanker() throws Exception {
        DatagramSocket[] udps = new DatagramSocket[20];
        for (int i = 0; i < udps.length; i++)   
            udps[i] = new DatagramSocket();
        
        final List list = new ArrayList();
        for (int i = 0; i < udps.length; i++)
            list.add(new IpPortImpl(udps[i]));

        final MLImpl ml = new MLImpl();

        PingRequest pr = new PingRequest(GUID.makeGuid(), (byte)1);
        UDPPinger pinger = new UniqueHostPinger();
        pinger.rank(list, ml, null, pr);
        
        Thread.sleep(500);
        assertTrue(ml.registered);

        for (int i = 0; i < udps.length; i++) {
            DatagramPacket pack = new DatagramPacket(new byte[1000], 1000);
            udps[i].setSoTimeout(TIMEOUT);
            try {
                udps[i].receive(pack);
            } catch (IOException bad) {
               fail("Did not get expected message, i = " + i, bad);
            }
            InputStream in = new ByteArrayInputStream(pack.getData());
            // as long as we don't get a ClassCastException we are good to go
            PingRequest ping = (PingRequest) Message.read(in);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PingReply pong = PingReply.create(ping.getGUID(), (byte)1);
            pong.write(baos);
            pack = new DatagramPacket(baos.toByteArray(), 
                                      baos.toByteArray().length,
                                      InetAddress.getLocalHost(), PORT);
            udps[i].send(pack);
        }

        Thread.sleep(2000); //processing
        assertEquals(udps.length, ml.count);

        // wait 20 seconds, make sure MessageRouter's map is clear
        Thread.sleep(20000);
        Map map = 
        (Map) PrivilegedAccessor.getValue(RouterService.getMessageRouter(),
                                          "_messageListeners");
        assertEquals(0, map.size());
        assertTrue(ml.unregistered);
    }

    //////////////////////////////////////////////////////////////////
    
    private class MLImpl implements MessageListener {
        public int count = 0;
        public boolean registered = false;
        public boolean unregistered = false;
        public void processMessage(Message m, ReplyHandler handler) {
            assertTrue(m instanceof PingReply);
            count++;
        }
        public void registered(byte[] guid) {
            registered = true;
        }
        public void unregistered(byte[] guid) {
            unregistered = true;
        }
    }
    
    private class IpPortImpl implements IpPort {
        DatagramSocket _ds;
        public IpPortImpl(DatagramSocket ds) {
            _ds = ds;
        }
        public InetAddress getInetAddress() { 
            try {
                return InetAddress.getLocalHost();
            }
            catch (Exception noway) {}
            return null;
        }
        public int getPort() { return _ds.getLocalPort(); }
        public String getAddress() {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            }
            catch (Exception noway) {}
            return null;
        }
    }

    public static Integer numUPs() {
        return new Integer(1);
    }

    public static ActivityCallback getActivityCallback() {
        return new MyActivityCallback();
    }

    private static byte[] myIP() {
        return new byte[] { (byte)192, (byte)168, 0, 1 };
    }

    public static class MyActivityCallback extends ActivityCallbackStub {
        private RemoteFileDesc rfd = null;
        public RemoteFileDesc getRFD() {
            return rfd;
        }

        public void handleQueryResult(RemoteFileDesc rfd,
                                      HostData data,
                                      Set locs) {
            this.rfd = rfd;
        }
    }


}
