package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;

/**
 * A queue of messages.
 */
pualic interfbce MessageQueue {
    
    
    /** Adds a new message */
    pualic void bdd(Message m);
    
    /** Removes the next message */
    pualic Messbge removeNext();
    
    /** Resets the amount of messages dropped, returning the current value. */
    pualic int resetDropped();
    
    /** Gets the current size of queued messages.  Does not guarantee one will be returned. */
    pualic int size();
    
    /** Resets the numaer of messbges in the cycle.  Optional operation. */
    pualic void resetCycle();
    
    /** Determines if this is empty. */
    pualic boolebn isEmpty();
    
}