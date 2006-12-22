package com.limegroup.gnutella.udpconnect;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import org.limewire.concurrent.ManagedThread;
import org.limewire.io.IOUtils;
import org.limewire.service.ErrorService;

import junit.framework.Test;

import com.limegroup.gnutella.Acceptor;
import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UDPServiceStub;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Put full UDPConnection system through various tests.
 */
public final class UDPConnectionTest extends LimeTestCase {

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
        UDPConnectionProcessor.setUDPServiceForTesting(UDPServiceStub.instance());
        new RouterService(new ActivityCallbackStub());
        setSettings();
    }
    
    private static void setSettings() throws Exception {
        Acceptor ac = RouterService.getAcceptor();
        ac.setAddress(InetAddress.getByName("127.0.0.1"));
        ac.setExternalAddress(InetAddress.getByName("127.0.0.1"));
        ConnectionSettings.LOCAL_IS_PRIVATE.setValue(false); 
        ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
        ConnectionSettings.FORCED_IP_ADDRESS_STRING.setValue("127.0.0.1");
    }

    public static void globalTearDown() throws Exception {
        // Cleanup the UDPServiceStub usage
        UDPConnectionProcessor.setUDPServiceForTesting(null);
    }

    public void setUp() throws Exception {
        setSettings();

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
        class Inner extends ManagedThread {
            boolean sSuccess = false;

            public void run()  {
                yield();
                try {
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        Inner t = new Inner();
        t.setName("EchoServer");
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);
        
        // Run the first connection
        boolean cSuccess = UStandalone.echoClient(uconn1, NUM_BYTES);

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
        class Inner extends ManagedThread {
            boolean sSuccess = false;

            public void run() {
                yield();
                try {
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UStandalone.echoServerBlock(uconn2, NUM_BLOCKS);
                } catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UStandalone.echoClientBlock(uconn1, NUM_BLOCKS);

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
        class Inner extends ManagedThread {
            boolean sSuccess = false;

            public void run() {
                yield();
                try {
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UStandalone.unidirectionalServer(uconn2, NUM_BYTES);
                } catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UStandalone.unidirectionalClient(uconn1, NUM_BYTES);

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

        // Read from reader
        rval = istream.read();

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
        class Inner extends ManagedThread {

            public void run() {
                try {
                    // Let reader lock up on block read
                    Thread.sleep(500);
    
                    // Close writer
                    uconn1.close(); 
                        
                } catch(InterruptedException ie) {
                }
            }
        }
        Inner st = new Inner();
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
        class Inner extends ManagedThread {
            boolean sSuccess = false;

            public void run() {
                yield();
                try {
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UStandalone.echoClient(uconn1, NUM_BYTES);

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
     * an extrely flaky UDPConnection where 15% of messages are lost.
     * 
     * @throws Exception if an error occurs
     */
    public void testExtremelyFlakyConnection() throws Exception {
        final int NUM_BYTES = 20000;

        // Clear out my standard setup
        UDPServiceStub.stubInstance().clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        // Make the connections 15% flaky
        UDPServiceStub.stubInstance().addReceiver(6346, 6348, 10, 10);
        UDPServiceStub.stubInstance().addReceiver(6348, 6346, 10, 10);

        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            boolean sSuccess = false;

            public void run() {
                yield();
                try {
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UStandalone.echoClient(uconn1, NUM_BYTES);

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
        class Inner extends ManagedThread {
            boolean sSuccess = false;

            public void run() {
                yield();
                try {
                    UDPConnection uconn2 = 
                      new UDPConnection("127.0.0.1",6348);
                    sSuccess = UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch(IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }

            public boolean getSuccess() {
                return sSuccess;
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        t.start();

        // Init the first connection
        UDPConnection uconn1 = new UDPConnection("127.0.0.1",6346);

        // Run the first connection
        boolean cSuccess = UStandalone.echoClient(uconn1, NUM_BYTES);

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

        public void run() {
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
