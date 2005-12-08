pbckage com.limegroup.gnutella;

import com.limegroup.gnutellb.messages.Message;

/**
 * This interfbce should be extended if you want to get notified of certain
 * messbges.  The MessageRouter will process messages as usual but then hand
 * off messbges (by guid) for special handling.  You have to register with the
 * MessbgeRouter for this.
 */
public interfbce MessageListener {

    /**
     * Cbllback for processing a message.
     *
     * This is intended to be used for processing messbges
     * with b specific GUID.
     */
    public void processMessbge(Message m, ReplyHandler handler);
    
    /**
     * Cbllback notifying this MessageListener that it is now registered
     * for listening to messbge with the specified guid.
     */
    public void registered(byte[] guid);
    
    /**
     * Cbllback notifying this MessageListener that it is now unregistered
     * for listening to messbges with the specified guid.
     */
    public void unregistered(byte[] guid);

}
