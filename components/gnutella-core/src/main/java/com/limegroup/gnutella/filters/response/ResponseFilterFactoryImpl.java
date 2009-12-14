package com.limegroup.gnutella.filters.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.core.settings.FilterSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.filters.KeywordFilter;
import com.limegroup.gnutella.filters.PhraseFilter;
import com.limegroup.gnutella.filters.URNFilter;

@Singleton
class ResponseFilterFactoryImpl implements ResponseFilterFactory {

    private final Provider<XMLDocFilter> xmlDocFilter;
    private final Provider<MandragoreWormFilter> wormFilter;
    private final Provider<ResponseQueryFilter> queryFilter;
    private final Provider<ProgramsFilter> programsFilter;
    private final Provider<SecureResultFilter> secureResultFilter;
    private final Provider<ResponseTypeFilter> typeFilter;
    private final Provider<WhiteListUpdateUrnFilter> whiteListUpdateUrnFilter;
    private final Provider<MutableGUIDFilter> mutableGUIDFilter;
    private final Provider<KeywordFilter> keywordFilter;
    private final Provider<URNFilter> urnFilter;
    private final Provider<AltLocFilter> altLocFilter;
    private final Provider<PhraseFilter> phraseFilter;

    @Inject
    public ResponseFilterFactoryImpl(Provider<XMLDocFilter> xmlDocFilter,
            Provider<MandragoreWormFilter> wormFilter,
            Provider<ResponseQueryFilter> queryFilter,
            Provider<ProgramsFilter> programsFilter,
            Provider<SecureResultFilter> secureResultFilter,
            Provider<ResponseTypeFilter> typeFilter,
            Provider<WhiteListUpdateUrnFilter> whiteListUpdateUrnFilter,
            Provider<MutableGUIDFilter> mutableGUIDFilter,
            Provider<KeywordFilter> keywordFilter,
            Provider<URNFilter> urnFilter,
            Provider<AltLocFilter> altLocFilter,
            Provider<PhraseFilter> phraseFilter) {
        this.xmlDocFilter = xmlDocFilter;
        this.wormFilter = wormFilter;
        this.queryFilter = queryFilter;
        this.programsFilter = programsFilter;
        this.secureResultFilter = secureResultFilter;
        this.typeFilter = typeFilter;
        this.whiteListUpdateUrnFilter = whiteListUpdateUrnFilter;
        this.mutableGUIDFilter = mutableGUIDFilter;
        this.keywordFilter = keywordFilter;
        this.urnFilter = urnFilter;
        this.altLocFilter = altLocFilter;
        this.phraseFilter = phraseFilter;
    }

    @Override
    public ResponseFilter createResponseFilter() {
        List<ResponseFilter> filters = new ArrayList<ResponseFilter>();

        filters.add(altLocFilter.get());
        filters.add(urnFilter.get());
        filters.add(keywordFilter.get());
        if(FilterSettings.FILTER_ADULT.getValue())
            filters.add(mutableGUIDFilter.get());
        filters.add(wormFilter.get());
        filters.add(queryFilter.get());
        filters.add(typeFilter.get());
        filters.add(secureResultFilter.get());
        filters.add(programsFilter.get());
        filters.add(xmlDocFilter.get());
        filters.add(phraseFilter.get());

        return new CompoundResponseFilter(filters,
                Collections.singletonList(whiteListUpdateUrnFilter.get()));
    }
}
