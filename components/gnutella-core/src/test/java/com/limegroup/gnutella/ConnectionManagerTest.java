package com.limegroup.gnutella;

import junit.framework.*;
import java.util.Properties;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.security.DummyAuthenticator;
import com.limegroup.gnutella.stubs.MessageRouterStub;
import com.limegroup.gnutella.MiniAcceptor;

/**
 * PARTIAL unit tests for ConnectionManager.  Makes sure HostCatcher is notified
 * of right events.  VERY slow--involves lots of timeouts--so not part of the
 * standard test suite.  
 */
public class ConnectionManagerTest extends TestCase {
    private ConnectionManager cm;
    private TestHostCatcher hc;

    public ConnectionManagerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ConnectionManagerTest.class);
    }

    public void setUp() {
        SettingsManager.instance().setKeepAlive(0);
        SettingsManager.instance().setConnectOnStartup(false);
        SettingsManager.instance().setQuickConnectHosts(new String[0]);        
        cm=new ConnectionManager(new ActivityCallbackStub(),
                                 new DummyAuthenticator());
        hc=new TestHostCatcher();
        cm.initialize(new MessageRouterStub(), hc);        
    }

    public void tearDown() {
        //Ensure no more threads.
        cm.disconnect();
    }
    
    public void testUnreachableHost() {
        hc.endpoint=new Endpoint("1.2.3.4", 6346);
        cm.setKeepAlive(1);
        sleep();
        assertEquals(0, hc.connectSuccess);
        assertEquals(1, hc.connectFailures);
        assertEquals(1, hc.loopsDone);
        cm.disconnect();
    }

    public void testWrongProtocolHost() {
        hc.endpoint=new Endpoint("www.yahoo.com", 80);
        cm.setKeepAlive(1);
        sleep();
        assertEquals(0, hc.connectSuccess);
        assertEquals(1, hc.connectFailures);
        assertEquals(1, hc.loopsDone);
    }

    public void testGoodHost() {
        MiniAcceptor acceptor=new MiniAcceptor(null);
        hc.endpoint=new Endpoint("localhost", 6346);
        cm.setKeepAlive(1);
        Connection in=acceptor.accept();
        assertEquals(1, hc.connectSuccess);
        assertEquals(0, hc.connectFailures);
        assertEquals(0, hc.loopsDone);
        in.close();
        sleep();
        assertEquals(1, hc.connectSuccess);
        assertEquals(0, hc.connectFailures);
        assertEquals(1, hc.loopsDone);        
    }

    public void testRejectHost() {
        MiniAcceptor acceptor=new MiniAcceptor(new RejectResponder());
        hc.endpoint=new Endpoint("localhost", 6346);
        cm.setKeepAlive(1);
        Connection in=acceptor.accept();
        sleep();
        assertEquals(1, hc.connectSuccess);   //success even though rejected
        assertEquals(0, hc.connectFailures);
        assertEquals(1, hc.loopsDone);
    }
    
    class RejectResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response, 
                                         boolean outgoing) {
            return new HandshakeResponse(HandshakeResponse.SHIELDED,
                                         HandshakeResponse.SHIELDED_MESSAGE,
                                         new Properties());
        }
    }

    private void sleep() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
    }        
}

class TestHostCatcher extends HostCatcher {
    volatile Endpoint endpoint;
    volatile int connectSuccess=0;
    volatile int connectFailures=0;
    volatile int loopsDone=0;

    TestHostCatcher() {
        super(new ActivityCallbackStub());
    }

    public synchronized Endpoint getAnEndpoint() throws InterruptedException {
        if (endpoint==null)
            throw new InterruptedException();
        else {
            Endpoint ret=endpoint;
            endpoint=null;
            return ret;
        }
    }

    public synchronized void doneWithConnect(Endpoint e, boolean success) {
        if (success)
            connectSuccess++;
        else
            connectFailures++;
    }

    public synchronized void doneWithMessageLoop(Endpoint e) {
        loopsDone++;
    }
}
