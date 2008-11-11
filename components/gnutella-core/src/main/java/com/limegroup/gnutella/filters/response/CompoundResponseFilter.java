package com.limegroup.gnutella.filters.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.QueryReply;

class CompoundResponseFilter implements ResponseFilter {
    
    private final List<ResponseFilter> filters = new ArrayList<ResponseFilter>();
    
    CompoundResponseFilter(Collection<? extends ResponseFilter> filters) {
        this.filters.addAll(filters);
    }
    
    @Override
    public boolean allow(QueryReply qr, Response response) {
        for(ResponseFilter filter : filters) {
            if(!filter.allow(qr, response)) {
                return false;
            }
        }
        
        return true;
    }

}
