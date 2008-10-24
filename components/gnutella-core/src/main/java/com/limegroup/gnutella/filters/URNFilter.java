package com.limegroup.gnutella.filters;

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Singleton;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

import org.limewire.core.settings.FilterSettings;

@Singleton
public class URNFilter implements SpamFilter {
    
    private static final Log LOG = LogFactory.getLog(URNFilter.class);
    private static final HashSet<URN> blacklist = new HashSet<URN>();

    // Called when the spam service starts and on SIMPP updates
    public void refreshURNs() {
        blacklist.clear();
        try {
            for(String s : FilterSettings.FILTERED_URNS_LOCAL.getValue())
                blacklist.add(URN.createSHA1Urn("urn:sha1:" + s));
            for(String s : FilterSettings.FILTERED_URNS_REMOTE.getValue())
                blacklist.add(URN.createSHA1Urn("urn:sha1:" + s));
        } catch (Exception x) {
            if(LOG.isDebugEnabled())
                LOG.debug("Error creating URN blacklist: " + x);
        }
    }
    
    @Override
    public boolean allow(Message m) {
        if(m instanceof QueryReply) {
            QueryReply q = (QueryReply)m;
            try {
                for(Response r : q.getResultsArray()) {
                    for(URN u : r.getUrns()) {
                        if(blacklist.contains(u)) {
                            if(LOG.isDebugEnabled())
                                LOG.debug("Filtering response with URN " + u);
                            return false;
                        }
                    }
                }
                return true;
            } catch (BadPacketException bpe) {
                return true;
            }
        } else if(m instanceof QueryRequest) {
            QueryRequest q = (QueryRequest)m;
            for(URN u : q.getQueryUrns()) {
                if(blacklist.contains(u)) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Filtering request with URN " + u);
                    return false;
                }
            }
            return true;
        }
        return true; // Don't block other kinds of messages
    }
}