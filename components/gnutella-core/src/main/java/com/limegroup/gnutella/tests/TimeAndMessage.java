package com.limegroup.gnutella.tests;

import com.limegroup.gnutella.Message;
import java.io.*;


public class TimeAndMessage implements Serializable 
{
    private long time;

    private Message message;

    
    public TimeAndMessage(long time, Message message) 
    {
        this.time = time;
        this.message = message;
    }
    
    public void set(long time, Message message) 
    {
        this.time = time;
        this.message = message;
    }

    public long getTime()
    {
        return time;
    }
    
    public Message getMessage()
    {
        return message;
    }

}
