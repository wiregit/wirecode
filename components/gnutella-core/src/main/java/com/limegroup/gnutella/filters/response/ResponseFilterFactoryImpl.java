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
    private final Provider<ProgramsFilter> programsFilter;
    
    @Inject
    public ResponseFilterFactoryImpl(Provider<MandragoreWormFilter> wormFilter,
            Provider<ResponseQueryFilter> queryFilter,
            Provider<ResponseTypeFilter> typeFilter,
            Provider<SecureResultFilter> secureFilter,
            Provider<ProgramsFilter> programsFilter) {
        this.wormFilter = wormFilter;
        this.queryFilter = queryFilter;
        this.typeFilter = typeFilter;
        this.secureFilter = secureFilter;
        this.programsFilter = programsFilter;
    }
    
    @Override
    public ResponseFilter createResponseFilter() {
        List<ResponseFilter> filters = new ArrayList<ResponseFilter>();
        
        filters.add(wormFilter.get());
        filters.add(queryFilter.get());
        filters.add(typeFilter.get());
        filters.add(secureFilter.get());
        filters.add(programsFilter.get());
        
        return new CompoundResponseFilter(filters);
    }

}
