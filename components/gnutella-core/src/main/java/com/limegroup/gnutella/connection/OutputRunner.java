
// Commented for the Learning branch

package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.io.Shutdownable;

/**
 * The MessageWriter class implements OutputRunner, and has a send(m) method.
 * ManagedConnection calls MessageWriter.send(m) to give it a Gnutella packet headed for the remote computer.
 * 
 * MessageWriter is the only class that LimeWire uses that implements this interface.
 */
public interface OutputRunner extends Shutdownable {

    /**
     * Call send(m) on a MessageWriter to give it a Gnutella packet to send to the remote computer.
     * 
     * @param m A Gnutella packet we're sending to the remote computer
     */
    public void send(Message m);
}
