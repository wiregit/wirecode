package com.limegroup.gnutella.filters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.IP;

public class LanFilter extends AbstractIPFilter {

    private static final Log LOG = LogFactory.getLog(LanFilter.class);

    private final IP loopback = new IP("127.0.0.0/8");
    private final IP private8 = new IP("10.0.0.0/8");
    private final IP private12 = new IP("172.16.0.0/12");
    private final IP private16 = new IP("192.168.0.0/16");

    @Override
    protected boolean allowImpl(IP ip) {
        if(!ConnectionSettings.LAN_MODE.getValue())
            return true;
        if(loopback.contains(ip) || private8.contains(ip) ||
                private12.contains(ip) || private16.contains(ip)) {
            if(LOG.isDebugEnabled())
                LOG.debug("Allowing " + ip);
            return true;
        }
        LOG.debug("Not allowing " + ip);
        return false;
    }

    @Override
    public boolean hasBlacklistedHosts() {
        return ConnectionSettings.LAN_MODE.getValue();
    }

    @Override
    public void refreshHosts() {
    }

    @Override
    public void refreshHosts(LoadCallback callback) {
        if(callback != null)
            callback.spamFilterLoaded();
    }
}
