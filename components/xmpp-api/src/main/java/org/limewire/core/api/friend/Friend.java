package org.limewire.core.api.friend;

/** 
 * A Friend.
 */
// TODO: Move this out of xmpp-api and into something that different
//       friend extensions can share.
public interface Friend {
    
    /**
     * @return the id of the user.  user-ids have the form <code>user@host.com</code>
     */
    public String getId();

    /**
     * @return the friendly user given name to the user; can be null.
     */
    public String getName();

}
