package com.limegroup.gnutella.filters;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

@Singleton
public class GeoIpLookupService {
    
    private static final Log LOG = LogFactory.getLog(GeoIpLookupService.class);

    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduledExecutorService;
    private volatile LookupService lookupService;
    private final File geoIpDatabaseFile = new File(CommonUtils.getUserSettingsDir(), "GeoLiteCit.dat");
    
    @Inject
    public GeoIpLookupService(@Named("backgroundExecutor") ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }
    
    public Location getLocation(IP ip) {
        start();
        LookupService copy = lookupService;
        if (copy != null) {
            return copy.getLocation(ip.addr);
        }
        return null;
    }
    
    public Location getLocation(InetAddress address) {
        start();
        LookupService copy = lookupService;
        if (copy != null) {
            return copy.getLocation(address);
        }
        return null;
    }

    public Location getLocation(String address) {
        start();
        LookupService copy = lookupService;
        if (copy != null) {
            try {
                return getLocation(InetAddress.getByName(address));
            } catch (UnknownHostException bad) {
            }
            return copy.getLocation(address);
        }
        return null;
    }
    
    public Location getLocation(Address address) {
        start();
        LookupService copy = lookupService;
        if (copy != null) {
            if (address instanceof Connectable) {
                return copy.getLocation(((Connectable)address).getInetAddress());
            }
            if (address instanceof PushEndpoint) {
                IpPort ipPort = ((PushEndpoint)address).getValidExternalAddress();
                if (ipPort != null) {
                    return copy.getLocation(ipPort.getInetAddress());
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
                CommonUtils.copyResourceFile("org/limewire/GeoLiteCity.dat", geoIpDatabaseFile, false);
            }
            lookupService = new LookupService(geoIpDatabaseFile, LookupService.GEOIP_MEMORY_CACHE);
        } catch (IOException e) {
            LOG.debugf("error", e);
            throw new RuntimeException(e);
        }
    }


}
