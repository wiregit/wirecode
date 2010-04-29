package com.limegroup.gnutella.filters.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.api.search.SearchResult;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;

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
class CompoundFilter implements ResponseFilter, ResultFilter {
    
    private static final Log LOG =
        LogFactory.getLog(CompoundFilter.class);
    
    private final List<ResponseFilter> blackListResponseFilters;    
    private final List<ResponseFilter> whiteListResponseFilters;
    
    private final List<ResultFilter> blackListResultFilters;    
    private final List<ResultFilter> whiteListResultFilters;
    
    CompoundFilter(Collection<? extends ResponseFilter> blackListResponseFilters, Collection<? extends ResponseFilter> whiteListResponseFilters,
            Collection<? extends ResultFilter> blackListResultFilters, Collection<? extends ResultFilter> whiteListResultFilters) {
        this.blackListResponseFilters = new ArrayList<ResponseFilter>(blackListResponseFilters);
        this.whiteListResponseFilters = new ArrayList<ResponseFilter>(whiteListResponseFilters);
        this.blackListResultFilters = new ArrayList<ResultFilter>(blackListResultFilters);
        this.whiteListResultFilters = new ArrayList<ResultFilter>(whiteListResultFilters);
    }
    
    @Override
    public boolean allow(QueryReply qr, Response response) {
        for(ResponseFilter blackFilter : blackListResponseFilters) {
            if(!blackFilter.allow(qr, response)) {
                for (ResponseFilter whiteFilter : whiteListResponseFilters) {
                    if (whiteFilter.allow(qr, response)) {
                        if(LOG.isTraceEnabled())
                            LOG.trace("Response whitelisted by " +
                                    whiteFilter.getClass().getSimpleName() +
                                    "\n" + response);
                        return true;
                    }
                }
                if(LOG.isTraceEnabled())
                    LOG.trace("Response blacklisted by " +
                            blackFilter.getClass().getSimpleName() +
                            "\n" + response);
                return false;
            }
        }
        LOG.trace("Response not blacklisted or whitelisted");
        return true;
    }

    @Override
    public boolean allow(SearchResult result, LimeXMLDocument document) {
        for(ResultFilter blackFilter : blackListResultFilters) {
            if(!blackFilter.allow(result, document)) {
                for (ResultFilter whiteFilter : whiteListResultFilters) {
                    if (whiteFilter.allow(result, document)) {
                        if(LOG.isTraceEnabled())
                            LOG.trace("Result whitelisted by " +
                                    whiteFilter.getClass().getSimpleName() +
                                    "\n" + result);
                        return true;
                    }
                }
                if(LOG.isTraceEnabled())
                    LOG.trace("Result blacklisted by " +
                            blackFilter.getClass().getSimpleName() +
                            "\n" + result);
                return false;
            }
        }
        LOG.trace("Result not blacklisted or whitelisted");
        return true;
    }

}
