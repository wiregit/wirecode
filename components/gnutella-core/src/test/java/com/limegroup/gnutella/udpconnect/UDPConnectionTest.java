package com.limegroup.gnutella.udpconnect;

import junit.framework.Test;

import com.limegroup.gnutella.util.BaseTestCase;
import com.limegroup.gnutella.UDPServiceStub;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;

/**
 * Tests the SequenceNumberExtender class.
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
    
    /**
     * Test that 
	 * 
     * 
     * @throws Exception if an error occurs
     */
    public void testBasics() throws Exception {
		new RouterService(new ActivityCallbackStub());
		final int NUM_BYTES = 2000;

		try {
			// Setup the test to use the UDPServiceStub
			UDPConnectionProcessor.setUDPServiceForTesting(
				UDPServiceStub.instance());

			// Add some simulated connections to the UDPServiceStub
			UDPServiceStub.stubInstance().addReceiver(6346, 6348, 10);
			UDPServiceStub.stubInstance().addReceiver(6348, 6346, 10);

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

		} finally {
			// Reset the UDPServiceStub usage from this test
			UDPConnectionProcessor.setUDPServiceForTesting(null);
			UDPServiceStub.stubInstance().clearReceivers();
		}
    }

    public void testBlockTransfers() throws Exception {
		new RouterService(new ActivityCallbackStub());
		final int NUM_BLOCKS = 10;

		try {
			// Setup the test to use the UDPServiceStub
			UDPConnectionProcessor.setUDPServiceForTesting(
				UDPServiceStub.instance());

			// Add some simulated connections to the UDPServiceStub
			UDPServiceStub.stubInstance().addReceiver(6346, 6348, 10);
			UDPServiceStub.stubInstance().addReceiver(6348, 6346, 10);

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

		} finally {
			// Reset the UDPServiceStub usage from this test
			UDPConnectionProcessor.setUDPServiceForTesting(null);
			UDPServiceStub.stubInstance().clearReceivers();
		}
    }
}
