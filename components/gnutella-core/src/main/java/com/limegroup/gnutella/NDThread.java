package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import java.util.*;

/** Is just a testing class at this stage. But the goal is to discover the topology
*	of the network.
*/


public class NDThread implements Runnable
{

/** Contains the Graph/Network Structure
*	Its 2_Dimensional in Nature (HashMap of HashMaps)
* 	In the first dimension, Endpoints(host-port pairs) are stored.
*	And corresponding to each Endpoint is another HashSet of Endpoints,
*	signifying the edges (endpoints to which it(endpoint in 1st dimension)
*	is connected).
*/
private Map graph;


/**
*  	Mantains information regarding connections
*	@see com.limegroup.gnutella.ConnectionManager
*/
private ConnectionManager manager = null;

/**
*	Queue of EndPoints (hosts) for BFS traversal of hosts
*/
private LinkedList hostQueue = null;

/**
*	The connection that this Thread is managing
*/
Connection connection = null;

/**
*	The object that creates and manages all the NDThreads
*/
private NetworkDiscoverer parent;

/**
*	This is the time uptil which the thread should expect to receive 
*	PING replies (PONGS) after sending an initial PING
*	After this much amount of time, the thread will not wait for PONGS
*/
private static final long TIME_TO_WAIT_FOR_PONGS = 30000; //30 seconds

/**
*	Endpoint to which this thread is connected
*/
private Endpoint endpoint = null;

/** Default Constructor 
*	Initializes various member fields to the arguments passed
*	@see NDThread#parent
*	@see NDThread#manager
*	@see NDThread#graph
*	@see NDThread#hostQueue
*/
public NDThread(NetworkDiscoverer parent, ConnectionManager manager, Map graph, LinkedList hostQueue)
{
	this.parent = parent;
	this.manager = manager;
	this.graph = graph;
	this.hostQueue = hostQueue;
}


public void run()
{

//A message object to store the current message received
Message message = null;

while(true)
{
try
{

	//Before opening a connection, set the TTL value to be 2
	//so as to trace the network
	Const.TTL = (byte)2;


	//Open connection to a host
	while(true)
	{
		try
		{
			connection = getOpenedConnection();
			if(connection != null)
				break;
		}
		catch(NoSuchElementException nsee)
		{
			//Wait for sometime when the hostcatcher gets some elements
			Thread.sleep(30000); //30 seconds

			//DEBUG
			System.out.println("no hostto connect");
		}	
	}	

	//Opening a connection also sends the first initial PING request to the
	//client to which we opened connection
		
	
	//Receive the PING replies
	//till the specified TIME_TO_WAIT_FOR_PONGS
	long startTime = (new Date()).getTime();

	while(((new Date()).getTime() - startTime) < TIME_TO_WAIT_FOR_PONGS)
	{
		message = connection.receive();
		if(message == null)
		{
			//Sleep for some time
			//Dont waste CPU cycles by doing busy waiting
			Thread.sleep(250); //sleep for 1/4th of a second

			//continue to the beginning of the while loop
			continue;
		}	
		//System.out.println("Message received " + message);	



		//Check if the message is a PING reply
		//no need to do anything with other messages
		if(message instanceof PingReply)
		{
			byte[] guid = message.getGUID();	
			Connection originator = manager.routeTable.get(guid);

			//Check if the message is for me
			if(originator == connection)
			{
				//Its PONG for me
				//Get the info regarding the host who sent the reply

				//Create a new EndPoint object (depicting the client from 
				//where the response has come)

				Endpoint remoteEndPoint = new Endpoint(((PingReply)message).getIP(),
												((PingReply)message).getPort());

				
				//Add info to the graph
				synchronized(parent.graphMutex)
				{
				HashSet connectedNodes = (HashSet)graph.get(endpoint);
				connectedNodes.add(remoteEndPoint);
				}
				
			}
			
		}//end of if
		else if(message instanceof PingRequest) //also reply to PINGs
		//but dont forward the PINGs
		{
			Connection inConnection = manager.routeTable.get(message.getGUID()); 
		    //connection has never been encountered before...
		    if (inConnection==null)
			{
			if (message.hop()!=0)
			{
			    manager.routeTable.put(message.getGUID(),connection);//add to Reply Route Table

				//no need to FWD to others
			    //manager.sendToAllExcept(message, this);//broadcast to other hosts

				//says little-endian, but is big-endian
			    byte[] ip=connection.getLocalAddress().getAddress(); //little endian
			    Message pingReply = new PingReply(message.getGUID(),message.getTTL(),manager.getListeningPort(),
							      ip,
							      0, //I think we will get this value from Rob's code
							      0); //Kilobytes also from Robs code
			    connection.send(pingReply);
			}
			}
		}
		else
		{ 
			//do nothing
		}	
		
	}//end of inner while

	//close the connection and proceed again
	manager.remove(connection);
	
}
catch(Exception e)
{
	e.printStackTrace();
	System.exit(1);
}

}//end of outer while loop	
}//end of run


/**
*	It opens a new connection and returns it back. It first tries to get a host:port
*	pair from the hostQueue maintained for BFS traversal. If it doesn't get any from
*	there, it gets one from the hostCatcher
*/
private Connection getOpenedConnection() throws NoSuchElementException
{
	Connection conn = null;
	Endpoint e = null;

	while(true)
	{

	//See if there's a host:port pair (Endpoint) in the hostQueue
	try
	{
		//get the last element from the queue
		//synchronize it
		synchronized(hostQueue)
		{
			e = (Endpoint)hostQueue.getLast();
			//remove it from the queue
			hostQueue.removeLast();
		}
	}
	catch(NoSuchElementException nsee)
	{
		//So, now the hostQueue doesn't have any host
		//Lets get one from the host catcher

		try
		{
			e = manager.catcher.getAnEndpoint();
		}
		catch(NoSuchElementException nsee2)
		{
			throw nsee2;
		}
	}

	//If came here, we have been able to find an Endpoint

	//Check if we have already previously connected to this Endpoint
	//In that case skip it
	synchronized(parent.graphMutex)
	{
	if(graph.containsKey(e))
	{
		//We have already connected directly to this host.
		//Skip it
		//Continue to the beginning of the while loop
		continue;
	}
	}

	//So, open a connection and return it;
	try
	{
		//set TTL for the initial ping (that the Connection's constructor)
		//is gonna send to 2.
		Const.TTL = 2;

		//open the connection
		conn = new Connection(manager, e.hostname, e.port);
		//If able to open the connection, IOException will occur at this point,
		//and the next statements wont be executed. It will be caught by the
		//exception handler

		//Set the endpoint (member field) to this new endpoint
		endpoint = e;

		//Add this node to the graph
		//Add an empty HashSet to store the nodes(Endpoints) this node(Endpoint) is 
		//connected to
		synchronized(parent.graphMutex)
		{
		graph.put(e, new HashSet());
		}

		//return the opened connection
		return conn;
	}
	catch(IOException ioe)
	{
		//No need to panic
		//We are in a while loop, and it should be able to find
		//another active host:port pair (Endpoint)
		//or will finally throw NoSuchElementException

		//continue to the beginning of the while
		continue;
	}

	}//end of while
	
	
}//end of getOpenedConnection


}//end of class
