package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.handshaking.*;
import com.limegroup.gnutella.stubs.ConnectionListenerStub;
import junit.framework.*;
import java.io.*;
import java.util.Properties;
import com.sun.java.util.collections.Arrays;

/**
 * Tests connection readng and writing.
 * @see Connection
 * @see ConnectionHandshakeTest
 */
public class ConnectionTest extends TestCase {
    public ConnectionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ConnectionTest.class);
    }

    final int PORT=6666;
    Connection out;
    Connection in;
    TestConnectionListener inListener;
    TestConnectionListener outListener;

    public void setUp() {
        SettingsManager.instance().loadDefaults();
        try {
            inListener=new TestConnectionListener();
            outListener=new TestConnectionListener();

            MiniAcceptor acceptor=new MiniAcceptor(inListener, null, PORT);
            out=new Connection("127.0.0.1", PORT);
            out.initialize(outListener);
            in=acceptor.accept();
            assertTrue("Couldn't accept connection", in!=null);
            assertTrue(inListener.normal());
            assertTrue(outListener.normal());
        } catch (IOException e) {
            fail("Couldn't establish connection");
        }
    }

    public void tearDown() {
        out.close();
        in.close();
    }

    //////////////////////////////////////////////////////////////////////////

    public void testWriteRead() {
        //1. Write a ping, wait for it to be sent...
        PingRequest ping=new PingRequest((byte)3);
        assertTrue(! out.write(ping));
        assertTrue(outListener.normal());
        sleep(200);
        //   ...and read it.
        assertTrue(inListener.message==null);
        in.read();
        assertTrue(inListener.normal());
        assertTrue(inListener.message!=null);
        assertTrue(inListener.message instanceof PingRequest);
        assertTrue(Arrays.equals(inListener.message.getGUID(), ping.getGUID()));
        inListener.message=null;

        //2. Write a query, wait for it to be sent...
        QueryRequest query=new QueryRequest((byte)3, 0, "hello");
        assertTrue(! out.write(query));
        sleep(200);
        assertTrue(outListener.normal());
        //   ...and read it.
        assertTrue(inListener.message==null);
        in.read();
        assertTrue(inListener.normal());
        assertTrue(inListener.message!=null);
        assertTrue(inListener.message instanceof QueryRequest);
        assertTrue(Arrays.equals(inListener.message.getGUID(), query.getGUID()));
        assertEquals(query.getQuery(),
                     (((QueryRequest)inListener.message).getQuery()));                
    }
    
    public void testEmptyWrite() {
        out.write();
        in.read();
        assertTrue(inListener.normal());
        assertTrue(outListener.normal());
    }

    public void testLargeWrite() {
        //Unfortunately this is OS-specific...
        final int SIZE=100000;
        SettingsManager.instance().setMaxLength(SIZE+1);

        //Try to write huge message.  Nothing should go through initially.
        PingRequest big=new PingRequest(GUID.makeGuid(), 
                                        (byte)3, (byte)0,
                                        new byte[SIZE]);
        assertTrue(out.write(big));
        assertTrue(outListener.needsWrite);
        assertTrue(!outListener.closed);
        assertTrue(outListener.error==null);        
        out.write(new QueryRequest((byte)3, 0, "drop me"));//will NOT be written
        in.read();
        assertTrue(inListener.message==null);
        assertTrue(inListener.normal());
        outListener.needsWrite=false;

        //Keep writing until message is sent or we timeout.
        final int TIMEOUT=2000;    //2 secs
        long start=System.currentTimeMillis();
        while (true) {
            long elapsed=System.currentTimeMillis()-start;
            if (elapsed > TIMEOUT) {
                fail("Couldn't send message in reasonable time");
            }
            if (! out.write())
                break;
        }
        assertTrue(outListener.normal());
        assertTrue("Still has queued data", !out.write());

        //Read until message is done
        start=System.currentTimeMillis();
        while (true) {
            long elapsed=System.currentTimeMillis()-start;
            if (elapsed > TIMEOUT) {
                fail("Couldn't read message in reasonable time");
            }
            in.read();
            if (inListener.message!=null)
                break;
        }
        assertTrue(outListener.normal());
        assertTrue(inListener.normal());
        assertTrue(inListener.message instanceof PingRequest);
        assertTrue(inListener.message.getLength()==SIZE); 

        //Make sure second message wasn't sent
        inListener.message=null;
        sleep(200);
        in.read();
        assertTrue(inListener.message==null);
        assertTrue(inListener.normal());
    }

    public void testUnrecoverablyLargeMessage() {    
        final int SIZE=100000;
        //Write a huge message
        PingRequest big=new PingRequest(GUID.makeGuid(), 
                                        (byte)3, (byte)0,
                                        new byte[SIZE]);
        out.write(big);
        while (out.write()) { }
        
        //Try to read it.   Get error
        final int TIMEOUT=2000;
        long start=System.currentTimeMillis();
        while (true) {
            long elapsed=System.currentTimeMillis()-start;
            if (elapsed > TIMEOUT) {
                fail("Couldn't read message in reasonable time");
            }
            in.read();
            if (inListener.closed==true)
                break;
        }
        assertTrue(inListener.closed);
        assertTrue(inListener.message==null);        
    }

    public void testReadFromClosed() {
        out.close();
        sleep(200);                        //give time for FIN to be propogated
        in.read();
        assertTrue(inListener.closed);     
        assertTrue(! outListener.closed);  //should not generate ERROR event
    }


    public void testWriteToClosed() {
        out.close();
        in.write(new PingRequest((byte)3));//it takes TWO writes to get FIN
        in.write(new PingRequest((byte)4));
        in.write(new PingRequest((byte)5));
        sleep(200);
        assertTrue(inListener.closed);     
        assertTrue(! outListener.closed);  //should not generate ERROR event
    }

    private void sleep(long msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException e) { }
    }
}

