package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 *This class extends Connection and is invkoed when the Connection Manager has reached it's
 *threshold and cannot handle any more connections without failing.
 *
 *In this situation, the Connecmtion manager opens a reject connection with the requesting host
 *and grabs a list of best hosts from the host catcher. Creates messages as if they were 
 *pongs from the hosts and sends them along and closes the connection
 * 
 *The connection that requested the connection thus gets fake pongs from us and 
 *populates it's host catcher with the very best connections on the network at this time
 */

class RejectConnection extends Connection{

    /** This method is used to establish an incomming connection
     * Note : No outgoing connection is ever a RejectConnection
     * we therefore do not deal with the other constuctor in Connection
     */
    public RejectConnection(Socket sock) throws IOException{
	super(sock.getInetAddress().toString(),sock.getPort(),true); 
	super.initIncoming(sock);
    }

    public void run(){
	Assert.that(sock!=null && in!=null && out!=null, "Illegal socket state for run");
	Assert.that(manager!=null && routeTable!=null && pushRouteTable!=null,"Illegal manager state for run");
	try {
	    while (true) { // continue till we get a PingRequest
		Message m=null;
		try {
		    m=receive(SettingsManager.instance().getTimeout()); //gets the timeout from SettingsManager
		    if (m==null)
			continue;
		}// end of try for BadPacketEception from socket 
		catch (BadPacketException e) {
		    //System.out.println("Discarding bad packet ("+e.getMessage()+")");
		    continue;
		}
		if( (m instanceof PingRequest)&& (m.getHops()==0) ){
		    //this is the only kind of message we will deal with in Reject Connection
		    //If any other kind of message comes in we drop 
		    Iterator iter = manager.catcher.getBestHosts(10);
		    //System.out.println("Sumeet: got a ping request to close");
		    //System.out.println("Sumeet: iter contains hosts " + iter.hasNext());
		    Endpoint bestEndPoint;
		    while(iter.hasNext()){ // we are going to send rejected host the top ten connections
			bestEndPoint =(Endpoint)iter.next();			
			// make a pong with this host info
			PingReply pr = new PingReply(m.getGUID(),(byte)1,bestEndPoint.getPort(),
						     bestEndPoint.getHostBytes(), 0,0);
			//System.out.println("Sumeet : Ip " + bestEndPoint.getHostname());
			//System.out.println("Sumeet : packet " + bestEndPoint.getPort());
			//System.out.println("Sumeet : packet " + pr.toString());
			// the ttl is 1; and for now the number of files and kbytes is set to 0
			// until chris stores more state in the hostcatcher
			send(pr);
			//System.out.println("Sumeet: still have hosts "+ iter.hasNext());
			// send the pont
		    }
		    // we have sent 10 hosts. Now close the connection
		    shutdown();
		}// end of (if m is PingRequest)
	    } // End of while(true)
	}// end of try
	catch (IOException e){
	    shutdown();// if we have an IO Exception drop the connection
	    //e.printStackTrace(); // remove this later
	} // end of catch
    } // end of run
} 
