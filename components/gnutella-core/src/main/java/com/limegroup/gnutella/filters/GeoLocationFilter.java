package com.limegroup.gnutella.filters;

import java.util.Collections;
import java.util.Set;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.IP;
import org.limewire.io.IpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.util.StringUtils;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.filters.response.ResponseFilter;
import com.limegroup.gnutella.messages.QueryReply;
import com.maxmind.geoip.Location;

public class GeoLocationFilter extends AbstractIPFilter implements ResponseFilter {
    
    private static final Log LOG = LogFactory.getLog(GeoLocationFilter.class);

    private volatile Set<String> filteredCountries = Collections.emptySet();
    private volatile Set<String> allowedCountries = Collections.emptySet();
    
    private final GeoIpLookupService geoIpLookupService; 
    
    @Inject
    public GeoLocationFilter(GeoIpLookupService geoIpLookupService) {
        this.geoIpLookupService = geoIpLookupService;
        loadFilteredCountries();
    }
    
    @Inject
    void registerSettingsListener() {
        SettingListener listener = new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                loadFilteredCountries();
            }
        };
        ConnectionSettings.FILTERED_COUNTRIES.addSettingListener(listener);
        ConnectionSettings.ALLOWED_COUNTRIES.addSettingListener(listener);
    }
    
    private void loadFilteredCountries() {
        ImmutableSet<String> localFiltered = ImmutableSet.copyOf(ConnectionSettings.FILTERED_COUNTRIES.get());
        ImmutableSet<String> localAllowed = ImmutableSet.copyOf(ConnectionSettings.ALLOWED_COUNTRIES.get());
        LOG.debugf("filtered {0}", localFiltered);
        LOG.debugf("allowed {0}", localAllowed);
        filteredCountries = localFiltered;
        allowedCountries = localAllowed;
    }

    @Override
    protected boolean allowImpl(IP ip) {
        Location location = geoIpLookupService.getLocation(ip);
        if (location == null) {
            LOG.debugf("location null for {0}", ip);
            return false;
        }
        if (filteredCountries.contains(location.countryCode)) {
            LOG.debugf("filtered {0}", StringUtils.toString(location));
            return false;
        } else if (!allowedCountries.isEmpty() && !allowedCountries.contains(location.countryCode)) {
            LOG.debugf("not allowed {0}", StringUtils.toString(location));
            return false;
        }
        return true;
    }

    @Override
    public boolean hasBlacklistedHosts() {
        return true;
    }

    @Override
    public void refreshHosts() {
    }

    @Override
    public void refreshHosts(LoadCallback callback) {
    }

    @Override
    public boolean allow(QueryReply qr, Response response) {
        if (!allow(qr)) {
            return false;
        }
        for (IpPort ip : response.getLocations()) {
            if (!allow(ip.getAddress())) {
                return false;
            }
        }
        return true;
    }

}
