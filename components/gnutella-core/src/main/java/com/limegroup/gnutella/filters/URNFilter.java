package com.limegroup.gnutella.filters;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.FilterSettings;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.spam.SpamManager;

/**
 * A filter that blocks query responses with URNs that match either of
 * two blacklists, one local and one remote (SIMPP). This is designed
 * for filtering out spam and malware.
 */
@Singleton
public class URNFilter implements SpamFilter {

    private static final Log LOG = LogFactory.getLog(URNFilter.class);
    private ImmutableSet<URN> blacklist = null;
    private final SpamManager spamManager;

    @Inject
    URNFilter(SpamManager spamManager) {
        this.spamManager = spamManager;
    }

    /**
     * Reloads the local and remote blacklists. Called when the spam service
     * starts and on SIMPP updates.
     */ 
    public void refreshURNs() {
        ImmutableSet.Builder<URN> builder = ImmutableSet.builder();
        try {
            for(String s : FilterSettings.FILTERED_URNS_LOCAL.get())
                builder.add(URN.createSHA1Urn("urn:sha1:" + s));
            if(FilterSettings.USE_NETWORK_FILTER.getValue()) {
                for(String s : FilterSettings.FILTERED_URNS_REMOTE.get())
                    builder.add(URN.createSHA1Urn("urn:sha1:" + s));
            }
        } catch (IOException iox) {
            LOG.debug("Error creating URN blacklist: ", iox);
        }
        blacklist = builder.build();
    }

    /**
     * Returns false if the message is a query reply with a URN that matches
     * the blacklist; matching query replies are passed to the spam filter.
     * Returns true for all other messages.
     */
    @Override
    public boolean allow(Message m) {
        if(blacklist == null)
            return true;
        if(m instanceof QueryReply) {
            QueryReply q = (QueryReply)m;
            if(isSpam(q)) {
                if(FilterSettings.FILTERED_URNS_ARE_SPAM.getValue())
                    spamManager.handleSpamQueryReply(q);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if any response in the query reply matches the blacklist.
     */
    public boolean isSpam(QueryReply q) {
        try {
            for(Response r : q.getResultsArray()) {
                for(URN u : r.getUrns()) {
                    if(blacklist.contains(u)) {
                        if(LOG.isDebugEnabled())
                            LOG.debug(u + " is spam");
                        return true;
                    }
                }
            }
            return false;
        } catch(BadPacketException bpe) {
            return true;
        }
    }
}