package com.limegroup.gnutella;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inject.Providers;

import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.filters.response.FilterFactory;
import com.limegroup.gnutella.search.SearchResultHandler;

import org.jmock.Mockery;
import org.jmock.Expectations;

public class SpamServicesImplTest extends LimeTestCase {
    private Mockery mockery;
    private IPFilter ipFilter;
    private URNFilter urnFilter;
    private SpamServicesImpl spamServices;

    public SpamServicesImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() {
        mockery = new Mockery();
        ipFilter = mockery.mock(IPFilter.class);
        urnFilter = mockery.mock(URNFilter.class);
        SpamFilterFactory spamFilterFactory = mockery.mock(SpamFilterFactory.class);
        ConnectionManager connectionManager = mockery.mock(ConnectionManager.class);
        SearchResultHandler searchResultHandler = mockery.mock(SearchResultHandler.class);
        FilterFactory responseFilterFactory = mockery.mock(FilterFactory.class);
        spamServices = new SpamServicesImpl(Providers.of(connectionManager),
                Providers.of(ipFilter),
                Providers.of(urnFilter),
                spamFilterFactory,
                Providers.of(searchResultHandler),
                responseFilterFactory);
    }

    @Override
    protected void tearDown(){}

    public void testReloadSpamFilters() {
        mockery.checking(new Expectations(){{
            one(urnFilter).refreshURNs(with(any(SpamFilter.LoadCallback.class)));
            one(ipFilter).refreshHosts(with(any(SpamFilter.LoadCallback.class)));
        }});
        spamServices.reloadSpamFilters();
        mockery.assertIsSatisfied();
    }
}
