package com.limegroup.gnutella.udpconnect;

import junit.framework.Test;
import java.io.*;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.UDPServiceStub;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.ManagedThread;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ByteReader;

/**
 * Put full UDPConnection system through various tests.
 */
public final class UDPConnectionTest extends BaseTestCase {

	/*
	 * Constructs the test.
	 */
	public UDPConnectionTest(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(UDPConnectionTest.class);
	}

	/**
	 * Runs this test individually.
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}


    public static void globalSetUp() throws Exception {
        // Setup the test to use the UDPServiceStub
        UDPConnectionProcessor.setUDPServiceForTesting(
            UDPServiceStub.instance());
        RouterService rs = new RouterService(new ActivityCallbackStub());
        Acceptor      ac = rs.getAcceptor();
        ac.start();
    }


    public static void globaltearDown() throws Exception {
        // Cleanup the UDPServiceStub usage
        UDPConnectionProcessor.setUDPServiceForTesting(null);
    }

    public void setUp() throws Exception {
        // Add some simulated connections to the UDPServiceStub
        UDPServiceStub.stubInstance().addReceiver(6346, 6348, 10, 0);
        UDPServiceStub.stubInstance().addReceiver(6348, 6346, 10, 0);
    }
    

    public void tearDown() throws Exception {
        // Clear out the receiver parameters for the UDPServiceStub
        UDPServiceStub.stubInstance().clearReceivers();
    }  

    /**
     * Test that data can be written, echoed and read through the
	 * UDPConnections.
     * 
     * @throws Exception if an error occurs
     */
    public void testBasics() throws Exception {
		final int NUM_BYTES = 20000;

        // Start the second connection in another thread
        // and run it to completion.
        class SubTest extends Thread {
            boolean sSuccess = false;

            public void run() {
                try {
                    yield();
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UTest.echoServer(uconn2, NUM_BYTES);
                    
                } catch(Throwable e) {
                    sSuccess = false;
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        SubTest t = new SubTest();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UTest.echoClient(uconn1, NUM_BYTES);

        // Wait for the second to finish
        t.join();

        // Get the success status of the second connection
        boolean sSuccess = t.getSuccess();

        // Validate the results
        assertTrue("echoClient should return true ", 
            cSuccess);
        assertTrue("echoServer should return true ", 
            sSuccess);
    }

    public void testBlockTransfers() throws Exception {
		final int NUM_BLOCKS = 100;

        // Start the second connection in another thread
        // and run it to completion.
        class SubTest extends Thread {
            boolean sSuccess = false;

            public void run() {
                try {
                    yield();
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UTest.echoServerBlock(uconn2, NUM_BLOCKS);
                    
                } catch(Throwable e) {
                    sSuccess = false;
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        SubTest t = new SubTest();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UTest.echoClientBlock(uconn1, NUM_BLOCKS);

        // Wait for the second to finish
        t.join();

        // Get the success status of the second connection
        boolean sSuccess = t.getSuccess();

        // Validate the results
        assertTrue("echoClient should return true ", 
            cSuccess);
        assertTrue("echoServer should return true ", 
            sSuccess);
    }

    public void testOneWayTransfers() throws Exception {
        final int NUM_BYTES = 20000;

        // Start the second connection in another thread
        // and run it to completion.
        class SubTest extends Thread {
            boolean sSuccess = false;

            public void run() {
                try {
                    yield();
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UTest.unidirectionalServer(uconn2, NUM_BYTES);
                    
                } catch(Throwable e) {
                    sSuccess = false;
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        SubTest t = new SubTest();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UTest.unidirectionalClient(uconn1, NUM_BYTES);

        // Wait for the second to finish
        t.join();

        // Get the success status of the second connection
        boolean sSuccess = t.getSuccess();

        // Validate the results
        assertTrue("unidirectionalClient should return true ", 
            cSuccess);
        assertTrue("unidirectionalServer should return true ", 
            sSuccess);
    }

    public void testIOUtilsOnStream() throws Exception {
        // Start the second connection in another thread
        UDPConnection uconn2;
        ConnStarter t = new ConnStarter();
        t.setDaemon(true);
        t.start();

        // Startup connection one in original thread
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Wait for commpletion of uconn2 startup
        t.join();

        // Get the initialized connection 2
        uconn2 = t.getConnection();
        
        // Output on the first connection
        OutputStream ostream = uconn1.getOutputStream();
        ostream.write("GET FOO BAR BLECK\r\nSecond Line\r\n".getBytes());
        //uconn1.close();

        // Read to end and one extra on second stream
        InputStream  istream = uconn2.getInputStream();
        uconn2.setSoTimeout(Constants.TIMEOUT); 
        String word = IOUtils.readWord(istream,8);
        uconn2.close();

        // Validate the results
        assertTrue("Read of word should be 'GET' - is:"+word, 
          "GET".equals(word));
    }

    public void testBufferedByteReader() throws Exception {
        String line1 = "GET FOO BAR BLECK";
        String line2 = "Second Line";

        // Start the second connection in another thread
        UDPConnection uconn2;
        ConnStarter t = new ConnStarter();
        t.setDaemon(true);
        t.start();

        // Startup connection one in original thread
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Wait for commpletion of uconn2 startup
        t.join();

        // Get the initialized connection 2
        uconn2 = t.getConnection();
        
        // Output on the first connection
        OutputStream ostream = uconn1.getOutputStream();
        ostream.write((line1+"\r\n"+line2+"\r\n").getBytes());

        // Read to end and one extra on second stream
        InputStream  istream = uconn2.getInputStream();
        uconn2.setSoTimeout(Constants.TIMEOUT); 
        BufferedInputStream bistream = new BufferedInputStream(istream);
        ByteReader br = new ByteReader(bistream);

        String line = br.readLine();

        uconn1.close();
        uconn2.close();

        // Validate the results
        assertTrue("Read of line should be:"+line1, 
          line1.equals(line));
    }


    public void testReadBeyondEnd() throws Exception {
        final int NUM_BYTES = 100;

        // Start the second connection in another thread
        UDPConnection uconn2;
        ConnStarter t = new ConnStarter();
        t.setDaemon(true);
        t.start();

        // Startup connection one in original thread
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Wait for commpletion of uconn2 startup
        t.join();

        // Get the initialized connection 2
        uconn2 = t.getConnection();
        
        // Output on the first connection
        OutputStream ostream = uconn1.getOutputStream();
        for ( int i = 0; i < NUM_BYTES; i++ )
            ostream.write(i % 256);

        // Read to end and one extra on second stream
        InputStream  istream = uconn2.getInputStream();
        int rval;
        for ( int i = 0; i < NUM_BYTES; i++ ) {
            rval = istream.read();
            if ( (i % 256)  != rval )
                fail("Error on byte:"+i);
        }
        // Close writer
        uconn1.close();

        // Wait a little
        try { Thread.sleep(200); } catch (InterruptedException e) {}

        // Read from reader
        rval = istream.read();
        uconn2.close();

        // Validate the results
        assertEquals("Read at end of stream should be -1", 
            rval, -1);
    }

    public void testReadBeyondEndAsBlock() throws Exception {
        final int NUM_BYTES = 100;

        // Start the second connection in another thread
        UDPConnection uconn2;
        ConnStarter t = new ConnStarter();
        t.setDaemon(true);
        t.start();

        // Startup connection one in original thread
        final UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Wait for commpletion of uconn2 startup
        t.join();

        // Get the initialized connection 2
        uconn2 = t.getConnection();
        
        // Output on the first connection
        OutputStream ostream = uconn1.getOutputStream();
        for ( int i = 0; i < NUM_BYTES; i++ )
            ostream.write(i % 256);

        // Let data get sent to reader
        Thread.sleep(500);

        // Close writer
        uconn1.close(); 

        // Read to end and one extra on second stream
        InputStream  istream = uconn2.getInputStream();
        byte bdata[] = new byte[512];
        int rval;
        int i = 0;
        while (true) {
            int len = istream.read(bdata);
            for ( int j = 0; j < len; j++ ) {
                rval = (int)bdata[j] & 0xff;
                if ( (i % 256)  != rval )
                    fail("Error on byte:"+i);
                i++;
            }
            if (i >= NUM_BYTES)
                break;
        }

        // Read from reader
        rval = istream.read(bdata);
        uconn2.close();

        // Validate the results
        assertEquals("Read at end of stream should be -1", 
            rval, -1);
    }

    public void testReadBeyondEndAsBlockDuringRead() throws Exception {
        final int NUM_BYTES = 100;

        // Start the second connection in another thread
        UDPConnection uconn2;
        ConnStarter t = new ConnStarter();
        t.setDaemon(true);
        t.start();

        // Startup connection one in original thread
        final UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Wait for commpletion of uconn2 startup
        t.join();

        // Get the initialized connection 2
        uconn2 = t.getConnection();
        
        // Output on the first connection
        OutputStream ostream = uconn1.getOutputStream();
        for ( int i = 0; i < NUM_BYTES; i++ )
            ostream.write(i % 256);


        // Close the writer while the reader is blocked
        class SubTest extends Thread {

            public void run() {
                try {
                    // Let reader lock up on block read
                    Thread.sleep(500);

                    // Close writer
                    uconn1.close(); 
                    
                } catch(Throwable e) {
                }
            }
        }
        SubTest st = new SubTest();
        st.setDaemon(true);
        st.start();

        // Read to end and one extra on second stream
        InputStream  istream = uconn2.getInputStream();
        byte bdata[] = new byte[512];
        int rval;
        int i = 0;
        while (true) {
            int len = istream.read(bdata);
            for ( int j = 0; j < len; j++ ) {
                rval = (int)bdata[j] & 0xff;
                if ( (i % 256)  != rval )
                    fail("Error on byte:"+i);
                i++;
            }
            if (i >= NUM_BYTES)
                break;
        }

        // Read from reader
        rval = istream.read(bdata);
        uconn2.close();

        // Validate the results
        assertEquals("Read at end of stream should be -1", 
            rval, -1);
    }

    /**
     * Test that data can be written, echoed and read through flaky
     * UDPConnections.
     * 
     * @throws Exception if an error occurs
     */
    public void testFlakyConnection() throws Exception {
        final int NUM_BYTES = 200000;

        // Clear out my standard setup
        UDPServiceStub.stubInstance().clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        // Make the connections 5% flaky
        UDPServiceStub.stubInstance().addReceiver(6346, 6348, 10, 5);
        UDPServiceStub.stubInstance().addReceiver(6348, 6346, 10, 5);

        // Start the second connection in another thread
        // and run it to completion.
        class SubTest extends Thread {
            boolean sSuccess = false;

            public void run() {
                try {
                    yield();
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UTest.echoServer(uconn2, NUM_BYTES);
                    
                } catch(Throwable e) {
                    sSuccess = false;
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        SubTest t = new SubTest();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UTest.echoClient(uconn1, NUM_BYTES);

        // Wait for the second to finish
        t.join();

        // Get the success status of the second connection
        boolean sSuccess = t.getSuccess();

        // Validate the results
        assertTrue("echoClient should return true ", 
            cSuccess);
        assertTrue("echoServer should return true ", 
            sSuccess);
    }

    /**
     * Test that data can be written, echoed and read through 
     * an extrely flaky UDPConnection where 25% of messages are lost.
     * 
     * @throws Exception if an error occurs
     */
    public void testExtremelyFlakyConnection() throws Exception {
        final int NUM_BYTES = 200000;

        // Clear out my standard setup
        UDPServiceStub.stubInstance().clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        // Make the connections 25% flaky
        UDPServiceStub.stubInstance().addReceiver(6346, 6348, 10, 25);
        UDPServiceStub.stubInstance().addReceiver(6348, 6346, 10, 25);

        // Start the second connection in another thread
        // and run it to completion.
        class SubTest extends Thread {
            boolean sSuccess = false;

            public void run() {
                try {
                    yield();
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UTest.echoServer(uconn2, NUM_BYTES);
                    
                } catch(Throwable e) {
                    sSuccess = false;
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        SubTest t = new SubTest();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UTest.echoClient(uconn1, NUM_BYTES);

        // Wait for the second to finish
        t.join();

        // Get the success status of the second connection
        boolean sSuccess = t.getSuccess();

        // Validate the results
        assertTrue("echoClient should return true ", 
            cSuccess);
        assertTrue("echoServer should return true ", 
            sSuccess);
    }

    /**
     * Test UDPConnections with a very long delay.
     * 
     * @throws Exception if an error occurs
     */
    public void testExtremelySlowConnection() throws Exception {
        final int NUM_BYTES = 60000;

        // Clear out my standard setup
        UDPServiceStub.stubInstance().clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        // Make the connections 25% flaky
        UDPServiceStub.stubInstance().addReceiver(6346, 6348, 1000, 0);
        UDPServiceStub.stubInstance().addReceiver(6348, 6346, 1000, 0);

        // Start the second connection in another thread
        // and run it to completion.
        class SubTest extends Thread {
            boolean sSuccess = false;

            public void run() {
                try {
                    yield();
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UTest.echoServer(uconn2, NUM_BYTES);
                    
                } catch(Throwable e) {
                    sSuccess = false;
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        SubTest t = new SubTest();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UTest.echoClient(uconn1, NUM_BYTES);

        // Wait for the second to finish
        t.join();

        // Get the success status of the second connection
        boolean sSuccess = t.getSuccess();

        // Validate the results
        assertTrue("echoClient should return true ", 
            cSuccess);
        assertTrue("echoServer should return true ", 
            sSuccess);
    }

    /**
     *  Startup a second UDPConnection in a thread since two connections will
     *  block if started in one thread.
     */
    class ConnStarter extends ManagedThread {
        UDPConnection uconn2;

        public ConnStarter() {
        }

        public void managedRun() {
            yield();
            try {
                uconn2 = 
                  new UDPConnection("127.0.0.1",6348);
            } catch (IOException ioe) {
                ErrorService.error(ioe);
            }
        }

        public UDPConnection getConnection() {
            return uconn2;
        }
    }
}
