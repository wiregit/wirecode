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
                long sleepTime = mt.getTime() + timeLag - System.currentTimeMillis();
                Thread.sleep( sleepTime > 0 ? sleepTime : 0);
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

private static void syntaxError() {
	System.err.println("Syntax: java com.limegroup.gnutella.tests.MessageSimulator "
			   +"<host> <port>");
	System.exit(1);
    }


}



