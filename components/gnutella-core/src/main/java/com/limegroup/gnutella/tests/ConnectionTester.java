package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.*;

/** 
* The server end of the Gnutella routing benchmark. This one
* is used to check the number of connections the servent can handle
* @see Client.
*/
public class ConnectionTester {

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
   

public ConnectionTester(String host,int port,int numThreads)
{
    this.host = host;
    this.port = port;
    this.numThreads = numThreads;
}



private static void syntaxError() 
{
    System.err.println("Syntax: java com.limegroup.gnutella.tests.ConnectionTester "
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
        ConnectionTester connectionTester =new ConnectionTester(host, port, numThreads);
        connectionTester.doTest();
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
private Vector queryRequests = new Vector(20, 10);

private boolean shouldRunResponder = false;
    
/**
* Connecion for this thread
*/
Connection connection = null;

public void run()
{

    try
    {           
        //open the connection to the servent
        connection = new Connection(host, port);
        connection.connect();

        //receive the first message
        Message m = null;
        QueryRequest request = null;
        while(m == null)
        {
            //receive a message
            try
            {
                m= connection.receive();
            }
            catch(BadPacketException bpe)
            {
                
            }
            if(m != null && m instanceof QueryRequest) 
            {
                //get the request object
                request = (QueryRequest)m;

            }
        }


        //Now write replies as fast as possible from each of the request.
        Response[] responses={new Response(0, 20, "file.mp3" + random.nextInt(2000))};

        byte[] clientGUID=new byte[16]; //different for each response
        byte[] ip=new byte[4];          //different for each response


 
        while(true)
        {
            random.nextBytes(clientGUID);
            random.nextBytes(ip);
            QueryReply reply=new QueryReply(request.getGUID(), (byte)5, 6346,
                                            ip, 56, responses, clientGUID);
            reply.hop();  //so servent doesn't think it's from me
            connection.send(reply); 
            //System.out.println("reply sent");
           
        }//end of while true
    }
    catch(IOException ioe)
    {
        System.err.println("Connections terminated; test is not valid.");
        System.exit(1);    
    }

 
}//end of fn run

}//end of class SThread




}//end of class Server