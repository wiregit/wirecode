package com.limegroup.gnutella.filters;

import com.limegroup.gnutella.messages.QueryReply;

/**
 * A filter that blocks query responses with URNs that match either of
 * two blacklists, one local and one remote (SIMPP). This is designed
 * for filtering out spam and malware.
 */
public interface URNFilter extends SpamFilter {

    /**
     * Reloads the local and remote blacklists. Called when the spam service
     * starts and on SIMPP updates.
     */ 
    public void refreshURNs();

    /**
     * Returns true if any response in the query reply matches the blacklist.
     * Unlike <code>allow(Message)</code>, matching query replies are not passed
     * to the spam filter.
     */
    public boolean isSpam(QueryReply q);
}