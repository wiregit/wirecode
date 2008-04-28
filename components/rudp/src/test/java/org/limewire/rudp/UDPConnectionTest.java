package org.limewire.rudp;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.concurrent.ManagedThread;
import org.limewire.io.IOUtils;
import org.limewire.nio.AbstractNBSocket;
import org.limewire.nio.NIODispatcher;
import org.limewire.rudp.messages.RUDPMessageFactory;
import org.limewire.rudp.messages.impl.DefaultMessageFactory;
import org.limewire.util.BaseTestCase;

/**
 * Put full UDPConnection system through various tests.
 */
public final class UDPConnectionTest extends BaseTestCase {

    private static final int TIMEOUT = 10 * 1000;

    private volatile UDPServiceStub stubService;
    private volatile UDPMultiplexor udpMultiplexor;
    private volatile UDPSelectorProvider udpSelectorProvider;

    private volatile AbstractNBSocket uconn1;
    private volatile AbstractNBSocket uconn2;
    
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

    @Override
    public void setUp() throws Exception {
        RUDPMessageFactory factory = new DefaultMessageFactory();
        stubService = new UDPServiceStub(factory);
        udpSelectorProvider = new UDPSelectorProvider(new DefaultRUDPContext(
                factory, NIODispatcher.instance().getTransportListener(),
                stubService, new DefaultRUDPSettings()));
        udpMultiplexor = udpSelectorProvider.openSelector();
        stubService.setUDPMultiplexor(udpMultiplexor);
        NIODispatcher.instance().registerSelector(udpMultiplexor,
                udpSelectorProvider.getUDPSocketChannelClass());
        
        // Add some simulated connections to the UDPServiceStub
        stubService.addReceiver(6346, 6348, 10, 0);
        stubService.addReceiver(6348, 6346, 10, 0);
    }
    
    @Override
    public void tearDown() throws Exception {
        if (uconn1 != null) {
            uconn1.shutdown();
        }
        if (uconn2 != null) {
            uconn2.shutdown();
        }
        
        // Clear out the receiver parameters for the UDPServiceStub
        stubService.clearReceivers();
        NIODispatcher.instance().removeSelector(udpMultiplexor);
    }  

    /**
     * Test that data can be written, echoed and read through the
	 * UDPConnections.
     * 
     * @throws Exception if an error occurs
     */
    public void testBasics() throws Exception {
		final int NUM_BYTES = 20000;

		final CountDownLatch threadEnder = new CountDownLatch(1);
        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            @Override
            public void run() {
                try {
                    uconn2 = udpSelectorProvider.openSocketChannel().socket();
                    uconn2.connect(new InetSocketAddress("127.0.0.1", 6348));
                    UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } finally {
                    threadEnder.countDown();
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        t.setName("EchoServer");
        try {
            t.start();

            // Init the first connection
            uconn1 = udpSelectorProvider.openSocketChannel().socket();
            uconn1.connect(new InetSocketAddress("127.0.0.1", 6346));

            // Run the first connection
            UStandalone.echoClient(uconn1, NUM_BYTES);
        } finally {
            // Wait for the second to finish
            assertTrue(threadEnder.await(2000 * 60, TimeUnit.MILLISECONDS));
        }
    }

    public void testBlockTransfers() throws Exception {
		final int NUM_BLOCKS = 100;

        // Start the second connection in another thread
        // and run it to completion.

        final CountDownLatch threadEnder = new CountDownLatch(1);
        class Inner extends ManagedThread {
            @Override
            public void run() {
                try {
                    uconn2 = udpSelectorProvider.openSocketChannel().socket();
                    uconn2.connect(new InetSocketAddress("127.0.0.1", 6348));
                    UStandalone.echoServerBlock(uconn2, NUM_BLOCKS);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } finally {
                    threadEnder.countDown();
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try{
            t.start();

            // Init the first connection
            uconn1 = udpSelectorProvider.openSocketChannel().socket();
            uconn1.connect(new InetSocketAddress("127.0.0.1", 6346));

            // Run the first connection
            UStandalone.echoClientBlock(uconn1, NUM_BLOCKS);
        } finally {
            // Wait for the second to finish
            assertTrue(threadEnder.await(2000 * 60, TimeUnit.MILLISECONDS));
        }
    }

    /**
     * Test that transfers data from a sender to a receiver, comparing 4 bytes at a time.
     */
    public void testOneWayTransfer() throws Exception {
        final int MAX_VALUE = 1 * 1000 * 1000;

        // Clear out my standard setup
        stubService.clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        // Make the connections 5% flaky
        stubService.addReceiver(6346, 6348, 1, 0);
        stubService.addReceiver(6348, 6346, 1, 0);
        final CountDownLatch threadEnder = new CountDownLatch(1);

        // start the first connection in another thread
        class Inner extends ManagedThread {
            @Override
            public void run() {
                try {
                    uconn1 = udpSelectorProvider.openSocketChannel().socket();
                    uconn1.connect(new InetSocketAddress("127.0.0.1", 6348), 5000);
                    uconn1.setSoTimeout(TIMEOUT);
                    InputStream  istream = uconn1.getInputStream();
                    for (int i = 0; i < MAX_VALUE; i++) {
                        int rval = readInt(istream);
                        assertEquals("Unexpected data at offset: " + i, Integer.toHexString(i), Integer.toHexString(rval));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    threadEnder.countDown();
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // start the second connection
            uconn2 = udpSelectorProvider.openSocketChannel().socket();
            uconn2.connect(new InetSocketAddress("127.0.0.1", 6346), 5000);
            uconn2.setSoTimeout(TIMEOUT);
            OutputStream ostream = uconn2.getOutputStream();
            for (int i = 0; i < MAX_VALUE; i++) {
                writeInt(i, ostream);
            }
        } finally {
            assertTrue(threadEnder.await(2000 * 60, TimeUnit.MILLISECONDS));
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
                rval = bdata[j] & 0xff;
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


        final CountDownLatch threadEnder = new CountDownLatch(1);
        // Close the writer while the reader is blocked
        class Inner extends ManagedThread {

            @Override
            public void run() {
                try {
                    // Let reader lock up on block read
                    Thread.sleep(500);

                    // Close writer
                    uconn1.close(); 
                } catch(InterruptedException ie) {
                } finally {
                    threadEnder.countDown();
                }

                
                
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
                    rval = bdata[j] & 0xff;
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
            assertTrue(threadEnder.await(2000 * 60, TimeUnit.MILLISECONDS));
        }
    }

    /**
     * Test that data can be written, echoed and read through 
     * UDPConnections.
     */
    public void testConnection() throws Exception {
        final int NUM_BYTES = 10 * 1000 * 1000;

        // Clear out my standard setup
        stubService.clearReceivers();

        // Add some simulated connections to the UDPServiceStub
        stubService.addReceiver(6346, 6348, 0, 0);
        stubService.addReceiver(6348, 6346, 0, 0);
        

        final CountDownLatch threadEnder = new CountDownLatch(1);

        // start the first connection in another thread
        class Inner extends ManagedThread {
            @Override
            public void run() {
                try {
                    uconn1 = udpSelectorProvider.openSocketChannel().socket();
                    uconn1.connect(new InetSocketAddress("127.0.0.1", 6348), 2000);
                    uconn1.setSoTimeout(TIMEOUT);
                    UStandalone.echoServer(uconn1, NUM_BYTES);                   
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    threadEnder.countDown();
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // start the second connection
            uconn2 = udpSelectorProvider.openSocketChannel().socket();
            uconn2.connect(new InetSocketAddress("127.0.0.1", 6346), 2000);
            uconn2.setSoTimeout(TIMEOUT);
            UStandalone.echoClient(uconn2, NUM_BYTES);
        } finally {
            assertTrue(threadEnder.await(2000 * 60, TimeUnit.MILLISECONDS));
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
        

        final CountDownLatch threadEnder = new CountDownLatch(1);

        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            @Override
            public void run() {
                try {
                    uconn1 = udpSelectorProvider.openSocketChannel().socket();
                    uconn1.connect(new InetSocketAddress("127.0.0.1", 6348), 2000);
                    uconn1.setSoTimeout(TIMEOUT);
                    UStandalone.echoServer(uconn1, NUM_BYTES);                   
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    threadEnder.countDown();
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // Start the first connection
            uconn2 = udpSelectorProvider.openSocketChannel().socket();
            uconn2.connect(new InetSocketAddress("127.0.0.1", 6346), 2000);
            uconn2.setSoTimeout(TIMEOUT);
            UStandalone.echoClient(uconn2, NUM_BYTES);
        } finally {
            // Wait for the second to finish
            assertTrue(threadEnder.await(2000 * 60, TimeUnit.MILLISECONDS));
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


        final CountDownLatch threadEnder = new CountDownLatch(1);
        
        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            @Override
            public void run() {
                try {
                    uconn2 = udpSelectorProvider.openSocketChannel().socket();
                    uconn2.connect(new InetSocketAddress("127.0.0.1", 6348), 2000);
                    uconn2.setSoTimeout(TIMEOUT);
                    UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } finally {
                    threadEnder.countDown();
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // Init the first connection
            uconn1 = udpSelectorProvider.openSocketChannel().socket();
            uconn1.connect(new InetSocketAddress("127.0.0.1", 6346), 2000);
            uconn1.setSoTimeout(TIMEOUT);
            
            // Run the first connection
            UStandalone.echoClient(uconn1, NUM_BYTES);
        } finally {
            // Wait for the second to finish
            assertTrue(threadEnder.await(2000 * 60, TimeUnit.MILLISECONDS));
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

        final CountDownLatch threadEnder = new CountDownLatch(1);

        // Start the second connection in another thread
        // and run it to completion.
        class Inner extends ManagedThread {
            @Override
            public void run() {
                try {
                    uconn2 = udpSelectorProvider.openSocketChannel().socket();
                    uconn2.connect(new InetSocketAddress("127.0.0.1", 6348), 10000);
                    uconn2.setSoTimeout(TIMEOUT);
                    UStandalone.echoServer(uconn2, NUM_BYTES);
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                } finally {
                    threadEnder.countDown();
                }
            }
        }
        Inner t = new Inner();
        t.setDaemon(true);
        try {
            t.start();

            // Init the first connection
            uconn1 = udpSelectorProvider.openSocketChannel().socket();
            uconn1.connect(new InetSocketAddress("127.0.0.1", 6346), 10000);
            uconn1.setSoTimeout(TIMEOUT);
            
            // Run the first connection
            UStandalone.echoClient(uconn1, NUM_BYTES);
        } finally {
            // Wait for the second to finish
            assertTrue(threadEnder.await(2000 * 60, TimeUnit.MILLISECONDS));
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

            final CountDownLatch threadEnder = new CountDownLatch(1);
            
            Thread t = new ManagedThread(new Runnable() {
                public void run() {
                    try {
                        uconn2 = udpSelectorProvider.openSocketChannel().socket();
                        uconn2.connect(new InetSocketAddress("127.0.0.1", 6348), 2000);
                    } catch (IOException e) {
                        fail("Error establishing UDP connection to port 6348", e);
                    } finally {
                        threadEnder.countDown();
                    }
                }
            });
            t.setDaemon(true);
                    
            try {
                t.start();
                
                // startup connection one in original thread
                uconn1 = udpSelectorProvider.openSocketChannel().socket();
                uconn1.connect(new InetSocketAddress("127.0.0.1", 6346), 2000);
            } finally {
                assertTrue(threadEnder.await(2000 * 60, TimeUnit.MILLISECONDS));
            }
        }
        
    }

    /**
     * Reads an int from <code>is</code> in big-endian byte-order.
     */
    private int readInt(InputStream is) throws IOException {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            int read = is.read();  
            if (read == -1) {
                throw new EOFException();
            }
            result |= read << 8 * (3 - i); 
        }
        return result;
    }

    /**
     * Writes an int to <code>os</code> in big-endian byte-order.
     */
    private void writeInt(int x, OutputStream os) throws IOException {
        os.write((byte)(x >> 24));
        os.write((byte)(x >> 16));
        os.write((byte)(x >>  8));
        os.write((byte) x       );
    }

}
