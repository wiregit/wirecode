package org.limewire.core.api.browse.server;

import java.util.Date;

import org.limewire.core.api.friend.Friend;

public interface BrowseTracker {
    /**
     * Used to track when a library refresh is sent to a friend
     * @param friend
     */
    public void sentRefresh(Friend friend);
    
    /**
     * Used to track when a browse has been done by a friend
     * @param friend
     */
    public void browsed(Friend friend);

    /**
     * @param friend
     * @return the last time the <code>friend</code> did a browse,
     * or null if they never have.
     */
    public Date lastBrowseTime(Friend friend);
    
    /**
     * @param friend
     * @return the last time a library refresh was sent to the <code>friend</code>,
     * or null if it never has.
     */
    public Date lastRefreshTime(Friend friend);
}
