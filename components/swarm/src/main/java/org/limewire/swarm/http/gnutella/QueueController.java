package org.limewire.swarm.http.gnutella;

import org.apache.http.ProtocolException;
import org.apache.http.nio.IOControl;

/**
 * A controller for remote download queues.
 */
public interface QueueController {

    /**
     * Adds a connection controlled by an {@link IOControl} to the queue.
     * The length of the queue is determined by the queue header.
     * <br/>
     * If the queue is full and this cannot take the place of another
     * item in the queue, the connection is closed.  Adding an item into
     * the queue may have a side effect of closing other queued items,
     * if they are bumped off the edge of the queue.
     * <br/>
     * The queue header must have
     *  <code>position=X, pollMin=Y, pollMax=Z</code>
     * and can optionally have other descriptors.
     * 
     * @param queueHeader the header describing how long to queue
     * @param ioctrl the i/o control to suspend/request i/o and/or close the connection
     * @throws ProtocolException If the queueHeader is invalid.
     */
    QueueInfo addToQueue(String queueHeader, IOControl ioctrl) throws ProtocolException;

    /**
     * Removes an item from the queue that was previously added.
     * 
     * If the given {@link QueueInfo} was not created by this controller,
     * it may throw {@link IllegalArgumentException}.
     * 
     * @param queueInfo A {@link QueueInfo} that was previously returned by {@link #addToQueue(String, IOControl)}.
     */
    void removeFromQueue(QueueInfo queueInfo);
    
    /**
     * Sets the maximum capacity.
     */
    void setMaxQueueCapacity(int maxCapacity);

}
