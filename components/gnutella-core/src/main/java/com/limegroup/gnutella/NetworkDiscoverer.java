package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import java.util.*;

/** Is just a testing class at this stage. But the goal is to discover the topology
*	of the network.
*/


public class NetworkDiscoverer
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
*	Number of Simultaneous threads to be run to discover the network structure
*/
private static int NUM_THREADS = 15;

/**
*  	Mantains information regarding connections
*	@see com.limegroup.gnutella.ConnectionManager
*/
private ConnectionManager manager = null;


/**
*	Queue of EndPoints (hosts) for BFS traversal of hosts
*/
private LinkedList hostQueue = (LinkedList)Collections.synchronizedList(new LinkedList());

/**
*	HashMap (of EndPoints) to store the Endpoints that have been already visited
*/
private HashMap alreadyVisited = (HashMap)Collections.synchronizedMap(new HashMap());

/** Default Constructor 
*	Initializes the Graph thats gonna maintain Network Structure
*	@see NetworkDiscoverer#graph
*/
public NetworkDiscoverer()
{
	//Initialize the Graph 
	graph = Collections.synchronizedMap(new HashMap());
}


public void doIt()
{
try
{
	//Initialize the connectionmanager. 
	manager = new ConnectionManager();


	//No need to start the ConnectionManager thread. We dont need to accept any
	//incoming connections.
	//But we will use HostCatcher in the connectionManager

	//Start new Threads to get the network information
	NDThread ndThread = null;
	Thread thread = null;
	for(int i=0; i< NUM_THREADS; i++)
	{
		ndThread = new NDThread(manager, graph, hostQueue, alreadyVisited);
		thread = new Thread(ndThread);
		thread.setDaemon(true);
		thread.start();	
	}

}
catch(Exception e)
{
	e.printStackTrace();
	System.exit(1);
}
}


/** Just for testing. Will get replaced completely
*/
public static void main(String[] args)
{


//Lets not get host-port at the argumnts, let it be in the gnutella.net file only

	//Create an instance of NetworkDiscoverer
	NetworkDiscoverer networkDiscoverer = new NetworkDiscoverer();

	//invoke the doIt function on the object, which is gonna take care of everything
	//similar to the run method in a thread.
	networkDiscoverer.doIt();
	
	


}//end of main


}//end of class
