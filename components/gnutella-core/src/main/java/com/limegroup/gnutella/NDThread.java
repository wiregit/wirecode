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
*	And corresponding to each Endpoint is another HashMap of Endpoints,
*	signifying the edges (endpoints to which it(endpoint in 1st dimension)
*	is connected.
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
*	HashMap (of EndPoints) to store the Endpoints that have been already visited
*/
private HashMap alreadyVisited = null;


/**
*	This is the time uptil which the thread should expect to receive 
*	PING replies (PONGS) after sending an initial PING
*	After this much amount of time, the thread will not wait for PONGS
*/
private static final long TIME_TO_WAIT_FOR_PONGS = 5000; //5 seconds

/**
*	Endpoint to which this thread is connected
*/
private Endpoint endpoint = null;

/** Default Constructor 
*	Initializes various member fields to the arguments passed
*	@see NDThread#manager
*	@see NDThread#graph
*	@see NDThread#hostQueue
*	@see NDThread#alreadyVisited
*/
public NDThread(ConnectionManager manager, Map graph, LinkedList hostQueue, HashMap alreadyVisited)
{
	this.manager = manager;
	this.graph = graph;
	this.hostQueue = hostQueue;
	this.alreadyVisited = alreadyVisited;
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
	connection = getOpenedConnection();

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
				

				//see if an entry to the 
			}
			
		}//end of if
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
		//get the last eleement from the queue
		e = (Endpoint)hostQueue.getLast();
		//remove it from the queue
		hostQueue.removeLast();
		
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
	if(graph.containsKey(e))
	{
		//We have already connected directly to this host.
		//Skip it
		//Continue to the beginning of the while loop
		continue;
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
