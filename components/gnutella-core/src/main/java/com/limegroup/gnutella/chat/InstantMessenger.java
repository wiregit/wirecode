package com.limegroup.gnutella.chat;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.net.*;
import java.io.*;

/**
 * this class is a subclass of Chat, also implementing
 * Chatter interface.  it is a one-to-one instant message
 * style chat implementation.
 * 
 *@author rsoule
 */
public class InstantMessenger implements Chatter {

	// Attributes
	private Socket _socket;
	private ByteReader _reader;
	private BufferedWriter _out;
	private String _host;
	private int _port;
	private String _message = "";
	private ActivityCallback _activityCallback;
	private ChatManager  _manager;
	private boolean _outgoing = false;

	/** constructor for an incoming chat request */
	public InstantMessenger(Socket socket, ChatManager manager, 
							ActivityCallback callback) throws IOException {
		_manager = manager;
		_socket = socket;
		_port = socket.getPort();
		_host = _socket.getInetAddress().getHostAddress();
		_activityCallback = callback;
		OutputStream os = _socket.getOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(os);
		_out=new BufferedWriter(osw);
		InputStream istream = _socket.getInputStream();
		_reader = new ByteReader(istream);
		readHeader();
	}

	/** constructor for an outgoing chat request */
	public InstantMessenger(String host, int port, ChatManager manager,
							ActivityCallback callback) throws IOException {
		_host = host;
		_port = port;
		_manager = manager;
		_activityCallback = callback;
		_outgoing = true;
	}

	/** this is only called for outgoing connections, so that the
		creation of the socket will be in the thread */
	private void OutgoingInitializer() throws IOException  {
		_socket =  new Socket(_host, _port);
		_socket.setSoTimeout(SettingsManager.instance().getTimeout());
		OutputStream os = _socket.getOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(os);
		_out=new BufferedWriter(osw);
		// CHAT protocal :
		// First we send the Chat connect string, followed by 
		// any number of '\r\n' terminated header strings, 
		// followed by a singe '\r\n'
        _out.write("CHAT CONNECT/0.1\r\n");
        _out.write("User-Agent: "+CommonUtils.getVendor()+"\r\n");
        _out.write("\r\n");
		_out.flush();
		// next we expect to read 'CHAT/0.1 200 OK' followed 
		// by headers, and then a blank line.
		// TODO: Add socket timeouts.
		InputStream istream = _socket.getInputStream();
		_reader = new ByteReader(istream);
		// we are being lazy here: not actually checking for the 
		// header, and reading until a blank line
		while (true) {
			String str = _reader.readLine();
			if (str == null) 
				return;
			if (str.equals("")) 
				break;
		}
		// finally, we send 
        _out.write("CHAT/0.1 200 OK\r\n");
        _out.write("\r\n");
		_out.flush();
		_socket.setSoTimeout(0);
		_activityCallback.acceptChat(this);
	}

	/** starts the chatting */
	public void start() {
		MessageReader messageReader = new MessageReader(this);
		Thread upThread = new Thread(messageReader);
		upThread.setDaemon(true);
		upThread.start();

	}

	/** stop the chat, and close the connections 
	 * this is always safe to call, but it is recommended
	 * that the gui try to encourage the user not to call 
	 * this
	 */
	public void stop() {
		_manager.removeChat(this);
		try {
			_out.close();
			_socket.close();
		} catch (IOException e) {
			
		} catch (NullPointerException e) {

		}
	}

	/** 
	 * send a message accross the socket to the other host 
	 * as with stop, this is alway safe to call, but it is
	 * recommended that the gui discourage the user from
	 * calling it when a connection is not yet established.
	 */
	public void send(String message) {
		try {
			_out.write(message+"\n");
			_out.flush();
		} catch (IOException e) {
			// e.printStackTrace();
		} catch (NullPointerException e) {

		}
	}

	/** returns the host name to which the 
		socket is connected */
	public String getHost() {
		return _host;
	}

	/** returns the port to which the socket is
		connected */
	public int getPort() {
		return _port;
	}

	public synchronized String getMessage() {
		String str = _message;
		_message = "";
		return str;
	}
	
	public void blockHost(String host) {
		_manager.blockHost(host);
	}

	/** Reads the header information from the chat
		request.  At the moment, the header information
		is pretty useless */
	public void readHeader() throws IOException {
		_socket.setSoTimeout(SettingsManager.instance().getTimeout());
		// For the Server side of the chat protocal:
		// We expect to be recieving 'CHAT CONNECT/0.1'
		// but 'CHAT' has been consumed by acceptor.
		// then, headers, followed by a blank line.
		// we are going to be lazy, and just read until
		// the blank line.
		while (true) {
			String str = _reader.readLine();
			if (str == null) 
				return;
			if (str.equals("")) 
				break;
		}
		// then we want to send 'CHAT/0.1 200 OK'
		_out.write("CHAT/0.1 200 OK\r\n");
		_out.write("\r\n");
		_out.flush();

		// Now we expect to read 'CHAT/0.1 200 OK'
		// followed by headers, followed by a blank line.
		// once again we will be lazy, and just read until
		// a blank line. 
		// TODO: add socket timeouts.
		while (true) {
			String str = _reader.readLine();
			if  (str.equals("")) 
				break;
			if (str == null) 
				return;
		}

		_socket.setSoTimeout(0);
	}


	/**
	 * a private class that handles the thread for reading
	 * off of the socket.
	 *
	 *@author rsoule
	 */
	
	private class MessageReader implements Runnable {
		Chatter _chatter;
		
		public MessageReader(Chatter chatter) {
			_chatter = chatter;
		}

		public void run() {

            try {
                if(_outgoing) {
                    try {
                        OutgoingInitializer();
                    } catch (IOException e) {
                        _activityCallback.chatUnavailable(_chatter);
                        return;
                    }
                }
                while (true){
                    String str;
                    try {
                        // read into a buffer off of the socket
                        // until a "\r" or a "\n" has been 
                        // reached. then alert the gui to 
                        // write to the screen.
                        str = _reader.readLine();
                        synchronized(InstantMessenger.this) {
                            if( ( str == null ) || (str == "") )
                                throw new IOException();
                            _message += str;
                            _activityCallback.receiveMessage(_chatter);
                        } 
                        
                    } catch (IOException e) {
                        // if an exception was thrown, then 
                        // the socket was closed, and the chat
                        // was terminated.
                        // return;
                        _activityCallback.chatUnavailable(_chatter);
                        
                        break;
                    }                     
                }
            } catch(Throwable t) {
                RouterService.error(t);
            }
		}
		
	}



}
