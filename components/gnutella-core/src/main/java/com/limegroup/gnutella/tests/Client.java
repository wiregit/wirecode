package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.*;

/**
* @author Anurag Singla
*/

/** 
 * The client part of a benchmark for testing Gnutella routing speed.  See
 * also Server.<p>
 *
 * This benchmark is designed to test the ability of a Gnutella servent
 * to read replies from many connections and route them to the appropriate clients
 * (interface the ones who initiate the query).
 * The output is the number of messages the servent can route
 * per sec.  The assumption is that the testers (Server and Client)
 * are much faster than the server, and the network bandwidth is not
 * a limiting factor.<p>
 *
 * The benchmark works as follows.  First you connect Server to the
 * servent to test.  For example:
 * <pre>
 *     java com.limegroup.gnutella.tests.Server 192.168.0.136 6346 10
 * </pre>
 *
 * This will make 10 connections between the RouteServer and the servent.
 * If working correctly, the RouteServer will print something like:
 * <pre>
 *    Connecting to router...done.
 *    Waiting for initial query request...
 * </pre>
 *
 * Now you connect Client to the same servent.  For example:
 * <pre>
 *    java com.limegroup.gnutella.tests.RouteClient 192.168.0.136 6346 10
 * </pre>
 * 
 * This will make 10 connections between the Client and the servent, 
 * each on a separate thread (ClientThread).
 * If all is working correctly, the ClientThreads will send a query request
 * to the servent, and the servent will forward it to each of the ServerThread
 * connections.  Then the ServerThreads will send repeated replies from each
 * of its connections as fast as it can.  The servent should route these to
 * the appropriate ClientThread connections.  The ClientThreads reads these 
 * replies as fast as they can.
 * After specified interval of test time, the Client polls all the ClientThreads
 * and gathers statistics from them, to measure the rate of messages that the
 * servent can route.
 */

public class Client {
    
/** A string highly unlikely to match on the tester. */
public static final String QUERY_STRING="alskdjfloqa";

/**
* The Host where the servent is running
*/
private String host;

/**
* The port number of the servent process
*/
private int port;

/**
* Number of ClientThreads to be started
*/
private int numThreads;

/**
* Array of Client Threads
*/
private CThread[] cThreads;


private static final long NUM_MESSAGES = 10000;

private static final long WARMUPTIME = 10000; //10 seconds

private long timeTaken = 0;

private long numClientsFinished = 0;

/**
* A random number for random usage
*/
private Random random = new Random();

public Client(String host, int port, int numThreads)
{
    this.host = host;
    this.port = port;
    this.numThreads = numThreads;
}

private static void syntaxError() 
{
    System.err.println("Syntax: java com.limegroup.gnutella.tests.Client "
                       +"<host> <port> <connections>");
    System.exit(1);
}

public static void main(String args[]) 
{
    try 
    {
        //parse host and port number
        String host=args[0];
        int port=Integer.parseInt(args[1]);
        int numThreads = Integer.parseInt(args[2]);
        
        //create a new client instance
        Client client =new Client(host, port, numThreads);
        client.doTest();
    } 
    catch (NumberFormatException e) 
    {
        syntaxError();
    } 
    catch (ArrayIndexOutOfBoundsException e) 
    {
        syntaxError();
    } 

}//end of main

public void doTest() 
{      
    //Start the required number of CThreads
    
    //allocate the required number of CThreads
    cThreads = new CThread[numThreads];
    
    //start the Threads
    for(int i=0; i < numThreads; i++)
    {
        cThreads[i] = new CThread();
        new Thread(cThreads[i]).start();
    }
    
  
}

private synchronized void reportTime(long time)
{
    timeTaken += time;
    numClientsFinished++;
    
    float bandwidth = (float)NUM_MESSAGES * numClientsFinished / (timeTaken/1000.f);
    System.out.println("Reply bandwidth: " + bandwidth +" replies/sec");
}


protected void report(int messages, float milliseconds) 
{
    float bandwidth=(float)messages/milliseconds*1000.f;
    System.out.println("Reply bandwidth: "+bandwidth+" replies/sec");
}

/**
* A client thread that sends the initial query to the servent and keeps on 
* receiving query responses
* and record statistics   
*/
private class CThread implements Runnable
{
 
/**
* Send the initial query to the servent and keeps on receiving query responses
* and record statistics    
*/    
public void run()
{
    try
    {
        //open the connection to the servent
        Connection c=new Connection(host, port);
        c.connect();

        //send initial query request
        QueryRequest qr=new QueryRequest((byte)4,0,QUERY_STRING + random.nextInt());
        c.send(qr);
        
        
        //just receive some packets in the waruptime;
        long warmupStartTime = new Date().getTime();
        while( (new Date().getTime() - warmupStartTime) < WARMUPTIME)
        {
            Message m = c.receive();
        }
        //warming up is over    

        //just keep on receiving the packets as fast as we can, without checking
        //what they mean
        //mesaure the time
        long startTime = new Date().getTime();
        for(int i=0; i < NUM_MESSAGES ; i++) 
        {
                Message m = c.receive();
                System.out.println("received" + m); 
        }
        long endTime = new Date().getTime();
        
        reportTime(endTime - startTime);
    }
    catch (IOException e) 
    {
        System.err.println("Connections terminated; test is not valid.");
        System.exit(1);
    } 
    catch (BadPacketException e) 
    {
        e.printStackTrace();
        System.err.println("Got bad packet;  test is not valid.");
        System.exit(1);
    }
}//end of fn run

}//end of class CThread


}//end of class Client


