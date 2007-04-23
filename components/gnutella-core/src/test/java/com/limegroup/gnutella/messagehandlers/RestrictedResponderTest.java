package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.List;

import junit.framework.Test;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.security.SecureMessage;
import org.limewire.security.SecureMessageCallback;
import org.limewire.security.SecureMessageVerifier;
import org.limewire.security.Verifier;
import org.limewire.setting.StringArraySetting;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.GGEP;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.vendor.RoutableGGEPMessage;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.simpp.SimppListener;
import com.limegroup.gnutella.simpp.SimppManager;
import com.limegroup.gnutella.stubs.ReplyHandlerStub;

@SuppressWarnings("all")
public class RestrictedResponderTest extends BaseTestCase {

    public RestrictedResponderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RestrictedResponderTest.class);
    }    
    
    static InetSocketAddress addr;
    static ReplyHandler h;
    public void setUp() throws Exception {
        FilterSettings.HOSTILE_IPS.setValue(new String[]{"1.2.3.4"});
        addr = new InetSocketAddress(InetAddress.getLocalHost(),1000);
        h = new ReplyHandlerStub() {
            public String getAddress() {
                return "1.2.3.4";
            }
        };
    }
    
    public void testRestrictions() throws Exception {
        // ban everyone
        FilterSettings.HOSTILE_IPS.setValue(new String[0]);
        TestResponder responder = new TestResponder(null);
        Message m = PingRequest.createMulticastPing();
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
        assertNull(responder.addr);
        assertNull(responder.handler);
        
        // allow this specific hosts
        FilterSettings.HOSTILE_IPS.setValue(new String[]{"1.2.3.4"});
        triggerSimppUpdate();
        responder.handleMessage(m, addr, h);
        assertSame(m, responder.msg);
        assertSame(addr, responder.addr);
        assertSame(h, responder.handler);
        
        // but someone else won't get in
        responder.reset();
        responder.handleMessage(m, addr, new ReplyHandlerStub() {
            public String getAddress() {
                return "1.2.3.5";
            }
        });
        
        assertNull(responder.msg);
        assertNull(responder.addr);
        assertNull(responder.handler);
    }

    /**
     * tests that messages are verified.
     */
    public void testSecure() throws Exception {
        TestVerifier verifier = new TestVerifier();
        
        TestResponder responder = new TestResponder(verifier);
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(),1000);
        ReplyHandler h = new ReplyHandlerStub() {
            public String getAddress() {
                return "1.2.3.4";
            }
        };
        TestGGEPMessage m = new TestGGEPMessage();
        
        // the message should be sent for verification
        responder.handleMessage(m, addr, h);
        assertSame(verifier.sm, m);
        
        // if it doesn't verify, it wont' get processed
        verifier.smc.handleSecureMessage(verifier.sm, false);
        assertNull(responder.msg);
        
        // but a message that verifies will be processed
        verifier.smc.handleSecureMessage(verifier.sm, true);
        assertSame(m,responder.msg);
    }
    
    /** tests that the return address of a message is respected */
    public void testReturnAddress() throws Exception {
        TestResponder responder = new TestResponder(null);
        final IpPort allowed = new IpPortImpl("1.2.3.4",100);
        final IpPort notAllowed = new IpPortImpl("1.2.3.5",100);
        
        // try a message that comes from an allowed host but wants to go
        // to a non-allowed return address.  It should not be handled.
        TestGGEPMessage m = new TestGGEPMessage();
        m.returnAddr = notAllowed;
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
        
        // try a message that comes from a non-allowed host but wants to
        // go to an allowed return address
        m.returnAddr = allowed;
        ReplyHandler notAllowedHandler = new ReplyHandlerStub() {
            public String getAddress() {
                return "1.2.3.6";
            }
        };
        responder.handleMessage(m,addr,notAllowedHandler);
        assertSame(m,responder.msg);
        assertEquals(allowed.getAddress(),responder.handler.getAddress());
        
        // if both the return address and source address are allowed, the
        // return address takes precedence.
        responder = new TestResponder(null);
        responder.handleMessage(m,addr,h);
        assertSame(m,responder.msg);
        assertEquals(allowed.getAddress(),responder.handler.getAddress());
    }
    
    /**
     * tests that only newer messages are handled.
     */
    public void testVersioning() throws Exception {
        TestResponder responder = new TestResponder(null);
        TestGGEPMessage m = new TestGGEPMessage();
        responder.handleMessage(m, addr, h);
        assertSame(m,responder.msg);
        
        // now try the same message again, it should not be handled.
        responder.msg = null;
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
        
        // but if the version number is higher, it gets handled.
        m.version = 2;
        responder.handleMessage(m, addr, h);
        assertSame(m,responder.msg);
        
        // a message w/o a version will go through.
        m.version = -1;
        responder.handleMessage(m, addr, h);
        assertSame(m,responder.msg);
        
        // but if it has a version it will have to be higher than the current.
        responder.msg = null;
        m.version = 1;
        responder.handleMessage(m, addr, h);
        assertNull(responder.msg);
    }
    
    private static int simppVersion;
    private void triggerSimppUpdate() throws Exception {
        List<SimppListener> l = (List<SimppListener>) 
            PrivilegedAccessor.getValue(SimppManager.instance(), "listeners");
        for (SimppListener s : l)
            s.simppUpdated(simppVersion++);
    }
    
    private static class TestResponder extends RestrictedResponder {

        Message msg;
        InetSocketAddress addr;
        ReplyHandler handler;
        public TestResponder(SecureMessageVerifier verifier) {
            super(FilterSettings.HOSTILE_IPS, verifier);
        }

        @Override
        protected void processAllowedMessage(Message msg, InetSocketAddress addr, ReplyHandler handler) {
            this.msg = msg;
            this.addr = addr;
            this.handler = handler;
        }
        
        void reset() {
            this.msg = null;
            this.addr = null;
            this.handler = null;
        }
    }
    
    private static class TestVerifier extends SecureMessageVerifier {
        SecureMessage sm;
        SecureMessageCallback smc;
        TestVerifier() {
            super("asdf","asdf");
        }
        @Override
        public void verify(Verifier verifier) {
            sm = verifier.getSecureMessage();
            smc = verifier.getSecureMessageCallback();
        }
        
        @Override
        public void verify(SecureMessage sm, SecureMessageCallback smc) {
            this.sm = sm;
            this.smc = smc;
        }
        
        @Override
        public void verify(PublicKey pubKey, String algorithm, 
                SecureMessage sm, SecureMessageCallback smc) {
            this.sm = sm;
            this.smc = smc;
        }
    }
    
    private static class TestGGEPMessage extends RoutableGGEPMessage {
        int version = 1;
        IpPort returnAddr;
        protected TestGGEPMessage() throws BadPacketException {
            super(new byte[4], 1, 1, new GGEP());
        }
        
        @Override
        public int getRoutableVersion() {
            return version;
        }
        
        @Override
        public IpPort getReturnAddress() {
            return returnAddr;
        }
    }
}
