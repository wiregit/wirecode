package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.*;
import java.io.*;
import java.util.*;

public class MessageSimulator 
{



public static void main(String[] args)
{
    try
    {
        //decode args
        if(args.length != 2)
        {
            syntaxError();
        }
        
        int port = Integer.parseInt(args[1]);
        
        //open a connection
        Connection connection = new Connection(args[0],port);
        
        //initialize it
        connection.initialize();
        
        Message m = null;
        
        //read messages
        ObjectInputStream in = new ObjectInputStream(new FileInputStream
                                                            ("messages.dat"));
        Vector messages = (Vector)in.readObject();

        in.close();

        //start a thread to read messages
        Thread receiveMessageThread = 
            new Thread(new ReceiveMessageThread(connection));
        receiveMessageThread.setDaemon(true);
        receiveMessageThread.start();
        
        //get the enum
        Enumeration enum = messages.elements();
        
        //get the first message 
        TimeAndMessage mt = (TimeAndMessage)enum.nextElement();
        
        //get the time lag
        long timeLag = System.currentTimeMillis() - mt.getTime();
        
        //send the message 
        connection.send(mt.getMessage());
        //flush it
        connection.flush();
        
        while(enum.hasMoreElements())
        {
            mt = (TimeAndMessage)enum.nextElement();
            
            try
            {
                long sleepTime = mt.getTime() 
                    + timeLag - System.currentTimeMillis();
                if(sleepTime > 0)
                {
                    Thread.sleep(sleepTime);
                }
            }
            catch(InterruptedException ie)
            {
                //do nothing
            }
            
            //send message
            connection.send(mt.getMessage());
            //flush it
            connection.flush();
        }
        
        
        
    }
    catch(Exception e)
    {
        e.printStackTrace();
    }
 
}//end of fn main

private static void syntaxError() 
{
    System.err.println(
        "Syntax: java com.limegroup.gnutella.tests.MessageSimulator "+
        "<host> <port>");
    System.exit(1);
}

private static class ReceiveMessageThread implements Runnable
{
    private Connection conn;
    
    public ReceiveMessageThread(Connection conn)
    {
        this.conn = conn;
    }
    
    public void run()
    {
        
        while(true)
        {
            try
            {
                conn.receive();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    
    
}


}



