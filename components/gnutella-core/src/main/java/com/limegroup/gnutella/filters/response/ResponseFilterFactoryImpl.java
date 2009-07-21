package com.limegroup.gnutella.filters.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.core.settings.FilterSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.filters.KeywordFilter;

@Singleton
class ResponseFilterFactoryImpl implements ResponseFilterFactory {
    
    private final Provider<MandragoreWormFilter> wormFilter;
    private final Provider<ResponseQueryFilter> queryFilter;
    private final Provider<ResponseTypeFilter> typeFilter;
    private final Provider<SecureResultFilter> secureFilter;
    private final Provider<ProgramsFilter> programsFilter;
    private final Provider<WhiteListUpdateUrnFilter> whiteListUpdateUrnFilter;
    private final Provider<KeywordFilter> keywordFilter;
    private final Provider<MutableGUIDFilter> mutableGUIDFilter;
    
    @Inject
    public ResponseFilterFactoryImpl(Provider<MandragoreWormFilter> wormFilter,
            Provider<ResponseQueryFilter> queryFilter,
            Provider<ResponseTypeFilter> typeFilter,
            Provider<SecureResultFilter> secureFilter,
            Provider<ProgramsFilter> programsFilter,
            Provider<WhiteListUpdateUrnFilter> whiteListUpdateUrnFilter,
            Provider<KeywordFilter> keywordFilter,
            Provider<MutableGUIDFilter> mutableGUIDFilter) {
        this.wormFilter = wormFilter;
        this.queryFilter = queryFilter;
        this.typeFilter = typeFilter;
        this.secureFilter = secureFilter;
        this.programsFilter = programsFilter;
        this.whiteListUpdateUrnFilter = whiteListUpdateUrnFilter;
        this.keywordFilter = keywordFilter;
        this.mutableGUIDFilter = mutableGUIDFilter;
    }
    
    @Override
    public ResponseFilter createResponseFilter() {
        List<ResponseFilter> filters = new ArrayList<ResponseFilter>();
        
        filters.add(keywordFilter.get());
        if(FilterSettings.FILTER_WHATS_NEW_ADULT.getValue())
            filters.add(mutableGUIDFilter.get());
        filters.add(wormFilter.get());
        filters.add(queryFilter.get());
        filters.add(typeFilter.get());
        filters.add(secureFilter.get());
        filters.add(programsFilter.get());
        
        return new CompoundResponseFilter(filters,
                Collections.singletonList(whiteListUpdateUrnFilter.get()));
    }
}
