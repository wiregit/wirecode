package com.limegroup.gnutella;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;

import com.limegroup.gnutella.guess.QueryKey;
import com.limegroup.gnutella.guess.QueryKeyGenerator;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.util.IOUtils;

/** Simulates a 'network' of unicast enabled clients.  The clients don't search,
 *  but they always respond to queries.
 */
public class UnicastSimulator {

    public static final int NUM_LISTENERS = 400;
    public static final int PORT_RANGE_BEGIN = 7070;
    public static final int GNUTELLA_PORT = 9000;
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
        this(InetAddress.getLocalHost().getHostAddress());
    }


    public UnicastSimulator(String externalAddress) throws Exception {
        _localAddress = InetAddress.getByName(externalAddress).getAddress();
        // create pings to return...
        createPongs();
        // create unicast listeners
        createListeners();
        // create server socket to listen for incoming Gnutella's
        createTCPListener(GNUTELLA_PORT);
    }


    private void createPongs() throws Exception {
        _pongs = new PingReply[NUM_LISTENERS];
        for (int i = 0; i < NUM_LISTENERS; i++) {
            _pongs[i] = 
                PingReply.createExternal(GUID.makeGuid(), (byte)5,
                                         PORT_RANGE_BEGIN+i, _localAddress, 
                                         true);                                         
            Assert.that(_pongs[i].isUltrapeer());
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
                sock.setSoTimeout(Constants.TIMEOUT);
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word=IOUtils.readWord(sock.getInputStream(),8);
                sock.setSoTimeout(0);

                if (word.equals(ConnectionSettings.CONNECT_STRING_FIRST_WORD)) {
                    Connection conn = new Connection(sock);
                    conn.initialize(null, null);
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
        QueryKeyGenerator secretKey = QueryKey.createKeyGenerator();
        while (shouldRun()) {
            try {				
                socket.receive(datagram);
                byte[] data = datagram.getData();
                int length = datagram.getLength();
                try {
                    // construct a message out of it...
                    InputStream in = new ByteArrayInputStream(data);
                    Message message = Message.read(in);		
                    if (message == null) continue;
                    if (message instanceof QueryRequest) {
                        String query = ((QueryRequest)message).getQuery();
                        QueryKey queryKey = 
                            ((QueryRequest)message).getQueryKey();
                        QueryKey computed = 
                            QueryKey.getQueryKey(datagram.getAddress(),
                                                 datagram.getPort(),
                                                 secretKey);
                        if (!computed.equals(queryKey))
                            continue; // querykey is invalid!!
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
                                                       GUID.makeGuid(), false);
                        // send the QR...
                        send(socket, qr, datagram.getAddress(),
                             datagram.getPort());
                        // also ack with a pong
                        PingReply toSend = _pongs[port - PORT_RANGE_BEGIN];
                        send(socket, toSend, datagram.getAddress(),
                             datagram.getPort());
                    }
                    else if (message instanceof PingRequest) {
                        PingRequest pr = (PingRequest)message;
                        pr.hop();  // need to hop it!!
                        if (pr.isQueryKeyRequest()) {
                            // send a QueryKey back!!!
                            QueryKey qk = 
                                QueryKey.getQueryKey(datagram.getAddress(),
                                                     datagram.getPort(),
                                                     secretKey);
                            PingReply pRep =
                                PingReply.createQueryKeyReply(pr.getGUID(),
                                                              (byte)1,
                                                              port,
                                                              _localAddress,
                                                              2, 2, true, qk);
                            send(socket, pRep, datagram.getAddress(),
                                 datagram.getPort());
                        }
                        else {
                            PingReply toSend = _pongs[port - PORT_RANGE_BEGIN];
                            send(socket, toSend, datagram.getAddress(),
                                 datagram.getPort());
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
        if (argv.length > 0) {
            UnicastSimulator simulator = new UnicastSimulator(argv[0]);
        }
        else {
            UnicastSimulator simulator = new UnicastSimulator();
        }
        
    }

}
