package com.limegroup.gnutella;

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

}