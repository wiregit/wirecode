package com.limegroup.gnutella.guess;

import java.io.*;
import java.net.*;
import junit.framework.*;
import com.limegroup.gnutella.*;

/** Provides primitives for contacting GUESS nodes on the network.
 *  THIS TEST SHOULD NOT BE INCLUDED IN ALL TESTS!  It is very specifically 
 *  tuned and will now work in general.
 */
public class GUESSTester extends TestCase {

	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

    private final int WAIT_TIME   = 500; // 1/2 a second...

    private DatagramSocket _socket = null;

    private QueryReply _qr = null;
    private final Object _qrLock = new Object();

    private PingReply _pong = null;
    private final Object _pongLock = new Object();

	/**
	 * Constructs a new <tt>GUESSTester</tt> instance.
	 */
	public GUESSTester(String name) {
		super(name);
        initUDPPort();
	}


    private void initUDPPort() {
        try {
            _socket = new DatagramSocket();
            _socket.setSoTimeout(1000);
            Thread listener = new Thread() {
                    public void run() {
                        listenLoop();
                    }
                };
            listener.start();
        }
        catch (Exception ignored) {
            System.out.println("Could not get a UDP Socket!!");
        }
    }


    private void listenLoop() {
		byte[] datagramBytes = new byte[BUFFER_SIZE];
		DatagramPacket datagram = new DatagramPacket(datagramBytes, 
                                                     BUFFER_SIZE);
        while (true) {
            try {				
                _socket.receive(datagram);
                byte[] data = datagram.getData();
                int length = datagram.getLength();
                try {
                    // construct a message out of it...
                    InputStream in = new ByteArrayInputStream(data);
                    Message message = Message.read(in);		
                    if (message == null) continue;
                    if (message instanceof QueryReply) {
                        synchronized (_qrLock) {
                            _qr = (QueryReply) message;
                            _qrLock.notify();
                        }
                    }
                    else if (message instanceof PingReply) {
                        synchronized (_pongLock) {
                            _pong = (PingReply) message;
                            _pongLock.notify();
                        }
                    }
                } 
                catch(BadPacketException e) {
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
    }


	/**
	 * Run this suite of tests.
	 */
	public static Test suite() {
		return new TestSuite(GUESSTester.class);
	}

    public void testSmall() {
        try {
            assertTrue(testAck("10.254.0.19", 6346) > 0);
            assertTrue(testQuery("10.254.0.19", 6346,
                                 new QueryRequest((byte) 1, 0, 
                                                  "morrissey")) != null);
        }
        catch (Exception whatever) {
            assertTrue(false);
        }
    }

    /** This method blocks for possibly several seconds.
     *  @return a non-negative value if the ack was recieved.  else 0...
     */
    public synchronized long testAck(String host, int port) 
        throws UnknownHostException, IOException {
        synchronized (_pongLock) {
            _pong = null;
        }
        QueryRequest qr = new QueryRequest((byte)1, 0, "susheel");
        InetAddress addr = InetAddress.getByName(host);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qr.write(baos);
        DatagramPacket toSend = new DatagramPacket(baos.toByteArray(),
                                                   baos.toByteArray().length,
                                                   addr, port);
        long startTime = 0, endTime = 0;
        synchronized (_pongLock) {
            startTime = System.currentTimeMillis();
            _socket.send(toSend);
            try {
                // wait up to 2.5 seconds for an ack....
                _pongLock.wait(WAIT_TIME);
            }
            catch (InterruptedException ignored) {}
            endTime = System.currentTimeMillis();
        }
        if (_pong == null)
            return 0;
        else return (endTime - startTime);
    }

    /** This method blocks for possibly several seconds.
     *  @return A QueryReply to your input Query.  May be null.
     */ 
    public synchronized QueryReply testQuery(String host, int port, 
                                             QueryRequest qr) 
        throws UnknownHostException, IOException {
        synchronized (_qrLock) {
            _qr = null;
        }
        synchronized (_pongLock) {
            _pong = null;
        }
        InetAddress addr = InetAddress.getByName(host);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        qr.write(baos);
        DatagramPacket toSend = new DatagramPacket(baos.toByteArray(),
                                                   baos.toByteArray().length,
                                                   addr, port);
        synchronized (_qrLock) {
            _socket.send(toSend);
            try {
                // wait up to 2.5 seconds for an ack....
                _qrLock.wait(WAIT_TIME);
            }
            catch (InterruptedException ignored) {}
        }
        return _qr;
    }


    public static final boolean debugOn = true;
    public static final void debug(String out) {
        if (debugOn)
            System.out.println(out);
    }


}
