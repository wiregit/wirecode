/*
 * Listens for events relayed from a NetworkClientCallbackStub.
 */
package com.limegroup.gnutella.stubs;


import java.net.*;
import java.io.*;

public class NetworkServerCallbackStub extends ActivityCallbackStub {
	
	private final ServerSocket _ss;
	private Socket _s;
	
	
	
	/**
	 * starts listening for the client callback on the local machine
	 * @param port the port to start listening on.
	 */
	public NetworkServerCallbackStub(int port) throws IOException{
		_ss = new ServerSocket(port);
	}
	
	/**
	 * waits for connection, blocking.  Make sure you run it before using.
	 * the callback stub is not intended to be re-used.
	 * @throws IOException couldn't connect.
	 */
	public void initialize() throws IOException{
		_s = _ss.accept();
	}
	
	private class NotificationDispatcher extends Thread {
		private final Socket _socket;
		
		public NotificationDispatcher(Socket s) {
			_socket =s;
		}
		
		public void run() {
			
			try {
				InputStream in = _socket.getInputStream();
				
				while(true) {
					int code = in.read();
					dispatch(code);
				}
			}catch (IOException die) {}//goodbye
		}
		
		public void dispatch(int code) {
			//TODO: dispatch the appropriate calls.
		}
	}
}
