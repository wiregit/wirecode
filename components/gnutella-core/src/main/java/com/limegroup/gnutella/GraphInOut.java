package com.limegroup.gnutella;

import java.net.*;
import java.io.*;
import java.util.*;

/** 
*	This class provides static functions to store the Graph of the network
*	determined by the NetworkDiscoverer class.
*	The methods on this class are invoked from the class NetworkDiscoverer
*/

public class GraphInOut
{

/**
*	Name of the file into which to store the object
*/
private static final String filePrefix = "GraphObject";


private static int count = 0;


/**
*  	Creates a copy of the graph and returns it
*	@param graph The graph (HashMap) to be copied
*/
public static Map getGraphCopy(Map graph)
{

	//Declare a new HashMap
	//of the same size as the graph HashMap
	//and a default load factor of 0.75
	Map copy = new HashMap(graph.size(), 0.75f);

	//get the iterator over the entries
	Iterator iterator = graph.entrySet().iterator();

	//Declare a HashSet for nodes (second field) the given node is connected to
	HashSet nodeSet;

	//iterate
	while(iterator.hasNext())
	{
		Map.Entry e = (Map.Entry) iterator.next();

		//Get the HashSet of nodes corresponding to a given node
		nodeSet = (HashSet) e.getValue();

		//Add the entry with the clone of the HashSet to the copy
		copy.put(e.getKey(), nodeSet.clone());
		
	}

	//return the copy of the graph object
	return copy;
}


/**
*	A static function to create a new DOT file from where we can generate the visual
*	graph using dot/neato utility
*	@param graph The graph to be plotted
*/
public static void makeDotFile(Map graph)
{

	//Declare a PrintWriter for the output file
	PrintWriter out = null;
	try
	{
		//Open the output file
		out = new PrintWriter(new BufferedWriter(new FileWriter(getNewFileName())));
	}
	catch(Exception e)
	{
		e.printStackTrace();
	}

  try
  {	

	//header
	out.println("graph G {");
		
	//get the iterator over the entries
	Iterator iterator = graph.entrySet().iterator();

	//Declare a iterator for nodes (second field) the given node is connected to
	Iterator connectedIterator;

	//iterate
	while(iterator.hasNext())
	{
		Map.Entry e = (Map.Entry) iterator.next();

		//output the node
		out.println(getIdentifier((Endpoint)e.getKey()) + "");

		//Get the iterator over the connected nodes
		connectedIterator = ((Set)e.getValue()).iterator();

		//Iterate over all the connected nodes and add the edges
		while(connectedIterator.hasNext())
		{
			Endpoint endP = (Endpoint)connectedIterator.next();

			//output the edge
			out.println(getIdentifier((Endpoint)e.getKey()) + " -- " + getIdentifier(endP) + " ;");
		}	
	}


	//end of file
	out.println("}");

	//close the file
	out.close();

  }
  catch(Exception e)
  {
	e.printStackTrace();
  }
	
}


/**
*	Generates a new file name and returns for the output file
*/
private static String getNewFileName()
{
	count++;
	return new String(filePrefix + count + ".dot");
}

/**
*	Converts Endpoint to a form that can be used as a node identifier 
*	@param ep The endpoint for which we need String representation (a valid identifier)
*	@return The identifier representation
*/
private static String getIdentifier(Endpoint ep)
{
	//Convert . to _
	String id = ep.getHostname().replace('.', '_');
	//append port num to make it unique
	id += "_" + ep.getPort();

	return id;
}

}
