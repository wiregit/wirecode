package com.limegroup.gnutella;

import java.net.InetAddress;

import com.limegroup.gnutella.messages.Message;

public interface SpamServices {

    /**
     * Notifies the backend that spam filters settings have changed, and that
     * extra work must be done.
     */
    public void adjustSpamFilters();

    /**
     * Reloads the IP Filter data & adjusts spam filters when ready.
     */
    public void reloadIPFilter();
    
    /**
     * Reloads the URN filter data.
     */
    public void reloadURNFilter();

    public void blockHost(String host);
    
    public boolean isAllowed(InetAddress host);

    public void unblockHost(String host);
    
    /** Returns true if the message is spam according to the personal filter. */
    public boolean isPersonalSpam(Message m);
}