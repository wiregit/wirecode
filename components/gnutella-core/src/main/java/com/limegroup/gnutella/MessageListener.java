package com.limegroup.gnutella;

import com.limegroup.gnutella.messages.Message;

/**
 * This interface should be extended if you want to get notified of certain
 * messages.  The MessageRouter will process messages as usual but then hand
 * off messages (by guid) for special handling.  You have to register with the
 * MessageRouter for this.
 */
public interface MessageListener {

    /** PRE: m.getGUID() is equal to the GUID you registered for.
     */
    public void processMessage(Message m);

}