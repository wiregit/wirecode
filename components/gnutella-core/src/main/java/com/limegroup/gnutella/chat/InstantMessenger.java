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

	/** constructor for an incoming chat request */
	public InstantMessenger(Socket socket, ChatManager manager, 
							ActivityCallback callback) throws IOException {
		System.out.println("InstantMessenger socket ");
		_manager = manager;
		_socket = socket;
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
		System.out.println("InstantMessenger host port ");
		_manager = manager;
		_activityCallback = callback;
		_socket =  new Socket(host, port);
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
			_out.write(message);
		} catch (IOException e) {
		}
	}

	/** read a message off of the socket */
	public String getMessage() {
		return "";
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
				System.out.println("reading..");

				String str;
				try {
					// read into a buffer off of the socket
					// until a "\r" or a "\n" has been 
					// reached. then alert the gui to 
					// write to the screen.
					str = _reader.readLine();
					_activityCallback.recieveMessage(str);
				} catch (IOException e) {
					// if an exception was thrown, then 
					// the socket was closed, and the chat
					// was terminated.
					return;
				}

			}
		}
		
	}



}
