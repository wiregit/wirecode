package com.limegroup.gnutella;

import java.io.*;
import java.util.*;

/**
 * The command-line UI for the router
 */
public class Main {	
    public static void main(String args[]) {
	//Start thread to accept connections.  Optional first arg is the 
	//listening port number.
	RouterService service;
	if (args.length==1) {
	    service=new RouterService(Integer.parseInt(args[0]));
	} else {
	    service=new RouterService();
	}

	BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
	for ( ; ;) {
	    System.out.print("LimeRouter> ");
	    try {
		String command=in.readLine();
		if (command==null)
		    break;
		else if (command.equals("quit"))
		    break;
		//Print routing tables
		else if (command.equals("route"))
		    service.dumpRouteTable();
		//Print connections
		else if (command.equals("stat")) {
		    service.dumpConnections();
		}
		String[] commands=split(command);
		//Connect to remote host (establish outgoing connection)
		if (commands.length>=2 && commands[0].equals("connect")) {
		    try {
			int port=6346;
			if (commands.length>=3)
			    port=Integer.parseInt(commands[2]);
			service.connectToHost(commands[1], port);
		    } catch (IOException e) {
			System.out.println("Couldn't establish connection.");
		    }
		}
	    } catch (IOException e) {
		System.exit(1);
	    }	    
	}
	System.out.println("Good bye.");
	service.shutdown(); //write gnutella.net
    }

    /** Returns an array of strings containing the words of s, where
     *  a word is any sequence of characters not containing a space.
     */
    public static String[] split(String s) {
	s=s.trim();
	int n=s.length();
	if (n==0)
	    return new String[0];
	Vector buf=new Vector();

	//s[i] is the start of the word to add to buf
	//s[j] is just past the end of the word
	for (int i=0; i<n; ) {
	    Assert.that(s.charAt(i)!=' ');
	    int j=s.indexOf(' ',i+1);
	    if (j==-1)
		j=n;
	    buf.add(s.substring(i,j));
	    //Skip past whitespace (if any) following s[j]
	    for (i=j+1; j<n ; ) {
		if (s.charAt(i)!=' ')
		    break;
		i++;
	    }			
	}
	String[] ret=new String[buf.size()];
	for (int i=0; i<ret.length; i++)
	    ret[i]=(String)buf.get(i);
	return ret;
    }
}
