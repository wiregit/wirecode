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
try
{

	//Before opening a connection, set the TTL value to be 2
	//so as to trace the network
	Const.TTL = (byte)2;


/*
	//Open connection to a host
	connection = getOpenedConnection();
		
	
	//Open a connection to the specified initial gnutella client
	Connection connection =new Connection(manager, host, port);
	Thread tc=new Thread(connection);
	tc.setDaemon(true);
	tc.start();

	//Create a new ping message with TTL 2
	Message message = new PingRequest((byte)2);


	//Send the message
	//connection.send(message);
	//System.out.println("message sent " + message);

	//Receive the messages
	
	while(true)
	{
		message = connection.receive();
		if(message == null)
			continue;
		System.out.println("Message received " + message);	
	}
	
*/

	//Send a ping request
	
	//get a response

}
catch(Exception e)
{
	e.printStackTrace();
	System.exit(1);
}
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

	//So, open a connection and return it;
	try
	{
		conn = new Connection(manager, e.hostname, e.port);
		return conn;
	}
	catch(IOException ioe)
	{
		//No need to panic
		//We are in a while loop, and it should be able to find
		//another active host:port pair (Endpoint)
		//or will finally throw NoSuchElementException
	}

	}//end of while
	
	
}//end of getOpenedConnection


}//end of class
