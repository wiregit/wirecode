package org.limewire.friend.api;

/** 
 * A Friend.
 */
public interface Friend {

    public static final String P2P_FRIEND_ID = "_@_GNUTELLA_@_";

    /**
     * Returns the ID of the friend.  This can be any form of unique ID.
     * For example, an friend can be in the form of <code>friend@host.com</code>,
     * whereas a Gnutella Friend can be the clientGUID.
     */
    public String getId();

    /**
     * Return the friendly given name to the friend, can be null.
     * For example, a friend can be the alias of the friend,
     * where a Gnutella friend can be the IP address.
     * */
    public String getName();
    
    /** Returns the best possible name this can be rendered with.
     *  
     *  NOTE: must not return null. 
     */
    public String getRenderName();
}
