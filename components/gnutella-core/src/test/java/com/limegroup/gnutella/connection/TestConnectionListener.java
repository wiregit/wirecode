package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.*;

/**
 * A message and the connection from which it was read.  Useful for testing.
 */
public class TestConnectionListener implements ConnectionListener {
    public boolean initialized=false;        
    public boolean closed=false;

    /** The last message received. */
    public Message message;
    /** All messages received, with the latest at the tail. */
    public List /* of Messages */ messages=new ArrayList();
    /** Any bad packets received? */
    public BadPacketException error;

    public boolean needsWrite=false;


    public void initialized(Connection c) { 
        initialized=true;
    }

    public void read(Connection c, Message m) { 
        message=m;
        messages.add(m);
    }

    public void read(Connection c, BadPacketException error) { 
        this.error=error;
    }

    public void needsWrite(Connection c) { 
        needsWrite=true;
    }

    public void error(Connection c) { 
        this.closed=true;
    }
    
    public boolean normal() {
        return initialized && closed==false && needsWrite==false && error==null;
    }
}
