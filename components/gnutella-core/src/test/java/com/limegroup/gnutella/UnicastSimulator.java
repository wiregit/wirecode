package com.limegroup.gnutella;

import java.net.*;
import java.io.*;

/** Simulates a 'network' of unicast enabled clients.  The clients don't search,
 *  but they always respond to queries.
 */
public class UnicastSimulator {

    public static final int NUM_LISTENERS = 400;
    public static final int PORT_RANGE_BEGIN = 7070;
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

	private final int SOCKET_TIMEOUT = 2*1000; // 2 second wait for a message

    PingReply[] _pongs;
    Thread[] _unicasters;

    public UnicastSimulator() throws Exception {
        // create pings to return...
        createPongs();
        // create unicast listeners
        createListeners();
        // create server socket to listen for incoming Gnutella's
    }


    private void createPongs() throws Exception {
        _pongs = new PingReply[NUM_LISTENERS];
        byte[] localAddress = InetAddress.getLocalHost().getAddress();
        for (int i = 0; i < NUM_LISTENERS; i++)
            _pongs[i] = new PingReply(GUID.makeGuid(), (byte) 5, 
                                      PORT_RANGE_BEGIN+i, localAddress, 
                                      10, 100, true);
    }

    
    private void createListeners() throws Exception {
        _unicasters = new Thread[NUM_LISTENERS];
        for (int i = 0; i < NUM_LISTENERS; i++) {
            final int offset = i;
            _unicasters[i] = new Thread() {
                    public void run() {
                        unicastLoop(PORT_RANGE_BEGIN+offset);
                    }
                };
            _unicasters[i].start();
        }
    }


    private boolean shouldRun() {
        return true;
    }

    /* @param port the port to listen for queries on...
     */ 
    private void unicastLoop(int port) {
        DatagramSocket socket = null;
		try {
			socket = new DatagramSocket(port);
            socket.setSoTimeout(1000);
            debug("UnicastSimulator.unicastLoop(): listening on port " +
                  port);
			//socket.setSoTimeout(SOCKET_TIMEOUT);
		} 
        catch (SocketException e) {
            debug("UnicastSimulator.unicastLoop(): couldn't listen on port " +
                  port);
			return;
		}
        catch (RuntimeException e) {
            debug("UnicastSimulator.unicastLoop(): couldn't listen on port " +
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
                    InputStream in = new ByteArrayInputStream(data);
                    Message message = Message.read(in);		
                    if(message == null) continue;
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
        debug("UnicastSimulator.unicastLoop(): closing down port " +
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

    
    public static void main(String argv[]) throws Exception {
        UnicastSimulator simulator = new UnicastSimulator();
    }

}
