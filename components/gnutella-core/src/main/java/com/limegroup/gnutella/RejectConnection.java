package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;

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

    /**
    * It listens to the socket till timeout for a ping request. If it receives a ping request
    * in that time, it sends some good hosts back to the connected host, and then closes the connection
    * //modified drastically by Anu
    */
    public void run(){
	Assert.that(sock!=null && in!=null && out!=null, "Illegal socket state for run");
	Assert.that(manager!=null && routeTable!=null && pushRouteTable!=null,"Illegal manager state for run");
	
        try {
            //declare a message instane
            Message m=null;
            
           
            //receive a message
            try {
                m=receive(SettingsManager.instance().getTimeout()); //gets the timeout from SettingsManager
                if (m==null){
                    //we havent received the message and timeout has occured, so just close the
                    //connection and return //anu
                    shutdown();
                    return;
                    //continue;
                }
            }// end of try for BadPacketEception from socket 
            catch (BadPacketException e) {
                //bad packet. Just close the connection and return
                shutdown();
                return;
            }
            
            //if its a ping request from the connected host
            if( (m instanceof PingRequest)&& (m.getHops()==0) ){
                //this is the only kind of message we will deal with in Reject Connection
                //If any other kind of message comes in we drop 
                
                 System.out.println("gonna send pongs");
                //get and send the pongs with good hosts info to the connected host
                sendPongs(m.getGUID());
                
                // we have sent the pongs. Now close the connection
                shutdown();
            }// end of (if m is PingRequest)
	}// end of try
	catch (IOException e){
	    shutdown();// if we have an IO Exception drop the connection
	} // end of catch
    } // end of run
    
    /**
    * Get good hosts from the Network Discovery Engine, and
    * sends pongs to the connected host, with the retrieved hosts
    * @param guid GUID for the pong
    */
    private void sendPongs(byte[] guid)
    {
        
        try
        {
            //retrieve an array of hosts from the network discovery engine
            Endpoint[] endpoints = manager.getNDAccess().getNGoodEndpoints(10);
            
            //iterate over the endpoints
            for(int i=0; i < endpoints.length; i++)
            {
                System.out.println("Endpoint[" + i + "]=" + endpoints[i]);
                // make a pong with this host info, set the TTL to 1
                
                try
                {
                    PingReply pr = new PingReply(guid,(byte)1,endpoints[i].getPort(),
                                                 endpoints[i].getHostBytes(), endpoints[i].getFiles(),
                                                 endpoints[i].getKbytes());
                    //send the pong (ping reply)
                    send(pr);
                }
                catch(UnknownHostException uhe)
                {
                    //dont need to do anything
                    //it will go to the next endpoint
                }
            }
        }
        catch(Exception e)
        {
            //e.printStackTrace();
            //doesnt matter if we r not able to send pongs
        }
    }
    
    
} 
