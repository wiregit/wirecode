package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.*;
import com.sun.java.util.collections.Arrays;

/**
 * A message and the connection from which it was read.  Useful for testing.
 */
class ReadPair {
    Connection connection;
    Message message;

    public ReadPair(Connection connection, Message message) {
        this.connection=connection;
        this.message=message;
    }

    public boolean equals(Object o) {
        if (! (o instanceof ReadPair))
            return false;
        ReadPair other=(ReadPair)o;
        return this.connection==other.connection 
            && Arrays.equals(this.message.getGUID(), other.message.getGUID());            
    }
}
