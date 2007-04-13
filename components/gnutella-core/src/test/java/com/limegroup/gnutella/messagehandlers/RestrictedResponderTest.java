package com.limegroup.gnutella.messagehandlers;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

import junit.framework.Test;

import org.limewire.setting.StringArraySetting;
import org.limewire.util.BaseTestCase;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingRequest;
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
    
    public void testRestrictions() throws Exception {
        // ban everyone
        FilterSettings.HOSTILE_IPS.setValue(new String[0]);
        TestResponder responder = new TestResponder(FilterSettings.HOSTILE_IPS);
        Message m = PingRequest.createMulticastPing();
        InetSocketAddress addr = new InetSocketAddress(InetAddress.getLocalHost(),1000);
        ReplyHandler h = new ReplyHandlerStub() {
            public String getAddress() {
                return "1.2.3.4";
            }
        };
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
        public TestResponder(StringArraySetting setting) {
            super(setting);
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
}
