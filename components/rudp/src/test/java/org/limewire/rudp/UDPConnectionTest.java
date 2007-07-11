package org.limewire.rudp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import junit.framework.Test;

import org.limewire.concurrent.ManagedThread;
import org.limewire.io.IOUtils;
import org.limewire.nio.NIODispatcher;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;
import org.limewire.util.BaseTestCase;

/**
 * Put full UDPConnection system through various tests.
 */
@SuppressWarnings( { "unchecked", "cast" } )
public final class UDPConnectionTest extends BaseTestCase {
    
    private static final int TIMEOUT = 10 * 1000;

    private static UDPSelectorProviderFactory defaultFactory = null;
    private static UDPServiceStub stubService;
    private static UDPMultiplexor udpMultiplexor;

    private volatile UDPConnection uconn1;
    private volatile UDPConnection uconn2;
    
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
        defaultFactory = UDPSelectorProvider.getDefaultProviderFactory();
        RUDPMessageFactory factory = new DefaultMessageFactory();
        stubService = new UDPServiceStub(factory);
        final UDPSelectorProvider provider = new UDPSelectorProvider(new DefaultRUDPContext(
                factory, NIODispatcher.instance().getTransportListener(),
                stubService, new DefaultRUDPSettings()));
        udpMultiplexor = provider.openSelector();
        stubService.setUDPMultiplexor(udpMultiplexor);
        UDPSelectorProvider.setDefaultProviderFactory(new UDPSelectorProviderFactory() {
            public UDPSelectorProvider createProvider() {
                return provider;
            }
        });
        NIODispatcher.instance().registerSelector(udpMultiplexor, provider.getUDPSocketChannelClass());
    }

    public static void globalTearDown() throws Exception {
        UDPSelectorProvider.setDefaultProviderFactory(defaultFactory);
        NIODispatcher.instance().removeSelector(udpMultiplexor);
    }

    public void setUp() throws Exception {
        if (defaultFactory == null) {
            globalSetUp();
        }
        
        // Add some simulated connections to the UDPServiceStub
        stubService.addReceiver(6346, 6348, 10, 0);
        stubService.addReceiver(6348, 6346, 10, 0);
    }
    
    public void tearDown() throws Exception {
        if (uconn1 != null) {
            uconn1.shutdown();
        }
        if (uconn2 != null) {
            uconn2.shutdown();
        }
        
        // Clear out the receiver parameters for the UDPServiceStub
        stubService.clearReceivers();
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
            public void run() {
                try {
                    uconn2 = new UDPConnection("127.0.0.1", 6348);
                    UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        t.setName("EchoServer");
        try {
            t.start();

            // Init the first connection
            uconn1 = new UDPConnection("127.0.0.1",6346);

            // Run the first connection
            UStandalone.echoClient(uconn1, NUM_BYTES);
        } finally {
            // Wait for the second to finish
            t.join();
        }
    }

    public void testBlockTransfers() throws Exception {
		final int NUM_BLOCKS = 100;

        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            public void run() {
                try {
                    uconn2 = new UDPConnection("127.0.0.1", 6348);
                    UStandalone.echoServerBlock(uconn2, NUM_BLOCKS);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try{
            t.start();

            // Init the first connection
            uconn1 = new UDPConnection("127.0.0.1",6346);

            // Run the first connection
            UStandalone.echoClientBlock(uconn1, NUM_BLOCKS);
        } finally {
            // Wait for the second to finish
            t.join();
        }
    }

    public void testOneWayTransfers() throws Exception {
        final int NUM_BYTES = 20000;

        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            public void run() {
                try {
                    uconn2 = new UDPConnection("127.0.0.1", 6348);
                    UStandalone.unidirectionalServer(uconn2,
                            NUM_BYTES);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // Init the first connection
            uconn1 = new UDPConnection("127.0.0.1",6346);

            // Run the first connection
            UStandalone.unidirectionalClient(uconn1, NUM_BYTES);
        } finally {
            // Wait for the second to finish
            t.join();
        }
    }

    public void testIOUtilsOnStream() throws Exception {
        // initialize connection
        ConnStarter starter = new ConnStarter();
        starter.connect();        
        
        // Output on the first connection
        OutputStream ostream = uconn1.getOutputStream();
        ostream.write("GET FOO BAR BLECK\r\nSecond Line\r\n".getBytes());
        //uconn1.close();

        // Read to end and one extra on second stream
        InputStream  istream = uconn2.getInputStream();
        uconn2.setSoTimeout(TIMEOUT); 
        String word = IOUtils.readWord(istream,8);

        assertEquals("GET", word);
    }

    public void testBufferedByteReader() throws Exception {
        String line1 = "GET FOO BAR BLECK";
        String line2 = "Second Line";

        // initialize connection
        ConnStarter starter = new ConnStarter();
        starter.connect();

        // Output on the first connection
        OutputStream ostream = uconn1.getOutputStream();
        ostream.write((line1 + "\r\n" + line2 + "\r\n").getBytes());

        // Read to end and one extra on second stream
        InputStream istream = uconn2.getInputStream();
        uconn2.setSoTimeout(TIMEOUT);
        BufferedReader br = new BufferedReader(new InputStreamReader(istream));
        String line = br.readLine();
        assertEquals(line1, line);
    }


    public void testReadBeyondEnd() throws Exception {
        final int NUM_BYTES = 100;

        // initialize connection
        ConnStarter starter = new ConnStarter();
        starter.connect();        
        
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

        // initialize connection
        ConnStarter starter = new ConnStarter();
        starter.connect();        
        
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

        // initialize connection
        ConnStarter starter = new ConnStarter();
        starter.connect();        
        
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
                } catch(InterruptedException ie) {
                }

                // Close writer
                uconn1.close(); 
            }
        }
        Inner st = new Inner();
        st.setDaemon(true);
        try {
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
        } finally {
            st.join();
        }
    }

    /**
     * Test that data can be written, echoed and read through 
     * UDPConnections.
     */
    public void testConnection() throws Exception {
        final int NUM_BYTES = 10000000;

        // Clear out my standard setup
        stubService.clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        // Make the connections 5% flaky
        stubService.addReceiver(6346, 6348, 0, 0);
        stubService.addReceiver(6348, 6346, 0, 0);

        // start the first connection in another thread
        class Inner extends ManagedThread {
            public void run() {
                try {
                    uconn1 = new UDPConnection("127.0.0.1", 6348);
                    uconn1.setSoTimeout(TIMEOUT);
                    UStandalone.echoServer(uconn1, NUM_BYTES);                   
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // start the second connection
            uconn2 = new UDPConnection("127.0.0.1", 6346);
            uconn2.setSoTimeout(TIMEOUT);
            UStandalone.echoClient(uconn2, NUM_BYTES);
        } finally {
            t.join();
        }
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
        stubService.clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        // Make the connections 5% flaky
        stubService.addReceiver(6346, 6348, 10, 5);
        stubService.addReceiver(6348, 6346, 10, 5);

        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            public void run() {
                try {
                    uconn1 = new UDPConnection("127.0.0.1",6348);
                    uconn1.setSoTimeout(TIMEOUT);
                    UStandalone.echoServer(uconn1, NUM_BYTES);                   
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // Start the first connection
            uconn2 = new UDPConnection("127.0.0.1",6346);
            uconn2.setSoTimeout(TIMEOUT);
            UStandalone.echoClient(uconn2, NUM_BYTES);
        } finally {
            // Wait for the second to finish
            t.join();
        }
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
        stubService.clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        // Make the connections 15% flaky
        stubService.addReceiver(6346, 6348, 10, 10);
        stubService.addReceiver(6348, 6346, 10, 10);

        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            public void run() {
                try {
                    uconn2 = new UDPConnection("127.0.0.1", 6348);
                    uconn2.setSoTimeout(TIMEOUT);
                    UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // Init the first connection
            uconn1 = new UDPConnection("127.0.0.1",6346);
            uconn1.setSoTimeout(TIMEOUT);
            
            // Run the first connection
            UStandalone.echoClient(uconn1, NUM_BYTES);
        } finally {
            // Wait for the second to finish
            t.join();
        }
    }

    /**
     * Test UDPConnections with a very long delay.
     * 
     * @throws Exception if an error occurs
     */
    public void testExtremelySlowConnection() throws Exception {
        final int NUM_BYTES = 60000;

        // Clear out my standard setup
        stubService.clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        // Make the connections 25% flaky
        stubService.addReceiver(6346, 6348, 1000, 0);
        stubService.addReceiver(6348, 6346, 1000, 0);

        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            public void run() {
                try {
                    uconn2 = new UDPConnection("127.0.0.1", 6348);
                    uconn2.setSoTimeout(TIMEOUT);
                    UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // Init the first connection
            uconn1 = new UDPConnection("127.0.0.1",6346);
            uconn1.setSoTimeout(TIMEOUT);
            
            // Run the first connection
            UStandalone.echoClient(uconn1, NUM_BYTES);
        } finally {
            // Wait for the second to finish
            t.join();
        }
    }

    /**
     * Startup two connections. The second UDPConnection is started in a thread
     * since two connections will block if started in one thread.
     * <p>
     * Connections are assigned to fields in {@link UDPConnectionTest}.  
     */
    private class ConnStarter {
        
        public ConnStarter() {
        }

        public void connect() throws Exception {
            Thread t = new ManagedThread(new Runnable() {
                public void run() {
                    try {
                        UDPConnectionTest.this.uconn2 = new UDPConnection("127.0.0.1", 6348);
                    } catch (IOException e) {
                        fail("Error establishing UDP connection to port 6348", e);
                    }
                }
            });
            t.setDaemon(true);
                    
            try {
                t.start();
                
                // startup connection one in original thread
                UDPConnectionTest.this.uconn1 = new UDPConnection("127.0.0.1", 6346);
            } finally {
                t.join();
            }
        }
        
    }
    
}
