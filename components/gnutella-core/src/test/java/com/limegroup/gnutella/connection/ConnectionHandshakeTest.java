package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.stubs.ConnectionListenerStub;
import junit.framework.*;
import java.io.*;
import java.util.Properties;
import com.sun.java.util.collections.Arrays;

/**
 * Tests connection handshaking code.
 * @see Connection
 * @see ConnectionTest
 */
public class ConnectionHandshakeTest extends TestCase {
    public ConnectionHandshakeTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ConnectionHandshakeTest.class);
    }

    /////////////// Setup Code: non-standard for legacy reasons ///////////////
  
    private static class ConnectionPair {
        Connection in;
        Connection out;
    }

    private static ConnectionPair connect(HandshakeResponder inProperties,
                                          Properties outProperties1,
                                          HandshakeResponder outProperties2) {
        ConnectionPair ret=new ConnectionPair();
        try {
            MiniAcceptor acceptor=new MiniAcceptor(inProperties, 6666);
            ret.out=new Connection("localhost", 6666,
                                   outProperties1, outProperties2,
                                   true);
            ret.out.initialize(new ConnectionListenerStub());
            ret.in=acceptor.accept();
            if (ret.in==null || ret.out==null)
                return null;
            else
                return ret;
        } catch (IOException e) { 
            fail("Couldn't create connection");
            return null;
        }
    }

    private static void disconnect(ConnectionPair cp) {
        if (cp.in!=null)
            cp.in.close();
        if (cp.out!=null)
            cp.out.close();
    } 

    ///////////////////////////// Data for Tests ////////////////////////////

    private final Properties props=new Properties(); {
        props.setProperty("Query-Routing", "0.3");        
    }
    HandshakeResponder standardResponder=new HandshakeResponder() {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) {
            return new HandshakeResponse(props);
        }
    };        

    private final HandshakeResponder secretResponder=new HandshakeResponder() {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) {
            Properties props2=new Properties();
            props2.setProperty("Secret", "abcdefg");
            return new HandshakeResponse(props2);
        }
    };
    
    private ConnectionPair p=null;

    /////////////////////////// Actual Tests ///////////////////////////

    public void testLessThan() {
        assertTrue(! Connection.notLessThan06("CONNECT"));
        assertTrue(! Connection.notLessThan06("CONNECT/0.4"));
        assertTrue(! Connection.notLessThan06("CONNECT/0.599"));
        assertTrue(! Connection.notLessThan06("CONNECT/XP"));
        assertTrue(Connection.notLessThan06("CONNECT/0.6"));
        assertTrue(Connection.notLessThan06("CONNECT/0.7"));
        assertTrue(Connection.notLessThan06("GNUTELLA CONNECT/1.0"));
    }

    public void test0404() {
        //1. 0.4 => 0.4
        p=connect(null, null, null);
        assertTrue(p!=null);
        assertTrue(p.in.getProperty("Query-Routing")==null);
        assertTrue(p.out.getProperty("Query-Routing")==null);
        disconnect(p);
    }

    public void test0606() {
        //2. 0.6 => 0.6
        p=connect(standardResponder, props, secretResponder);
        assertTrue(p!=null);
        assertTrue(p.in.getProperty("Query-Routing").equals("0.3"));
        assertTrue(p.out.getProperty("Query-Routing").equals("0.3"));
        assertTrue(p.out.getProperty("Secret")==null);
        assertTrue(p.in.getProperty("Secret").equals("abcdefg"));
        disconnect(p);
    }

    public void test0406() {
        //3. 0.4 => 0.6 (Incoming doesn't send properties)
        p=connect(standardResponder, null, null);
        assertTrue(p!=null);
        assertTrue(p.in.getProperty("Query-Routing")==null);
        assertTrue(p.out.getProperty("Query-Routing")==null);
        disconnect(p);
    }

    public void test0604() {
        //4. 0.6 => 0.4 (If the receiving connection were Gnutella 0.4, this
        //wouldn't work.  But the new guy will automatically upgrade to 0.6.)
        p=connect(null, props, standardResponder);
        assertTrue(p!=null);
        //assertTrue(p.in.getProperty("Query-Routing")==null);
        assertTrue(p.out.getProperty("Query-Routing")==null);
        disconnect(p);
    }

    public void testConnectWithNameTimeout() {
        //TODO: can we get timeout in TCP phase too?
        Connection c=new Connection("this-host-does-not-exist.limewire.com", 6346);
        int TIMEOUT=1000;
        long start=System.currentTimeMillis();
        try {
            c.initialize(new ConnectionListenerStub(), TIMEOUT);
            assertTrue(false);
        } catch (IOException e) {
            //Check that exception happened quickly.  Note fudge factor below.
            long elapsed=System.currentTimeMillis()-start;  
            assertTrue("Took too long to connect: "+elapsed, elapsed<(3*TIMEOUT)/2);
        }
    }   

    private void sleep(long msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) { }
    }
}
