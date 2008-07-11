package org.limewire.swarm.http.gnutella;

public interface QueueInfo {

    /** Returns true if the connection is currently queued. */
    boolean isQueued();
    
    /** Enqueues this queue object. */
    void enqueue();
    
    /** Dequeues this queue object. */
    void dequeue();

}
