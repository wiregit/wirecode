package com.limegroup.gnutella.chat;
/**
 * this class is a subclass of Chat, also implementing
 * Chatter interface.  it is a one-to-one instant message
 * style chat implementation.
 * 
 *@author rsoule
 */

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.net.*;
import java.io.*;

public class InstantMessenger extends Chat {

	// Attributes
	private Socket _socket;
	private ByteReader _reader;
	private BufferedWriter _out;
	private String _host;
	private int _port;
	private String _message = "";

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
		_socket =  new Socket(_host, _port);
		OutputStream os = _socket.getOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(os);
		_out=new BufferedWriter(osw);
        _out.write("CHAT /chat/" + " HTTP/1.0\r\n");
        _out.write("User-Agent: "+CommonUtils.getVendor()+"\r\n");
        _out.write("\r\n");
		_out.flush();
		InputStream istream = _socket.getInputStream();
		_reader = new ByteReader(istream);
	}

	/** starts the chatting */
	public void start() {
		MessageReader messageReader = new MessageReader(this);
		Thread upThread = new Thread(messageReader);
		upThread.setDaemon(true);
		upThread.start();

	}

	/** stop the chat, and close the connections */
	public void stop() {
		_manager.removeChat(this);
		_activityCallback.removeChat(this);
		try {
			_out.close();
			_socket.close();
		} catch (IOException e) {
			
		}
	}

	/** send a message accross the socket to the other host */
	public void send(String message) {
		try {
			_out.write(message+"\n");
			_out.flush();
		} catch (IOException e) {
			// e.printStackTrace();
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

	public String getMessage() {
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
	public void readHeader() {
		try {
			for (int i =0; i < 6; i++) { 
				_reader.readLine();
			}
		} catch (IOException e) {
			return;
		}
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
			while (true){
				String str;
				try {
					// read into a buffer off of the socket
					// until a "\r" or a "\n" has been 
					// reached. then alert the gui to 
					// write to the screen.
					str = _reader.readLine();
					if ( str != null ) {
						_message += str;
						_activityCallback.recieveMessage(_chatter);
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
		}
		
	}



}
