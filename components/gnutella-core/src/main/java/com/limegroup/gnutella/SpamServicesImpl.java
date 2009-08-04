package com.limegroup.gnutella;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import org.limewire.collection.Comparators;
import org.limewire.core.settings.FilterSettings;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.connection.RoutedConnection;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.filters.SpamFilter;
import com.limegroup.gnutella.filters.SpamFilterFactory;
import com.limegroup.gnutella.filters.URNFilter;
import com.limegroup.gnutella.filters.response.ResponseFilterFactory;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.search.SearchResultHandler;

@Singleton
public class SpamServicesImpl implements SpamServices {

    private final Provider<ConnectionManager> connectionManager;
    private final Provider<IPFilter> ipFilter;
    private final Provider<URNFilter> urnFilter;
    private final SpamFilterFactory spamFilterFactory;
    private final SearchResultHandler searchResultHandler;
    private final ResponseFilterFactory responseFilterFactory;
    private volatile SpamFilter personalFilter;

    @Inject
    public SpamServicesImpl(Provider<ConnectionManager> connectionManager,
            Provider<IPFilter> ipFilter, Provider<URNFilter> urnFilter,
            SpamFilterFactory spamFilterFactory,
            SearchResultHandler searchResultHandler,
            ResponseFilterFactory responseFilterFactory) {
        this.connectionManager = connectionManager;
        this.ipFilter = ipFilter;
        this.urnFilter = urnFilter;
        this.spamFilterFactory = spamFilterFactory;
        this.searchResultHandler = searchResultHandler;
        this.responseFilterFactory = responseFilterFactory;
        personalFilter = spamFilterFactory.createPersonalFilter();
    }

    @Override
    public void adjustSpamFilters() {
        personalFilter = spamFilterFactory.createPersonalFilter();
        searchResultHandler.setResponseFilter(responseFilterFactory.createResponseFilter());
        
        // Replace the route filter on each connection
        for(RoutedConnection c : connectionManager.get().getConnections()) {
            if(ipFilter.get().allow(c.getAddress())) {
                c.setRouteFilter(spamFilterFactory.createRouteFilter());
            } else {
                // If the connection isn't allowed now, close it.
                c.close();
            }
        }

        // TODO: notify DownloadManager & UploadManager about new banned IP ranges
    }

    @Override
    public void reloadIPFilter() {
        ipFilter.get().refreshHosts(new IPFilter.IPFilterCallback() {
            public void ipFiltersLoaded() {
                adjustSpamFilters();
            }
        });
    }

    @Override
    public void reloadURNFilter() {
        urnFilter.get().refreshURNs();
    }

    @Override
    public boolean isAllowed(InetAddress host) {
        return ipFilter.get().allow(host.getAddress());
    }

    @Override
    public void blockHost(String host) {
        // FIXME move into IPFilter
        // FIXME synchronize access to setting properly?
        String[] bannedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.get();
        Arrays.sort(bannedIPs, Comparators.stringComparator());

        if (Arrays.binarySearch(bannedIPs, host, Comparators.stringComparator()) < 0) {
            String[] more_banned = new String[bannedIPs.length + 1];
            System.arraycopy(bannedIPs, 0, more_banned, 0, bannedIPs.length);
            more_banned[bannedIPs.length] = host;
            FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(more_banned);
            reloadIPFilter();
        }
    }

    @Override
    public void unblockHost(String host) {
        // FIXME move into IPFilter
        // FIXME synchronize access to setting properly?
        String[] bannedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.get();
        List<String> bannedList = Arrays.asList(bannedIPs);
        if (bannedList.remove(host)) {
            FilterSettings.BLACK_LISTED_IP_ADDRESSES.set(bannedList.toArray(new String[0]));
            reloadIPFilter();
        }
    }

    @Override
    public boolean isPersonalSpam(Message m) {
        return !personalFilter.allow(m);
    }
}
