package com.limegroup.gnutella.filters.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;

/**
 * Contains a list of black list filters and white list filters and delegates
 * filter requests to them in the following fashion:
 * 
 * It iterates over all black list filters and if one of the black list filters
 * doesn't allow the {@link Response}, it iterates over all white list filters
 * to see if one of them vetoes the decision by allowing it explicitly. If not,
 * the response will be rejected.
 * 
 * If no black filter disallows the response, it will go through.
 */
class CompoundResponseFilter implements ResponseFilter {
    
    private static final Log LOG =
        LogFactory.getLog(CompoundResponseFilter.class);
    
    private final List<ResponseFilter> blackListFilters;    
    private final List<ResponseFilter> whiteListFilters;
    
    CompoundResponseFilter(Collection<? extends ResponseFilter> blackListFilters, Collection<? extends ResponseFilter> whiteListFilters) {
        this.blackListFilters = new ArrayList<ResponseFilter>(blackListFilters);
        this.whiteListFilters = new ArrayList<ResponseFilter>(whiteListFilters);
    }
    
    @Override
    public boolean allow(QueryReply qr, Response response) {
        for(ResponseFilter blackFilter : blackListFilters) {
            if(!blackFilter.allow(qr, response)) {
                for (ResponseFilter whiteFilter : whiteListFilters) {
                    if (whiteFilter.allow(qr, response)) {
                        if(LOG.isTraceEnabled())
                            LOG.trace("Response whitelisted by " +
                                    whiteFilter.getClass().getSimpleName());
                        return true;
                    }
                }
                if(LOG.isTraceEnabled())
                    LOG.trace("Response blacklisted by " +
                            blackFilter.getClass().getSimpleName());
                return false;
            }
        }
        LOG.trace("Response not blacklisted or whitelisted");
        return true;
    }

}
