package com.limegroup.gnutella;

import junit.framework.*;
import java.net.*;
import java.io.*;
import java.util.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.guess.*;
import com.limegroup.gnutella.stubs.*;

public class QueryUnicasterTest extends com.limegroup.gnutella.util.BaseTestCase {

	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

	private final int SOCKET_TIMEOUT = 2*1000; // 2 second wait for a message

    private final int NUM_UDP_LOOPS = 25;

    private boolean _shouldRun = true;
    private boolean shouldRun() {
        return _shouldRun;
    }
    
    private static boolean anError = false;

    static {
        
        try {
    		Backend backend = Backend.createBackend(40*1000);
            backend.start();
        } catch (Throwable t) {
            anError = true;
        }
    }


    // produces should add(), consumers should firstElement()
    private Vector _messages = new Vector();
    
    public QueryUnicasterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(QueryUnicasterTest.class);
    }
    
    public void setUp() {
        if ( anError ) fail( "error during Backend creation" );
    }

    
    public void testConstruction() {
        QueryUnicaster qu = QueryUnicaster.instance();
        assertTrue(qu.getUnicastEndpoints().size() == 0);
    }


    public void testQueries() {
        // start udp hosts....
        Thread[] udpLoopers = new Thread[NUM_UDP_LOOPS];
        for (int i = 0; i < NUM_UDP_LOOPS; i++) {
            final int index = i;
            udpLoopers[i] = new Thread() {
                    public void run() {
                        udpLoop(5000 + index);
                    }
                };
            udpLoopers[i].start();
        }

        // add these endpoints....
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName("127.0.0.1");
        }
        catch (Exception damn) {
            assertTrue(false);
        }
        for (int i = 0; i < NUM_UDP_LOOPS; i++) 
            QueryUnicaster.instance().addUnicastEndpoint(addr, 5000+i);

        // add a Query
        QueryRequest qr = new QueryRequest((byte)2, 0, "Susheel", false);
        assertTrue(QueryUnicaster.instance().getQueryNumber() == 0);
        QueryUnicaster.instance().addQuery(qr, null);
        assertTrue(QueryUnicaster.instance().getQueryNumber() == 1);

        // give udpLoopers time to execute
        // get messages from vector, should be a message or a ping
        // wait some seconds for thread to do work.  this is not scientific 
        // but should do the job...
        try {
            Thread.sleep(5 * 1000);
        }
        catch (InterruptedException ignored) {}
        int numMessages = 0, numQRs = 0, numPings = 0, numQKReqs = 0;
        while (!_messages.isEmpty()) {
            Message currMessage = (Message) _messages.remove(0);
            numMessages++;
            if (currMessage instanceof QueryRequest) {
                QueryRequest currQR = (QueryRequest) currMessage;
                assertTrue(currQR.getQuery().equals("Susheel"));
                numQRs++;
            }
            else if (currMessage instanceof PingRequest) {
                numPings++;
                if (((PingRequest)currMessage).isQueryKeyRequest())
                    numQKReqs++;
            }
            else
                assertTrue("A different message encountered! : " + 
                           currMessage, 
                           false);  // this should never happen!
        }
        assertTrue(numMessages == (numPings + numQRs));
        // can't send a Query without sending a Ping....
        assertTrue(numQRs <= numPings);
        assertTrue(numQRs > 0);
        assertTrue(numQRs <= numQKReqs);
        debug("QueryUnicasterTest.testQueries(): numMessages = " +
              numMessages);
        debug("QueryUnicasterTest.testQueries(): numQRs = " +
              numQRs);
        debug("QueryUnicasterTest.testQueries(): numPings = " +
              numPings);

        // shut off udp listeners....
        _shouldRun = false;
        for (int i = 0; i < NUM_UDP_LOOPS; i++)
            udpLoopers[i].interrupt();

        // wait for them to stop...
        try {
            Thread.sleep(2 * 1000);
        }
        catch (InterruptedException ignored) {}
        
        // get rid of old query...
        QueryReply qRep = generateFakeReply(qr.getGUID(), 251);
        QueryUnicaster.instance().handleQueryReply(qRep);        
    }



    public void testResultMaxOut() {
        // clear out messages...
        _messages.clear();
        // start up threads...
        _shouldRun = true;

        // start udp hosts....
        Thread[] udpLoopers = new Thread[NUM_UDP_LOOPS];
        for (int i = 0; i < NUM_UDP_LOOPS; i++) {
            final int index = i;
            udpLoopers[i] = new Thread() {
                    public void run() {
                        udpLoop(5500 + index);
                    }
                };
            udpLoopers[i].start();
        }

        // add a Query
        QueryRequest qr = new QueryRequest((byte)2, 0, "Daswani", false);
        QueryUnicaster.instance().addQuery(qr, null);

        // add these endpoints....
        InetAddress addr = null;
        try {
            addr = InetAddress.getByName("127.0.0.1");
        }
        catch (Exception damn) {
            assertTrue(false);
        }
        for (int i = 0; i < NUM_UDP_LOOPS; i++) {
            QueryUnicaster.instance().addUnicastEndpoint(addr, 5000+i);
            if (i % 5 == 0) {
                try {
                    // give some time for queries to get out...
                    Thread.sleep(200);
                }
                catch (InterruptedException ignored) {}
            }
            
            //add some results...
            QueryReply qRep = generateFakeReply(qr.getGUID(),
                                                getNumberBetween(25, 35));
            QueryUnicaster.instance().handleQueryReply(qRep);
        }

        // give udpLoopers time to execute
        // get messages from vector, should be a message or a ping
        // wait some seconds for thread to do work.  this is not scientific 
        // but should do the job...
        try {
            Thread.sleep(3 * 1000);
            assertTrue(QueryUnicaster.instance().getQueryNumber() == 0);
        }
        catch (InterruptedException ignored) {}
        int numMessages = 0, numQRs = 0, numPings = 0;
        while (!_messages.isEmpty()) {
            Message currMessage = (Message) _messages.remove(0);
            numMessages++;
            if (currMessage instanceof QueryRequest) {
                QueryRequest currQR = (QueryRequest) currMessage;
                assertTrue(currQR.getQuery().equals("Daswani"));
                numQRs++;
            }
            else if (currMessage instanceof PingRequest) {
                numPings++;
            }
            else
                assertTrue("A different message encountered! : " + 
                           currMessage, 
                           false);  // this should never happen!
        }
        assertTrue(numMessages == (numPings + numQRs));
        assertTrue("numQRs = " + numQRs, numQRs < 11); // 15 * 25 >> 250
        assertTrue(QueryUnicaster.instance().getUnicastEndpoints().size() >
                   0);
        debug("QueryUnicasterTest.testQueries(): numMessages = " +
              numMessages);
        debug("QueryUnicasterTest.testQueries(): numQRs = " +
              numQRs);
        debug("QueryUnicasterTest.testQueries(): numPings = " +
              numPings);

        // shut off udp listeners....
        _shouldRun = false;
        for (int i = 0; i < NUM_UDP_LOOPS; i++) 
            udpLoopers[i].interrupt();

        // wait for them to stop...
        try {
            Thread.sleep(2 * 1000);
        }
        catch (InterruptedException ignored) {}
    }



    private QueryReply generateFakeReply(byte[] guid, int numResponses) {
        Response[] resps = new Response[numResponses];
        for (int i = 0; i< resps.length; i++)
            resps[i] = new Response(i, i, ""+i);
        byte[] ip = {(byte)127, (byte)0, (byte)0, (byte)1};
        QueryReply toReturn = new QueryReply(guid,
                                             (byte) 2,
                                             0, ip, 0, resps,
                                             GUID.makeGuid());
        return toReturn;
    }


    /** returns a number from low to high (both inclusive).
     */
    private int getNumberBetween(int low, int high) {
        Random rand = new Random();
        int retInt = low - 1;
        while (retInt < low)
            retInt = rand.nextInt(high+1);
        return retInt;
    }
        

    private static byte[] localhost = {(byte)127,(byte)0,(byte)0,(byte)1};
        
	/**
     * Busy loop that listens on a port for udp messages and then logs them.
	 */
	private void udpLoop(int port) {
        DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(port);
            socket.setSoTimeout(1000);
            debug("QueryUnicasterTest.udpLoop(): listening on port " +
                  port);
			//socket.setSoTimeout(SOCKET_TIMEOUT);
		} 
        catch (SocketException e) {
            debug("QueryUnicasterTest.udpLoop(): couldn't listen on port " +
                  port);
			return;
		}
        catch (RuntimeException e) {
            debug("QueryUnicasterTest.udpLoop(): couldn't listen on port " +
                  port);
			return;
		}
		
		
		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                     BUFFER_SIZE);
        QueryKey.SecretKey key = QueryKey.generateSecretKey();
        QueryKey.SecretPad pad = QueryKey.generateSecretPad();
        while (shouldRun()) {
            try {				
                socket.receive(datagram);
                byte[] data = datagram.getData();
                int length = datagram.getLength();
                try {
                    // construct a message out of it...
                    InputStream in = new ByteArrayInputStream(data);
                    Message message = Message.read(in);		
                    if(message == null) continue;
                    if (message instanceof PingRequest) {
                        PingRequest pr = (PingRequest)message;
                        pr.hop();  // need to hop it!!
                        if (pr.isQueryKeyRequest()) {
                            // send a QueryKey back!!!
                            QueryKey qk = 
                                QueryKey.getQueryKey(datagram.getAddress(),
                                                     datagram.getPort(),
                                                     key, pad);
                            PingReply pRep = new PingReply(pr.getGUID(),
                                                           (byte)1,
                                                           port,
                                                           localhost,
                                                           2, 2, true, qk);
                            pRep.hop();
                            debug("QueryUnicasterTest.udpLoop(): sending QK.");
                            QueryUnicaster.instance().handleQueryKeyPong(pRep);
                        }
                    }
                    // log the message....
                    synchronized (_messages) {
                        _messages.add(message);
                        _messages.notify();
                    }
                } catch(BadPacketException e) {
                    continue;
                }
            } 
            catch (InterruptedIOException e) {
                continue;
            } 
            catch (IOException e) {
                continue;
            }
		}
        debug("QueryUnicasterTest.udpLoop(): closing down port " +
              port);
		socket.close();
	}


    private final static boolean debugOn = false;
    private final static void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }
    private final static void debug(Exception out) {
        if (debugOn)
            out.printStackTrace();
    }



}
