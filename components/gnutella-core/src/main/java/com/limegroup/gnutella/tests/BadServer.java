package com.limegroup.gnutella.tests;

import java.net.*;
import java.io.*;

/** A bad Gnutella server.  For testing purposes only! */
public class BadServer {
    public static void main(String[] args) {
	try {
	    String host="127.0.0.1";
	    int port=3333;
	    ServerSocket sock=new ServerSocket(port);
	    System.out.println("Waiting for incoming connection...");
	    Socket client=sock.accept();
	    System.out.println("Waiting for connect string...");
	    expectString(client, "GNUTELLA CONNECT/0.4\n\n");
	    System.out.println("Sending reply...");	
	    client.getOutputStream().write(("GNUTEL").getBytes());
	    while (true) { }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
	
    private static void expectString(Socket sock, String s) throws IOException {       	
	byte[] bytes=s.getBytes();
	InputStream in=sock.getInputStream();	
	for (int i=0; i<bytes.length; i++) {
	    int got=in.read();  //Could be optimized, but doesn't matter here.
	    if (got==-1)
		throw new IOException();
	    if (bytes[i]!=(byte)got)
		throw new IOException();
	}
    }
}
