package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import java.util.*;

/** Simulates a 'network' of unicast enabled clients.  The clients don't search,
 *  but they always respond to queries.
 */
public class UnicastSimulator {

    public static final int NUM_LISTENERS = 400;
    public static final int PORT_RANGE_BEGIN = 7070;
    public static final int GNUTELLA_PORT = 7000;
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 8192;

	private final int SOCKET_TIMEOUT = 2*1000; // 2 second wait for a message

    private PingReply[] _pongs;
    private Thread[] _unicasters;
    private byte[] _localAddress;
    private Thread _tcpListener;

    private boolean _shouldRun = true;

    private Random rand = new Random();

    public UnicastSimulator() throws Exception {
        // create pings to return...
        createPongs();
        // create unicast listeners
        createListeners();
        // create server socket to listen for incoming Gnutella's
        createTCPListener(GNUTELLA_PORT);
    }


    private void createPongs() throws Exception {
        _pongs = new PingReply[NUM_LISTENERS];
        _localAddress = InetAddress.getLocalHost().getAddress();
        for (int i = 0; i < NUM_LISTENERS; i++) {
            _pongs[i] = new PingReply(GUID.makeGuid(), (byte) 5, 
                                      PORT_RANGE_BEGIN+i, _localAddress, 
                                      10, 100, true);
            Assert.that(_pongs[i].isMarked());
        }
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


    private void createTCPListener(final int port) {
        _tcpListener = new Thread() {
                public void run() {
                    tcpLoop(port);
                }
            };
        _tcpListener.start();
    }


    private boolean shouldRun() {
        return _shouldRun;
    }


    private void tcpLoop(int port) {
        ServerSocket servSock = null;
        try {
            // create a ServerSocket to listen for Gnutellas....
            servSock = new ServerSocket(port);
            debug("UnicastSimulator.tcpLoop(): listening on port " +
                  port);
            System.out.println("LISTENING FOR GNUTELLA CONNECTIONS ON PORT " +
                               port);
        }
        catch (Exception noWork) {
            debug("UnicastSimulator.tcpLoop(): couldn't listen on port " +
                  port);
			return;
        }

        while (shouldRun()) {
            try {
                // listen for GNUTELLA connections, send back pings when
                // established
                Socket sock = servSock.accept();
                debug("UnicastSimulator.tcpLoop(): got a incoming connection.");
                sock.setSoTimeout(SettingsManager.instance().getTimeout());
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word=IOUtils.readWord(sock.getInputStream(),8);
                sock.setSoTimeout(0);

                if (word.equals(SettingsManager.instance().
                        getConnectStringFirstWord())) {
                    Connection conn = new Connection(sock);
                    conn.initialize();
                    debug("UnicastSimulator.tcpLoop(): sending pings.");
                    for (int i = 0; i < _pongs.length; i++) {
                        conn.send(_pongs[i]);
                        Thread.sleep(10);
                    }
                    conn.close();
                }                
            }
            catch (Exception ignored) {
            }
        }

        try {
            servSock.close();
        }
        catch (Exception ignored) {}
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
                    if (message instanceof QueryRequest) {
                        String query = ((QueryRequest)message).getQuery();
                        byte[] inGUID = ((QueryRequest)message).getGUID();
                        Response[] resps = new Response[rand.nextInt(15)];
                        for (int i = 0; i < resps.length; i++) {
                            resps[i] = new Response(port, 200, 
                                                    query + " - " + 
                                                    rand.nextInt(250));
                        }
                        QueryReply qr = new QueryReply(inGUID, (byte) 5,
                                                       port, _localAddress,
                                                       0, resps, 
                                                       GUID.makeGuid());
                        // send the QR...
                        send(socket, qr, datagram.getAddress(),
                             datagram.getPort());
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
        debug("UnicastSimulator.unicastLoop(): closing down port " +
              port);
		socket.close();
    }


    private void send(DatagramSocket socket, Message msg, 
                      InetAddress ip, int port) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			msg.write(baos);
		} catch(IOException e) {
			e.printStackTrace();
			// can't send the hit, so return
			return;
		}

		byte[] data = baos.toByteArray();
		DatagramPacket dg = new DatagramPacket(data, data.length, ip, port); 
		try {
            socket.send(dg);
		} catch(IOException e) {
			e.printStackTrace();
			// not sure what to do here -- try again??
		}
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
