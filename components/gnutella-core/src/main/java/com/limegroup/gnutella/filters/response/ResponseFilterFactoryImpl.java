package com.limegroup.gnutella.filters.response;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ResponseFilterFactoryImpl implements ResponseFilterFactory {
    
    private final Provider<MandragoreWormFilter> wormFilter;
    private final Provider<ResponseQueryFilter> queryFilter;
    private final Provider<ResponseTypeFilter> typeFilter;
    private final Provider<SecureResultFilter> secureFilter;
    
    @Inject
    public ResponseFilterFactoryImpl(Provider<MandragoreWormFilter> wormFilter,
            Provider<ResponseQueryFilter> queryFilter,
            Provider<ResponseTypeFilter> typeFilter,
            Provider<SecureResultFilter> secureFilter) {
        this.wormFilter = wormFilter;
        this.queryFilter = queryFilter;
        this.typeFilter = typeFilter;
        this.secureFilter = secureFilter;
    }
    
    @Override
    public ResponseFilter createResponseFilter() {
        List<ResponseFilter> filters = new ArrayList<ResponseFilter>();
        
        filters.add(wormFilter.get());
        filters.add(queryFilter.get());
        filters.add(typeFilter.get());
        filters.add(secureFilter.get());
        
        return new CompoundResponseFilter(filters);
    }

}
