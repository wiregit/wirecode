package com.limegroup.gnutella.bootstrap;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.inspection.InspectablePrimitive;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ConnectionServices;
import com.limegroup.gnutella.ExtendedEndpoint;
import com.limegroup.gnutella.MulticastService;
import com.limegroup.gnutella.messages.PingRequest;
import com.limegroup.gnutella.messages.PingRequestFactory;

@Singleton
class BootstrapperImpl implements Bootstrapper {

    private static final Log LOG = LogFactory.getLog(BootstrapperImpl.class);

    /** Milliseconds to wait between multicast fetches. */
    static int MULTICAST_INTERVAL = 50 * 1000;

    /** Milliseconds to wait after trying multicast before falling back to UDP. */
    static int UDP_FALLBACK_DELAY = 5 * 1000;

    /** Milliseconds to wait between UDP fetches. */
    static int UDP_INTERVAL = 30 * 1000;

    /** Milliseconds to wait after trying UDP before falling back to TCP. */
    static int TCP_FALLBACK_DELAY = 40 * 1000;

    /** Milliseconds to wait between TCP fetches. */
    static int TCP_INTERVAL = 5 * 60 * 1000;        

    /** The time at which we started to think about bootstrapping. */
    private long firstBootstrapCheck = 0;

    /**
     * The next allowed multicast time.
     * Incremented after each attempted multicast fetch.
     */
    private long nextAllowedMulticastTime = 0; // Immediately

    /**
     * The next allowed UDP fetch time.
     * Incremented after each attempted UDP fetch.
     */
    private long nextAllowedUdpTime = Long.MAX_VALUE; // Not just yet

    /**
     * The next allowed TCP fetch time.
     * Incremented after each attempted TCP fetch.
     */
    private long nextAllowedTcpTime = Long.MAX_VALUE; // Not just yet

    /**
     * Inspection points to see which bootstrap techniques are used.
     */
    @InspectablePrimitive("multicast bootstrapping")
    @SuppressWarnings("unused")
    private boolean triedMulticast = false;
    @InspectablePrimitive("udp bootstrapping")
    @SuppressWarnings("unused")
    private boolean triedUDP = false;
    @InspectablePrimitive("tcp bootstrapping")
    @SuppressWarnings("unused")
    private boolean triedTCP = false;
    
    private final ConnectionServices connectionServices;
    private final Provider<MulticastService> multicastService;
    private final PingRequestFactory pingRequestFactory;
    private final Bootstrapper.Listener listener;
    private final TcpBootstrap tcpBootstrap;
    private final UDPHostCache udpHostCache;

    @Inject
    public BootstrapperImpl(ConnectionServices connectionServices,
            Provider<MulticastService> multicastService,
            PingRequestFactory pingRequestFactory,
            Bootstrapper.Listener listener,
            TcpBootstrap tcpBootstrap,
            UDPHostCache udpHostCache) {
        this.connectionServices = connectionServices;
        this.multicastService = multicastService;
        this.pingRequestFactory = pingRequestFactory;
        this.listener = listener;
        this.tcpBootstrap = tcpBootstrap;
        this.udpHostCache = udpHostCache;
    }

    /**
     * Determines whether or not it is time to get more hosts,
     * and if we need them, gets them.
     */
    @Override
    public synchronized void run() {            
        if(ConnectionSettings.DO_NOT_BOOTSTRAP.getValue()) {
            LOG.trace("Not bootstrapping");
            return;
        }

        long now = System.currentTimeMillis();
        if(firstBootstrapCheck == 0)
            firstBootstrapCheck = now;

        // If we need endpoints, try any bootstrapping methods that
        // haven't been tried too recently
        if(needsHosts(now)) {
            multicastFetch(now);
            udpHostCacheFetch(now);
            tcpHostCacheFetch(now);
        }
    }

    @Override
    public void reset() {
        firstBootstrapCheck = 0;
        nextAllowedMulticastTime = 0;
        nextAllowedUdpTime = Long.MAX_VALUE;
        nextAllowedTcpTime = Long.MAX_VALUE;
        tcpBootstrap.resetData();
        udpHostCache.resetData();
        tcpBootstrap.loadDefaults();
        udpHostCache.loadDefaults();
    }

    @Override
    public boolean addUDPHostCache(ExtendedEndpoint ee) {
        return udpHostCache.add(ee);
    }

    @Override
    public boolean isWriteDirty() {
        return udpHostCache.isWriteDirty();
    }

    @Override
    public void write(Writer out) throws IOException {
        udpHostCache.write(out);
    }

    /**
     * Determines whether or not we need more hosts.
     */
    private boolean needsHosts(long now) {
        if(listener.needsHosts()) {
            LOG.trace("Need hosts: none known");
            return true;
        }
        long delay = now - firstBootstrapCheck;
        if(!connectionServices.isConnected() &&
                delay > ConnectionSettings.BOOTSTRAP_DELAY.getValue()) {
            if(LOG.isTraceEnabled())
                LOG.trace("Need hosts: not connected after " +
                        delay + " milliseconds");
            return true;
        }
        LOG.trace("Do not need hosts");
        return false;
    }

    /**
     * Considers fetching hosts via multicast and returns true if a fetch
     * was attempted.
     */
    private boolean multicastFetch(long now) {
        if(ConnectionSettings.DO_NOT_MULTICAST_BOOTSTRAP.getValue()) {
            LOG.trace("Never fetching via multicast");
            if(nextAllowedUdpTime == Long.MAX_VALUE)
                nextAllowedUdpTime = 0;
            return false;
        }
        if(nextAllowedMulticastTime < now) {
            LOG.trace("Fetching via multicast");
            PingRequest pr = pingRequestFactory.createMulticastPing();
            multicastService.get().send(pr);
            triedMulticast = true;
            nextAllowedMulticastTime = now + MULTICAST_INTERVAL;
            // If this is the first multicast fetch, set the UDP fallback time
            if(nextAllowedUdpTime == Long.MAX_VALUE)
                nextAllowedUdpTime = now + UDP_FALLBACK_DELAY;
            return true;
        }
        LOG.trace("Not fetching via multicast");
        return false;
    }

    /**
     * Considers fetching hosts via UDP and returns true if a fetch was
     * attempted.
     */
    private boolean udpHostCacheFetch(long now) {
        if(nextAllowedUdpTime < now) {
            LOG.trace("Fetching via UDP");
            udpHostCache.fetchHosts();
            triedUDP = true;
            nextAllowedUdpTime = now + UDP_INTERVAL;
            // If this is the first UDP fetch, set the TCP fallback time
            if(nextAllowedTcpTime == Long.MAX_VALUE)
                nextAllowedTcpTime = now + TCP_FALLBACK_DELAY;
            return true;
        }
        LOG.trace("Not fetching via UDP");
        return false;
    }

    /**
     * Considers fetching hosts via TCP and returns true if a fetch was
     * attempted.
     */
    private boolean tcpHostCacheFetch(long now) {
        if(nextAllowedTcpTime < now) {
            LOG.trace("Fetching via TCP");
            tcpBootstrap.fetchHosts(listener);
            triedTCP = true;
            nextAllowedTcpTime = now + TCP_INTERVAL;
            return true;
        }
        LOG.trace("Not fetching via TCP");
        return false;
    }
}
