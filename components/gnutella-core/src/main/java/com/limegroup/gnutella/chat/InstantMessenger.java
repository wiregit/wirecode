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
	private ChatLineReader _reader;
	private BufferedWriter _out;
	private String _host;
	private int _port;

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
		_reader = new ChatLineReader(istream);
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
		_reader = new ChatLineReader(istream);
	}

	/** starts the chatting */
	public void start() {
		MessageReader messageReader = new MessageReader();
		Thread upThread = new Thread(messageReader);
		upThread.setDaemon(true);
		upThread.start();

	}

	/** stop the chat, and close the connections */
	public void stop() {
		_manager.removeChat(this);
	}

	/** send a message accross the socket to the other host */
	public void send(String message) {
		try {
			_out.write(message+"\r");
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


	/**
	 * a private class that handles the thread for reading
	 * off of the socket.
	 *
	 *@author rsoule
	 */
	
	private class MessageReader implements Runnable {

		public void run() {
			while (true){
				String str;
				try {
					// read into a buffer off of the socket
					// until a "\r" or a "\n" has been 
					// reached. then alert the gui to 
					// write to the screen.
					str = _reader.readLine();
					if ( str != null )
						_activityCallback.recieveMessage(str);
				} catch (IOException e) {
					// if an exception was thrown, then 
					// the socket was closed, and the chat
					// was terminated.
					// return;
					break;
				}

			}
		}
		
	}



}
