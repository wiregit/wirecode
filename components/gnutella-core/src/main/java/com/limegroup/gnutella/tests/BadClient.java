package com.limegroup.gnutella.tests;

import java.net.*;
import java.io.*;

/** A bad Gnutella client.  For testing purposes only! */
public class BadClient {
    public static void main(String[] args) {
	String host="127.0.0.1";
	int port=6346;
	try {
	    Socket s=new Socket(host, port);
	    OutputStream out=s.getOutputStream();
	    out.write(("GNUTELLA CONN").getBytes());
	    out.flush();
	    System.out.println("Your server should disconnect me now.  I'm looping forever.");
	    while (true) { }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}
