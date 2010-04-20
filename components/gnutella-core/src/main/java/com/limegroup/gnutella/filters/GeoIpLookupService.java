package com.limegroup.gnutella.filters;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.IP;
import org.limewire.io.IpPort;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.PushEndpoint;
import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;

@Singleton
public class GeoIpLookupService {
    
    private static final Log LOG = LogFactory.getLog(GeoIpLookupService.class);

    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduledExecutorService;
    private volatile LookupService lookupService;
    private final File geoIpDatabaseFile = new File(CommonUtils.getUserSettingsDir(), "GeoIP.dat");
    
    @Inject
    public GeoIpLookupService(@Named("backgroundExecutor") ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }
    
    public Country getCountry(IP ip) {
        start();
        LookupService copy = lookupService;
        if (copy != null) {
            Country country = copy.getCountry(ip.addr & 0xFFFFFFFFL);
            return country;
        }
        return null;
    }
    
    
    
    public Country getCountry(InetAddress address) {
        start();
        LookupService copy = lookupService;
        if (copy != null) {
            Country country = copy.getCountry(address);
            return country;
        }
        return null;
    }

    public Country getCountry(String address) {
        start();
        LookupService copy = lookupService;
        if (copy != null) {
            return copy.getCountry(address);
        }
        return null;
    }
    
    public Country getCountry(Address address) {
        start();
        LookupService copy = lookupService;
        if (copy != null) {
            if (address instanceof Connectable) {
                return copy.getCountry(((Connectable)address).getInetAddress());
            }
            if (address instanceof PushEndpoint) {
                IpPort ipPort = ((PushEndpoint)address).getValidExternalAddress();
                if (ipPort != null) {
                    return copy.getCountry(ipPort.getInetAddress());
                }
            }
        }
        return null;
    }
    
    private void start() {
        if (loaded.compareAndSet(false, true)) {
            LOG.debug("scheduling load");
            scheduledExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    loadLookupService();
                }
            });
        }
    }
    
    private void loadLookupService() {
        try {
            if (!geoIpDatabaseFile.exists()) {
                LOG.debug("copy db file");
                CommonUtils.copyResourceFile("org/limewire/GeoIP.dat", geoIpDatabaseFile, false);
            }
            lookupService = new LookupService(geoIpDatabaseFile, LookupService.GEOIP_MEMORY_CACHE);
        } catch (IOException e) {
            LOG.debugf("error", e);
            throw new RuntimeException(e);
        }
    }


}
