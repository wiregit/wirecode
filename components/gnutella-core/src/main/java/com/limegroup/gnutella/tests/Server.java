package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.*;

/** The server end of the Gnutella routing benchmark.  See Client. */
public class Server {

/**
* The Host where the servent is running
*/
private String host;

/**
* The port number of the servent process
*/
private int port;

/**
* Number of Server Threads to be started
*/
private int numThreads;

/**
* Array of Server Threads
*/
private SThread[] sThreads;

/**
* A random number for random usage
*/
private Random random = new Random();    
   

public Server(String host, int port, int numThreads)
{
    this.host = host;
    this.port = port;
    this.numThreads = numThreads;
}



private static void syntaxError() 
{
    System.err.println("Syntax: java com.limegroup.gnutella.tests.Server "
                       +"<host> <port> <connections>");
    System.exit(1);
}

public static void main(String args[]) {
    try 
    {
        //parse host and port number
        String host=args[0];
        int port=Integer.parseInt(args[1]);
        int numThreads = Integer.parseInt(args[2]);
        
        //create a new server instance
        Server server =new Server(host, port, numThreads);
        server.doTest();
    } 
    catch (NumberFormatException e) 
    {
        syntaxError();
    } 
    catch (ArrayIndexOutOfBoundsException e) 
    {
        syntaxError();
    } 
}

public void doTest() 
{      
    //Start the required number of sThreads
    
    //allocate the required number of SThreads
    sThreads = new SThread[numThreads];
    
    //start the Threads
    for(int i=0; i < numThreads; i++)
    {
        sThreads[i] = new SThread();
        (new Thread(sThreads[i])).start();
    }
    
  
}


private class SThread implements Runnable
{

/**
* List of queryRequests that this thread have received
*/
Vector queryRequests = new Vector(20, 10);
    
/**
* Connecion for this thread
*/
Connection connection = null;

public void run()
{

    try
    {           
        //open the connection to the servent
        connection =new Connection(host, port);
        connection.connect();

        //start a request receiver thread
        RequestReceiver rr = new RequestReceiver();
        new Thread(rr).start();

        //start a responder thread
        //Responder responder = new Responder();
        //new Thread(responder).start();

    }
    catch(IOException ioe)
    {
        System.err.println("Connections terminated; test is not valid.");
        System.exit(1);
    }
 
}//end of fn run


/**
* Inner class to the SThread class. It sends query replies to the clients
* from where SThread has received queries
*/
private class Responder implements Runnable
{

/**
* The requests received (may not be up-to-date)
*/
QueryRequest[] requests = null;    


    
public void run()
{    
    
    try
    {
        while(true)
        {
            
            //for efficiency reasons, the list of queryRequests is consulted for any
            //new additions, only periodically
            
            int size = 0;
            while( size <= 0)
            {
            
                synchronized(queryRequests)
                {
                    //create the array
                    requests = new QueryRequest[queryRequests.size()];

                    //fill the array
                    Enumeration e = queryRequests.elements();
                    for(int i=0; e.hasMoreElements(); i++) 
                    {
                        try
                        {
                            requests[i] = (QueryRequest)(e.nextElement());
                        }
                        catch(ArrayIndexOutOfBoundsException oobe)
                        {
                            //do nothing, just break out of the loop
                            break;
                        }
                    }

                }//end of synchronization
                
                System.out.println("clients = " + size);
                //Sleep for some time
                //Dont waste CPU cycles by doing busy waiting
                try
                {
                    Thread.sleep(250); //sleep for 1/4th of a second
                }
                catch(InterruptedException ie)
                {
                }
            }




            //Now write replies as fast as possible from each of the request.
            Response[] responses={new Response(0, 20, "file.mp3" + random.nextInt(2000))};

            byte[] clientGUID=new byte[16]; //different for each response
            byte[] ip=new byte[4];          //different for each response



            int n = requests.length;

            //send replies 50 times before consulting for new queries
            for(int j=0; j < 50; j++)
            {
                for (int i=0; i < n ;i++) 
                {    
                    random.nextBytes(clientGUID);
                    random.nextBytes(ip);
                    QueryReply reply=new QueryReply(requests[i].getGUID(), (byte)5, 6346,
                                                    ip, 56, responses, clientGUID);
                    reply.hop();  //so servent doesn't think it's from me
                    connection.send(reply); 
                }//end of inner for	
            }//end of outer for
            
            Thread.yield();

        }//end of while true
    }
    catch(IOException ioe)
    {
        System.err.println("Connections terminated; test is not valid.");
        System.exit(1);    
    }
        
}//end of run

}//end of class Responder

/**
* Adds the request to the list of requests
* @param request The new request to be added
*/
private void addRequest(QueryRequest request)
{
    synchronized(queryRequests)
    {
        queryRequests.add(request);
        
        System.out.println("added request: " + request);
    }
}


/**
* Inner class to the SThread class. It receives the requests
* and notifies SThread of the new requests
*/
private class RequestReceiver implements Runnable
{
    
public void run()
{
    try
    {
        //Get the query requests 

        //declare the variables to be used inside the loops
        Message m = null;
        QueryRequest request = null;
        //keep on receiving requests
        while(true)
        {
            //receive a message
            m= connection.receive();

            //if no message received
            if(m == null)
            {
                //Sleep for some time
                //Dont waste CPU cycles by doing busy waiting
                try
                {
                    Thread.sleep(250); //sleep for 1/4th of a second
                }
                catch(InterruptedException ie){}
                //continue to the beginning of the while loop
                continue;
            }
            else if (m instanceof QueryRequest) 
            {
                //get the request object
                request = (QueryRequest)m;

                //Add it to the list of query requests in the parent
                addRequest(request);
            }
        }
    }
    catch(IOException e) 
    {
        System.err.println("Connections terminated; test is not valid.");
        System.exit(1);
    } 
    catch(BadPacketException e) 
    {
        e.printStackTrace();
        System.err.println("Got bad packet;  test is not valid.");
        System.exit(1);
    }
        
}//end of fn run
    
}//end of class RequestReceiver

}//end of class SThread




}//end of class Server