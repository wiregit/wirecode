package com.limegroup.gnutella.connection;

import java.io.*;
import junit.framework.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import java.util.Properties;
import com.sun.java.util.collections.*;

public class ConnectionDriverTest extends TestCase {
    public ConnectionDriverTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ConnectionDriverTest.class);
        //TestSuite ret=new TestSuite("Simple suite");
        //ret.addTest(new ConnectionDriverTest("testHugeWrite"));
        //return ret;
    }

//     public static void main(String args[]) {
//         ConnectionDriverTest test=new ConnectionDriverTest("test test");
//         test.setUp();
//         System.out.println("Set up");
//         try { Thread.sleep(100); } catch (InterruptedException e) { }
//         test.tearDown();
//         System.out.println("Tear down");

//         try { Thread.sleep(10000); } catch (InterruptedException e) { }

//         test.setUp();
//         System.out.println("Set up");
//         try { Thread.sleep(100); } catch (InterruptedException e) { }

//         test.tearDown();
//         System.out.println("Tear down");
//     }

    /*
     * in1  <--> out1
     * in2  <--> out2
     */
    Connection in1, out1;
    Connection in2, out2;
    final int PORT=6666;
    TestConnectionDriver driver;

    public void setUp() {
        SettingsManager.instance().loadDefaults();
        driver=new TestConnectionDriver();
        driver.initialize();
        try {
            MiniAcceptor acceptor=new MiniAcceptor(driver, null, PORT);
            out1=new Connection("127.0.0.1", PORT);
            out1.initialize(driver);
            in1=acceptor.accept();
            assertTrue(in1!=null);

            acceptor=new MiniAcceptor(driver, null, PORT+1);
            out2=new Connection("127.0.0.1", PORT+1);
            out2.initialize(driver);
            in2=acceptor.accept();
            assertTrue(in2!=null);
        } catch (IOException e) { 
            e.printStackTrace();
            fail("Couldn't create connection");
        }
    }

    public void tearDown() {
        in1.close();
        out1.close();
        in2.close();
        out2.close();
    }

    private void sleep(long msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) { }
    }

    class GGEPProperties extends Properties {
        GGEPProperties() {
            this.put("GGEP", "0.6");
        }
    }

    class GGEPResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) throws IOException {
            Properties props=new Properties();
            props.put("GGEP", "0.6"); 
            return new HandshakeResponse(props);
        }
    }

    class EmptyResponder implements HandshakeResponder {
        public HandshakeResponse respond(HandshakeResponse response,
                                         boolean outgoing) throws IOException {
            Properties props=new Properties();
            return new HandshakeResponse(props);
        }
    }

    //////////////////////////////////////////////////////////////////////////

    public void testWriteRead() {
        //Send writes
        QueryRequest query1=new QueryRequest((byte)3, 0, "query 1");
        QueryRequest query2=new QueryRequest((byte)3, 0, "query 2");
        out1.write(query1);   // out1 -> in1
        in2.write(query2);    // in2  -> out2
        sleep(200);

        //Driver should take care of reads
        assertEquals(2, driver.reads.size());
        assertTrue(driver.reads.contains(new ReadPair(in1, query1)));
        assertTrue(driver.reads.contains(new ReadPair(out2, query2)));
    }

    public void testHugeWrite() {
        //Try to write huge message.
        final int SIZE=100000;
        SettingsManager.instance().setMaxLength(SIZE+1);
        PingRequest big=new PingRequest(GUID.makeGuid(), 
                                        (byte)3, (byte)0,
                                        new byte[SIZE]);
        out1.write(big);     // out1 -> in1
        sleep(200);
        assertEquals(1, driver.reads.size());
        assertTrue(driver.reads.contains(new ReadPair(in1, big)));
    }
}

class TestConnectionDriver extends NonBlockingConnectionDriver {
    List /* of ReadPair */ reads=Collections.synchronizedList(new LinkedList());
    
    public void initialize() {
        super.initialize(null, null, null);
    }
    
    public void read(Connection c, Message m) {
        reads.add(new ReadPair(c, m));
    }

    public void error(Connection c) { }
}

