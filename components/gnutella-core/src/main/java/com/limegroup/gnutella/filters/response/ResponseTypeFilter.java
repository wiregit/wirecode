package com.limegroup.gnutella.filters.response;

import org.limewire.security.SecureMessage;

import com.google.inject.Inject;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.QueryReply;

class ResponseTypeFilter implements ResponseFilter {
    
    private final SearchServices searchServices;
    
    @Inject public ResponseTypeFilter(SearchServices searchServices) {
        this.searchServices = searchServices;
    }
    
    @Override
    public boolean allow(QueryReply qr, Response response) {
        return qr.isBrowseHostReply()
            || qr.getSecureStatus() == SecureMessage.SECURE
            || searchServices.matchesType(qr.getGUID(), response);
    }

}
