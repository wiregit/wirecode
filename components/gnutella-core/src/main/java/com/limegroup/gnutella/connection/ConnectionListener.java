package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.Connection;
import com.limegroup.gnutella.Message;
import com.limegroup.gnutella.BadPacketException;

/**
 * An observer of Connection events.  Used to drive messaging connections with
 * non-blocking or blocking IO.  
 */
public interface ConnectionListener {
    /** Notifies this that c has completed the handshaking process and is ready
     *  for normal messaging. */
    public void initialized(Connection c);

    /** Notifies this that c successfully read the message m. */
    public void read(Connection c, Message m);

    /** Notifies this that c encountered a bad message but was able to continue
     *  Usually this can be ignored. */
    public void read(Connection c, BadPacketException error);


    /** Notifies this that c has data queued for writing, requiring a call to
     *  c.write().  In a non-blocking system, this typically means that c should
     *  be registered with a selector for the OP_WRITE (and OP_READ) events.  In
     *  a blocking system, this typically means that c's writer thread should be
     *  notified. */
    public void needsWrite(Connection c);

    /** Notifies this that c has been closed, received an IOException, or
     *  encountered some other fatal error and should be cleaned up.  c may
     *  or may not already be closed.  */
    public void error(Connection c);
}
