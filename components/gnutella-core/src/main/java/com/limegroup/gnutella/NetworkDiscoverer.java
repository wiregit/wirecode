package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import java.util.*;

/** 
*	This class determines the network topology of the gnutella clients.
*	It periodically keeps on storing the updated information (object) in the
*	file specified by the constant name variable: GraphInOut.filename
*	@see GraphInOut
*/


public class NetworkDiscoverer
{

/** Contains the Graph/Network Structure
*	Its 2_Dimensional in Nature
* 	In the first dimension, Endpoints(host-port pairs) are stored.
*	And corresponding to each Endpoint is another HashSet of Endpoints,
*	signifying the edges (endpoints to which it(endpoint in 1st dimension)
*	is connected).
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
*	Object for synchronizing access to graph
*	@see NetworkDiscoverer#graph
*/
public Object graphMutex = new Object();


/** Default Constructor 
*	Initializes the Graph thats gonna maintain Network Structure
*	@see NetworkDiscoverer#graph
*/
public NetworkDiscoverer()
{
	//Initialize the Graph 
	graph = new HashMap();
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
		ndThread = new NDThread(this, manager, graph, hostQueue);
		thread = new Thread(ndThread);
		thread.setDaemon(true);
		thread.start();	
	}

	//Now we have started all the threads 
	//Those threads will keep on getting information regarding the network
	//and will keep on updating the graph

	//Now we will periodically extract information from the current graph, 
	//and output it to file to visualize the graph

	//These variables are to be used inside this function only
	//Therefore are being declared local
	long WARMUP_TIME = 30000; //30 seconds
	long WAIT_TIME	 = 30000; //30 seconds

	//Now wait for some initial time (WARMUP_TIME) for some initial data to get 
	//into the graph
	Thread.sleep(WARMUP_TIME);


	//Keep on outputting the DOT file from which Graph can be constructed
	while(true)
	{
		//Make a copy the graph object (of type HashMap) so that other threads can continue
		//We will work on the copy of the object to avoid contention

		Map graphCopy;
	
		synchronized(graphMutex)
		{
			graphCopy = GraphInOut.getGraphCopy(graph);		
		}//copy made

		//output the graph to file

		//sleep for some time

		Thread.sleep(WAIT_TIME);

		
	}//end of while


}
catch(Exception e)
{
	e.printStackTrace();
	System.exit(1);
}
}




/** 
*	Creates an instance of NetworkDescriptor class and leaves the rest
*	to the doit() function
*	@see NetworkDiscoverer#doIt()
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
