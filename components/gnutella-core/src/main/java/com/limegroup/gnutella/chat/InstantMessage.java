package com.limegroup.gnutella.chat;
/**
 * handles a one-to-one, Instant Message style chat situation
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.*;
import java.net.*;
import java.io.*;


public class InstantMessage extends ChatMessenger implements Chatter {
	
	/** the socket over which the chat is taking place */
	private Socket _socket;
	/** reads off of the socket */
	private ByteReader _byteReader;

	private ActivityCallback _activityCallback;

    private OutputStreamWriter _osw;

	private BufferedWriter _out;

	/** constructor for an incoming (socket is already 
		established) chat connection */
	public InstantMessage(Socket socket) throws IOException {
		// check to see if the socket is null, as a quick 
		// error check.  throw an exception if it is null.
		if (socket == null)
			throw new IOException();
		_socket = socket;
	}

	/** constructor for establishing a connection to a 
		host for a chat session */
	public InstantMessage(String host, int port) throws IOException {
		// attempt to open the socket to the specified host and port
		try {
			_socket = new Socket(host, port);
		} catch (IOException e) {
			// i could just throw this, but maybe in the future, 
			// we would want to throw a specific exception, such
			// as a downloader.CantConnectException.
			throw e;
			// throw new IOException();
		}   

        OutputStream os = _socket.getOutputStream();
		_osw = new OutputStreamWriter(os);
		_out=new BufferedWriter(_osw);
        _out.write("CHAT /chat/" + " HTTP/1.0\r\n");
        _out.write("User-Agent: "+CommonUtils.getVendor()+"\r\n");
        _out.write("\r\n");
        _out.flush();
		System.out.println("Wrote out information");
		
	}
	
	/**
	 * establishes the input and output streams for performing
	 * the chat.
	 */
	private void connect() throws IOException {
		InputStream istream;
		try {
			istream = _socket.getInputStream();
		} catch (IOException e) {
			throw new IOException();
		}
		_byteReader = new ByteReader(istream);
	}


	/** sends a message across the socket */
	public void sendMessage(String msg) throws IOException {
		// String message = ProtocalConverter.instance().toLime(msg);
        _out.write(msg);
        _out.write("\r\n");
        _out.flush();
	}

	/** recieves a message from the socket */
	public String recieveMessage() throws IOException {
		String message = "";
		//cString message = ProtocalConverter.instance().toPlain(msg);
		return message;
	}
	
	/** close a session */
	public void stop() {

	}
}
