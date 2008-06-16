package org.limewire.swarm.http.gnutella;

public interface QueueInfo {

    /** Returns true if the connection is queued with respect to the current time. */
    boolean isQueued();
    
    /** Enqueues this queue object. */
    void enqueue();
    
    /** Dequeues this queue object. */
    void dequeue();

}
