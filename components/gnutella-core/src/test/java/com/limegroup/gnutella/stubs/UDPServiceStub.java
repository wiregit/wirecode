package com.limegroup.gnutella.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.messages.*;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 * This class allows the creation of a UDPService instances with 
 * controlled delay times and loss rates for testing UDP communication.
 * It routes outgoing messages to itself after the delay time.
 */
public final class UDPServiceStub extends UDPService {

	/**
	 * The queue that processes packets to send.
	 */
	private final ProcessingQueue SEND_QUEUE;

	/**
	 * Constant for the single <tt>UDPService</tt> instance.
	 */
	private final static UDPService INSTANCE1 = new UDPServiceStub();
    
	/**
	 * Constant for the size of UDP messages to accept -- dependent upon
	 * IP-layer fragmentation.
	 */
	private final int BUFFER_SIZE = 1024 * 32;
    

	
	/**
	 * Instance accessor.
	 */
	public static UDPService instance() {
		return INSTANCE1;
	}

	/**
	 * Stub Instance accessor.
	 */
	public static UDPServiceStub stubInstance() {
		return (UDPServiceStub) INSTANCE1;
	}

	/**
	 * Constructs a new <tt>UDPAcceptor</tt>.
	 */
	private UDPServiceStub() {	    

        SEND_QUEUE = new ProcessingQueue("UDPServiceStub-Sender");
    }

    /** 
     */
    public GUID getConnectBackGUID() {
        return null;
    }

    /** 
     */
    public GUID getSolicitedGUID() {
        return null;
    }

    /** 
     */
    DatagramSocket newListeningSocket(int port) throws IOException {
        throw new IOException("no one should be calling me :"+port);
    }


	/** 
     *  Does nothing
	 */
	void setListeningSocket(DatagramSocket datagramSocket) {
	}

	/** Default wait time */
	private final long LONG_TIME = 24 * 60 * 60 * 1000;

	/** The active receivers of messages */
	private final ArrayList RECEIVER_LIST = new ArrayList();

	/**
     *  Create receiver for each simulated incoming connection
	 */
	public void addReceiver(int toPort, int fromPort, int delay, int pctFlaky) {
		Receiver r = new Receiver(toPort, fromPort, delay, pctFlaky);
		synchronized(RECEIVER_LIST) {
			RECEIVER_LIST.add(r);
		}
	}

	/**
     *  Clean up the receiver list
	 */
	public void clearReceivers() {
		synchronized(RECEIVER_LIST) {
			RECEIVER_LIST.clear();
		}
	}

    private class Receiver extends ManagedThread {
        private final ArrayList _messages;
        private final int       _toPort;
        private final int       _fromPort;
        private final int       _delay;
        private final int       _pctFlaky;
		private MessageRouter   _router;
        private Random          _random;
        
        Receiver(int toPort, int fromPort, int delay, int pctFlaky) {
            _messages = new ArrayList();
            _toPort   = toPort;
            _fromPort = fromPort;
            _delay    = delay;
            _pctFlaky = pctFlaky;
			_router   = RouterService.getMessageRouter();
            _random   = new Random();
        	setDaemon(true);
			start();
        }

		public int getPort() {
			return _toPort;
		}

        public void add(DatagramPacket dp) {
			synchronized(_messages) {
				_messages.add(new MessageWrapper(dp, _delay));
				Collections.sort(_messages);
				_messages.notify();
			}
		}
        
        public void managedRun() {
            // forward on message when the time is right
            // ------
            MessageWrapper msg      = null;
			long           time;
			long 		   waitTime;

            while (true) {
				// Add some delay to the loop
				try { Thread.sleep( 1 ); 
				} catch(InterruptedException e) {}

				time = System.currentTimeMillis();
			    synchronized(_messages) {
                    msg = null;
					while (_messages.size() > 0) {
						msg = (MessageWrapper) _messages.get(0);	

                        // Drop message if in flaky range
                        int num = _random.nextInt(100);
                        if (num < _pctFlaky) {
                            msg = null;  
                        } else {
                            break;
                        }
                    }
					if (msg != null ) {
						waitTime = msg._scheduledTime - time;
					} else {
						waitTime = LONG_TIME;
					}

					// wait for the scheduledTime or for earlier msgs
					if ( waitTime > 0 ) {
						try {
							_messages.wait( LONG_TIME );
               			} catch(InterruptedException e) {}
						continue;
					}
				}

				if (msg == null) 
					continue;

				// Message time has been reached so process it
				receive(msg);

			    synchronized(_messages) {
					_messages.remove(0);
				}
            }
        }

		private void receive(MessageWrapper msg) {
        	DatagramPacket datagram = msg._dp;
			// swap the port to the sender from the receiver
			datagram.setPort(_fromPort);

			// ----------------------------*                
			// process packet....
			// *----------------------------
			if(!NetworkUtils.isValidAddress(datagram.getAddress()))
				return;
			if(!NetworkUtils.isValidPort(datagram.getPort()))
				return;
			
			byte[] data = datagram.getData();
			try {
				// we do things the old way temporarily
				InputStream in = new ByteArrayInputStream(data);
				Message message = Message.read(in, Message.N_UDP);
				if(message == null) return;                    
				_router.handleUDPMessage(message, datagram);
			} catch (IOException e) {
				return;
			} catch (BadPacketException e) {
				return;
			}
			// ----------------------------*
		}
    }	

    private class MessageWrapper implements Comparable {
        public final DatagramPacket _dp;
        public final long           _scheduledTime;
        public final int            _delay;
        
        MessageWrapper(DatagramPacket dp, int delay) {
            _dp            = dp;
            _scheduledTime = System.currentTimeMillis() + (long) delay;
            _delay         = delay;
        }

		public int compareTo(Object o) {
			MessageWrapper other = (MessageWrapper) o;
			if (_scheduledTime < other._scheduledTime) 
				return -1;
			else if (_scheduledTime > other._scheduledTime) 
				return 1;
			return 0;
		}

        public String toString() {
            if (_dp != null)
                return _dp.toString();
            else
                return "null";
        }
    }	


	/**
	 *  This code replaces the socket.send.  It internally routes the message
	 *  to a receiver.  This allows multiple local receivers 
	 *  with different ip/ports to be simulated.
	 */
	public void internalSend(DatagramPacket dp) throws NoRouteToHostException {

		Receiver r;

		synchronized(RECEIVER_LIST) {
			for (int i = 0; i < RECEIVER_LIST.size(); i++) {
				r = (Receiver) RECEIVER_LIST.get(i);

				if ( r.getPort() == dp.getPort() ) {
					r.add(dp);
					return;
				}
			}
			throw new NoRouteToHostException("I don't see this ip/port");
		}
	}
    
    /**
     * Sends the specified <tt>Message</tt> to the specified host.
     * 
     * @param msg the <tt>Message</tt> to send
     * @param host the host to send the message to
     */
    public void send(Message msg, IpPort host) {
        send(msg, host.getInetAddress(), host.getPort());
    }

	/**
	 * Sends the <tt>Message</tt> via UDP to the port and IP address specified.
     * This method should not be called if the client is not GUESS enabled.
     *
     * If sending fails for reasons such as a BindException,
     * NoRouteToHostException or specific IOExceptions such as
     * "No buffer space available", this message is silently dropped.
     *
	 * @param msg  the <tt>Message</tt> to send
	 * @param ip   the <tt>InetAddress</tt> to send to
	 * @param port the port to send to
	 */
    public void send(Message msg, InetAddress ip, int port) 
        throws IllegalArgumentException {
        send(msg, ip, port, ErrorService.getErrorCallback());
    }

	/**
	 * Sends the <tt>Message</tt> via UDP to the port and IP address specified.
     * This method should not be called if the client is not GUESS enabled.
     *
     * If sending fails for reasons such as a BindException,
     * NoRouteToHostException or specific IOExceptions such as
     * "No buffer space available", this message is silently dropped.
     *
	 * @param msg  the <tt>Message</tt> to send
	 * @param ip   the <tt>InetAddress</tt> to send to
	 * @param port the port to send to
     * @param err  an <tt>ErrorCallback<tt> if you want to be notified errors
     * @throws IllegalArgumentException if msg, ip, or err is null.
	 */
    public void send(Message msg, InetAddress ip, int port, ErrorCallback err) 
        throws IllegalArgumentException {
        if (err == null)
            throw new IllegalArgumentException("Null ErrorCallback");
        if (msg == null)
            throw new IllegalArgumentException("Null Message");
        if (ip == null)
            throw new IllegalArgumentException("Null InetAddress");
        if (!NetworkUtils.isValidPort(port))
            throw new IllegalArgumentException("Invalid Port: " + port);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            msg.write(baos);
        } catch(IOException e) {
            // this should not happen -- we should always be able to write
            // to this output stream in memory
            ErrorService.error(e);
            // can't send the hit, so return
            return;
        }

        byte[] data = baos.toByteArray();
        DatagramPacket dg = new DatagramPacket(data, data.length, ip, port);
        SEND_QUEUE.add(new Sender(dg, err));
	}
    
    // the runnable that actually sends the UDP packets.  didn't wany any
    // potential blocking in send to slow down the receive thread.  also allows
    // received packets to be handled much more quickly
    private class Sender implements Runnable {
        private final DatagramPacket _dp;
        private final ErrorCallback _err;
        
        Sender(DatagramPacket dp, ErrorCallback err) {
            _dp = dp;
            _err = err;
        }
        
        public void run() {
            // send away
            // ------

			try {
				internalSend(_dp);
			} catch(NoRouteToHostException nrthe) {
				// oh well, if we can't find that host, ignore it ...
			} catch(IOException ioe) {
				String errString = "ip/port: " + 
								   _dp.getAddress() + ":" + 
								   _dp.getPort();
				_err.error(ioe, errString);
			}
        }
    }


	/**
	 */	
	public boolean isGUESSCapable() {
		return false;
	}

	/**
	 * Returns whether or not this node is capable of receiving UNSOLICITED
     * UDP packets.  
	 */	
	public boolean canReceiveUnsolicited() {
		return false;
	}

	/**
	 * Returns whether or not this node is capable of receiving SOLICITED
     * UDP packets.  
	 */	
	public boolean canReceiveSolicited() {
		return true;
	}

	/**
	 */	
	public void setReceiveSolicited(boolean value) {
	}

	/**
	 */
	public boolean isListening() {
        return true;
	}

	/** 
	 * Overrides Object.toString to give more informative information
	 * about the class.
	 */
	public String toString() {
		return "UDPServerStub\r\n loopback";
	}
}
