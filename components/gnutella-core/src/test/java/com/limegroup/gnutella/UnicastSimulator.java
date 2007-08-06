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

import org.limewire.io.IOUtils;
import org.limewire.security.AddressSecurityToken;

import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.MessageFactory;
import com.limegroup.gnutella.messages.PingReply;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.settings.ConnectionSettings;

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
                ProviderHacks.getPingReplyFactory().createExternal(GUID.makeGuid(), (byte)5,
                                         PORT_RANGE_BEGIN+i, _localAddress, 
                                         true);                                         
            assert(_pongs[i].isUltrapeer());
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
            System.out.println("LISTENING FOR GNUTELLA CONNECTIONS ON PORT " +
                               port);
        }
        catch (Exception noWork) {
			return;
        }

        while (shouldRun()) {
            try {
                // listen for GNUTELLA connections, send back pings when
                // established
                Socket sock = servSock.accept();
                sock.setSoTimeout(Constants.TIMEOUT);
                //dont read a word of size more than 8 
                //("GNUTELLA" is the longest word we know at this time)
                String word=IOUtils.readWord(sock.getInputStream(),8);
                sock.setSoTimeout(0);

                if (word.equals(ConnectionSettings.CONNECT_STRING_FIRST_WORD)) {
                    Connection conn = new Connection(sock);
                    conn.initialize(null, null, 1000);
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
			//socket.setSoTimeout(SOCKET_TIMEOUT);
		} 
        catch (SocketException e) {
			return;
		}
        catch (RuntimeException e) {
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
                    if (message == null) continue;
                    if (message instanceof QueryRequest) {
                        String query = ((QueryRequest)message).getQuery();
                        AddressSecurityToken addressSecurityToken = 
                            ((QueryRequest)message).getQueryKey();
                        AddressSecurityToken computed = 
                            new AddressSecurityToken(datagram.getAddress(),
                                    datagram.getPort());
                        if (!computed.equals(addressSecurityToken))
                            continue; // querykey is invalid!!
                        byte[] inGUID = ((QueryRequest)message).getGUID();
                        Response[] resps = new Response[rand.nextInt(15)];
                        for (int i = 0; i < resps.length; i++) {
                            resps[i] = ProviderHacks.getResponseFactory().createResponse(port, 200,
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
                            // send a AddressSecurityToken back!!!
                            AddressSecurityToken qk = 
                                new AddressSecurityToken(datagram.getAddress(),
                                        datagram.getPort());
                            PingReply pRep =
                                ProviderHacks.getPingReplyFactory().createQueryKeyReply(pr.getGUID(),
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
    

    
    public static void main(String argv[]) throws Exception {
        if (argv.length > 0) {
            new UnicastSimulator(argv[0]);
        } else {
            new UnicastSimulator();
        }
        
    }

}
