package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.util.*;
import java.io.*;

/**
 * @Author Anurag Singla
 */

/**
 * Extends the StandardMessageRouter class to implement caching of query
 * results. It caches the results for most frequently used queries, and in case
 * sufficient number of results are available for a query, it returns those,
 * and doesnt forward the query,
 * else behaves the same as RouterMessageRouter for all other purposes
 */


public class RecorderMessageRouter extends StandardMessageRouter
{
    
/**
* Mappings of connection to the list(vector) of messages received on the
* connection, along with the timestamps.
*/
//Future use
//Map connectionMessageMap = Collections.synchronizedMap(new HashMap());
    
/**
* Vector of messages that we receive along with time stamps
*/
private Vector messages = new Vector(100000); //100,000 messages 

private int mCount = 0;
    
private synchronized void addMessage(Message m)
{
    messages.add(new TimeAndMessage(System.currentTimeMillis(), m));
    
    if((mCount++ % 1000) == 0)
    {
        dumpMessages();
    }
}


private synchronized void dumpMessages()
{
    try
    {
        ObjectOutputStream out = new ObjectOutputStream(
                                    new FileOutputStream("messages.dat"));
        out.writeObject(messages);
        out.close();
    }
    catch(IOException ioe)
    {
        ioe.printStackTrace();
    }
}

    
/**
 * Creates new instance
 * @param callback Callback, whom to notify on various events
 */
public RecorderMessageRouter(ActivityCallback callback) 
{
    //pass it to super class
    super(callback);
}

/** 
* Handles ping requests
*/
protected void handlePingRequest(PingRequest pingRequest,ManagedConnection receivingConnection) 
{
    addMessage(pingRequest);
    
    super.handlePingRequest(pingRequest, receivingConnection);
    
}

public void handlePingReply(PingReply pingReply,
                                ManagedConnection receivingConnection)
{
    addMessage(pingReply);
    
    super.handlePingReply(pingReply, receivingConnection);
}

public void handleQueryRequest(QueryRequest queryRequest,
                                   ManagedConnection receivingConnection)
{
     addMessage(queryRequest);
    
    super.handleQueryRequest(queryRequest, receivingConnection);
}

public void handleQueryReply(QueryReply queryReply,
                                 ManagedConnection receivingConnection)
{
    addMessage(queryReply);
    
    super.handleQueryReply(queryReply, receivingConnection);
}

public void handlePushRequest(PushRequest pushRequest,
                                  ManagedConnection receivingConnection)
{
    addMessage(pushRequest);
    
    super.handlePushRequest(pushRequest, receivingConnection);
}






}//end of class