package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;
import java.util.Vector;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.security.AddressSecurityToken;
import org.limewire.util.PrivilegedAccessor;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.stubs.ActivityCallbackStub;
import com.limegroup.gnutella.stubs.MessageRouterStub;

@SuppressWarnings("unchecked")
public class QueryUnicasterTest extends com.limegroup.gnutella.util.LimeTestCase {
    private static final Log LOG = LogFactory.getLog(QueryUnicasterTest.class);
    
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

    private final int NUM_UDP_LOOPS = 25;
    
    private static RouterService _rs;

    private boolean _shouldRun = true;
    private boolean shouldRun() {
        return _shouldRun;
    }


    // produces should add(), consumers should firstElement()
    private Vector _messages = new Vector();
    
    public QueryUnicasterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(QueryUnicasterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public static void globalSetUp() throws Exception {
        _rs = new RouterService(new
                ActivityCallbackStub(),
                new MessageRouterStub());
        _rs.start();
    }
    
    public void setUp() throws Exception {
        QueryUnicaster qu = ProviderHacks.getQueryUnicaster();        
        PrivilegedAccessor.invokeMethod(qu, "resetUnicastEndpointsAndQueries");
       
        ConnectionSettings.DO_NOT_BOOTSTRAP.setValue(true);
        ConnectionSettings.CONNECT_ON_STARTUP.setValue(false);
    }
    
    protected void tearDown() throws Exception {
    }

    public void testConstruction() {
        QueryUnicaster qu = ProviderHacks.getQueryUnicaster();
        
        assertEquals("unexpected amount of unicast endpoints",
            0, qu.getUnicastEndpoints().size());
    }

    public void testQueries() throws Exception {
        _messages.clear();
        _shouldRun=true;

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
            Thread.yield(); // let it run.
        }

        // add these endpoints....
        InetAddress addr = null;
        addr = InetAddress.getByName("127.0.0.1");
        for (int i = 0; i < NUM_UDP_LOOPS; i++)  {
            ProviderHacks.getQueryUnicaster().addUnicastEndpoint(addr, 5000+i);
            if (i % 5 == 0) {
                try {
                    // give some time for queries to get out...
                    Thread.sleep(200);
                }
                catch (InterruptedException ignored) {}
            }
        }            

        // add a Query
		QueryRequest qr = ProviderHacks.getQueryRequestFactory().createQuery("susheel", (byte)2);
        
		assertEquals("unexpected number of queries",
		    0, ProviderHacks.getQueryUnicaster().getQueryNumber() );
        ProviderHacks.getQueryUnicaster().addQuery(qr, null);
        assertEquals("unexpected number of queries",
            1, ProviderHacks.getQueryUnicaster().getQueryNumber() );

        // give udpLoopers time to execute
        // get messages from vector, should be a message or a ping
        // wait some seconds for thread to do work.  this is not scientific 
        // but should do the job...
        try {
            Thread.sleep(30 * 1000);
        }
        catch (InterruptedException ignored) {}
        int numMessages = 0, numQRs = 0, numPings = 0, numQKReqs = 0;
        while (!_messages.isEmpty()) {
            Message currMessage = (Message) _messages.remove(0);
            numMessages++;
            if (currMessage instanceof QueryRequest) {
                QueryRequest currQR = (QueryRequest) currMessage;
                assertEquals("unexpected query", "susheel", currQR.getQuery() );
                numQRs++;
            }
            else if (currMessage instanceof PingRequest) {
                numPings++;
                if (((PingRequest)currMessage).isQueryKeyRequest())
                    numQKReqs++;
            }
            else
                fail("unexpected message: " + currMessage);
        }

        assertEquals("unexpected number of messages", numMessages, numPings + numQRs);
        
        // can't send a Query without sending a Ping....
        assertLessThanOrEquals("unexpected number of QRs", numPings, numQRs);
        assertGreaterThan("unexpected number of QRs", 0, numQRs);
        assertLessThanOrEquals("unexpected number of QRs", numQKReqs, numQRs);
        
        if( LOG.isDebugEnabled() ) {
            LOG.debug("QueryUnicasterTest.testQueries(): numMessages = " + numMessages);
            LOG.debug("QueryUnicasterTest.testQueries(): numQRs = " + numQRs);
            LOG.debug("QueryUnicasterTest.testQueries(): numPings = " + numPings);
        }
            
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
        ProviderHacks.getQueryUnicaster().handleQueryReply(qRep);
    }



    public void testResultMaxOut() throws Exception {
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
                        udpLoop(5000 + index);      //  Bugfix: port 5000+i not 5500+i
                    }
                };
            udpLoopers[i].start();
            Thread.yield(); // let it run.
        }

        // add a Query
		QueryRequest qr = ProviderHacks.getQueryRequestFactory().createQuery("Daswani", (byte)2);
        ProviderHacks.getQueryUnicaster().addQuery(qr, null);

        // add these endpoints....
        InetAddress addr = null;
        addr = InetAddress.getByName("127.0.0.1");
        for (int i = 0; i < NUM_UDP_LOOPS; i++) {
            ProviderHacks.getQueryUnicaster().addUnicastEndpoint(addr, 5000+i);
            if (i % 5 == 0) {
                try {
                    // give some time for queries to get out...
                    Thread.sleep(200);
                }
                catch (InterruptedException ignored) {}
            }
            
            //add some results...
            //  BugFix: now works despite different NUM_UDP_LOOPS values
            int low=250/NUM_UDP_LOOPS+1;    int hi=254/NUM_UDP_LOOPS+1;
            if( low<25 )low=25;             if( hi<35 )hi=35;
            QueryReply qRep = generateFakeReply(qr.getGUID(),
                                                getNumberBetween(low, hi));
            ProviderHacks.getQueryUnicaster().handleQueryReply(qRep);
        }

        // give udpLoopers time to execute
        // get messages from vector, should be a message or a ping
        // wait some seconds for thread to do work.  this is not scientific 
        // but should do the job...
        try {
            Thread.sleep(30 * 1000);
            assertEquals("unexpected number of queries",
                0, ProviderHacks.getQueryUnicaster().getQueryNumber() );
        }
        catch (InterruptedException ignored) {}
        int numMessages = 0, numQRs = 0, numPings = 0;
        while (!_messages.isEmpty()) {
            Message currMessage = (Message) _messages.remove(0);
            numMessages++;
            if (currMessage instanceof QueryRequest) {
                QueryRequest currQR = (QueryRequest) currMessage;
                assertEquals("daswani", currQR.getQuery());
                numQRs++;
            }
            else if (currMessage instanceof PingRequest) {
                numPings++;
            }
            else
                 fail("unexpected message: " + currMessage);
        }
        assertEquals("unexpected number of messages", numMessages, numPings + numQRs);
        assertLessThan("unexpected number of QRs", 11, numQRs); // 15 * 25 >> 250
        
        //  If we are using fewer than 11 UDP endpoints, then ALL of them will have been
        //      used to reply to the UnicastQuery, so none will be left.  Therefore, only
        //      apply the following assert if there are more left over.
        if( NUM_UDP_LOOPS>10 )
            assertGreaterThan("unexpected endpoint size",
                    0, ProviderHacks.getQueryUnicaster().getUnicastEndpoints().size() );
        
        if( LOG.isDebugEnabled() ) {
            LOG.debug("QueryUnicasterTest.testQueries(): numMessages = " +
                  numMessages);
            LOG.debug("QueryUnicasterTest.testQueries(): numQRs = " +
                  numQRs);
            LOG.debug("QueryUnicasterTest.testQueries(): numPings = " +
                  numPings);
        }

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
            resps[i] = ProviderHacks.getResponseFactory().createResponse(i, i, ""+i);
        byte[] ip = {(byte)127, (byte)0, (byte)0, (byte)1};
        QueryReply toReturn = ProviderHacks.getQueryReplyFactory().createQueryReply(guid, (byte) 2, 1,
                ip, 0, resps, GUID.makeGuid(), false);
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
            if( LOG.isDebugEnabled() )
                LOG.debug("QueryUnicasterTest.udpLoop(): listening on port " +
                  port);
			//socket.setSoTimeout(SOCKET_TIMEOUT);
		} 
        catch (SocketException e) {
            if( LOG.isDebugEnabled() )
                LOG.debug("QueryUnicasterTest.udpLoop(): couldn't listen on port " +
                  port);
			return;
		}
        catch (RuntimeException e) {
            if( LOG.isDebugEnabled() )
                LOG.debug("QueryUnicasterTest.udpLoop(): couldn't listen on port " +
                  port);
			return;
		}
		
		
		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                     BUFFER_SIZE);
        while (shouldRun()) {
            try {				
                socket.receive(datagram);
                byte[] data = datagram.getData();
                int length = datagram.getLength();
                try {
                    // construct a message out of it...
                    InputStream in = new ByteArrayInputStream(data, 0, length);
                    Message message = MessageFactory.read(in);		
                    if(message == null) continue;
                    if (message instanceof PingRequest) {
                        PingRequest pr = (PingRequest)message;
                        pr.hop();  // need to hop it!!
                        if (pr.isQueryKeyRequest()) {
                            // send a AddressSecurityToken back!!!
                            AddressSecurityToken qk = 
                                new AddressSecurityToken(datagram.getAddress(),
                                        datagram.getPort());
                            
                            PingReply pRep = 
                                ProviderHacks.getPingReplyFactory().createQueryKeyReply(pr.getGUID(), 
                                                              (byte)1,
                                                              port,
                                                              localhost,
                                                              2,2, true,
                                                              qk);
                            pRep.hop();
                            LOG.debug("QueryUnicasterTest.udpLoop(): sending QK.");
                            ProviderHacks.getQueryUnicaster().handleQueryKeyPong(pRep);
                        }
                    }
                    // log the message....
                    synchronized (_messages) {
                        if( LOG.isDebugEnabled() )
                            LOG.debug(" ** Adding message to _messages queue (newSize="+(_messages.size()+1)+") m="+message);
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
        if( LOG.isDebugEnabled() )
            LOG.debug("QueryUnicasterTest.udpLoop(): closing down port " +
              port);
		socket.close();
	}

}
