package org.limewire.core.impl.connection;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.limewire.core.api.connection.ConnectionLifecycleEventType;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.SwingSafePropertyChangeSupport;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class GnutellaConnectionManagerImpl implements GnutellaConnectionManager {

    /** The number of messages a connection must have sent before we consider it stable. */
    private static final int STABLE_THRESHOLD = 5;
    
    private final ConnectionManager connectionManager;
    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);
    
    private volatile long lastIdleTime;
    private volatile ConnectionStrength currentStrength = ConnectionStrength.DISCONNECTED;
    
    private volatile ConnectionLifecycleEventType lastStrengthRelatedEvent;

    @Inject
    public GnutellaConnectionManagerImpl(com.limegroup.gnutella.ConnectionManager connectionManager) {
        this.connectionManager = Objects.nonNull(connectionManager, "connectionManager");
    }
    
    @Inject void register(ServiceRegistry registry, final @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        registry.register(new Service() {
            private volatile ScheduledFuture<?> meter;
            private volatile ConnectionLifecycleListener listener;
            
            @Override
            public String getServiceName() {
                return "Connection Strength Meter";
            }
            
            @Override
            public void initialize() {
                listener = new ConnectionLifecycleListener() {
                    @Override
                    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
                        switch(evt.getType()) {
                        case NO_INTERNET:
                        case CONNECTION_INITIALIZED:
                        case CONNECTED:
                            lastStrengthRelatedEvent = evt.getType();
                            break;
                        }
                    }
                };
                connectionManager.addEventListener(listener);
            }
            
            @Override
            public void start() {
                meter = backgroundExecutor.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        setConnectionStrength(calculateStrength());
                    }
                }, 0, 1, TimeUnit.SECONDS);
            }
            
            @Override
            public void stop() {
                if(meter != null) {
                    meter.cancel(false);
                    meter = null;
                }
                if(listener != null) {
                    connectionManager.removeEventListener(listener);
                    listener = null;
                }
            }
        });
    }
    
    private void setConnectionStrength(ConnectionStrength newStrength) {
        ConnectionStrength oldStrength = currentStrength;
        currentStrength = newStrength;
        changeSupport.firePropertyChange("strength", oldStrength, newStrength);
    }
    
    private ConnectionStrength calculateStrength() {
        int stable = connectionManager.countConnectionsWithNMessages(STABLE_THRESHOLD);
            
        ConnectionStrength strength;

        if(stable == 0) {
            int initializing = connectionManager.getNumFetchingConnections();
            int connections = connectionManager.getNumInitializedConnections();
            // No initializing or stable connections
            if(initializing == 0 && connections == 0) {
                //Not attempting to connect at all...
                if(!connectionManager.isConnecting()) {
                    strength = ConnectionStrength.DISCONNECTED;
                } else {
                    //Attempting to connect...
                    strength = ConnectionStrength.CONNECTING;
                }
            } else if(connections == 0) {
                // No initialized, all initializing - connecting
                strength = ConnectionStrength.CONNECTING;
            } else {
                // Some initialized - poor connection.
                strength = ConnectionStrength.WEAK;
            }
        } else if(connectionManager.isConnectionIdle()) {
            lastIdleTime = System.currentTimeMillis();
            strength = ConnectionStrength.FULL;
        } else {
            int preferred = connectionManager.getPreferredConnectionCount();
            // account for pro having more connections.
            if(LimeWireUtils.isPro()) {
                preferred -= 2;
            }
            
            // ultrapeers don't need as many...
            if(connectionManager.isSupernode()) {
                preferred -= 5;
            }
            
            preferred = Math.max(1, preferred); // prevent div by 0

            double percent = (double)stable / (double)preferred;
            if(percent <= 0.25) {
                strength = ConnectionStrength.WEAK;
            } else if(percent <= 0.5) {
                strength = ConnectionStrength.MEDIUM;
            } else if(percent <= 0.75) {
                strength = ConnectionStrength.MEDIUM;
            } else if(percent <= 1) {
                strength = ConnectionStrength.FULL;
            } else /* if(percent > 1) */ {
                strength = ConnectionStrength.TURBO;
            }
        }
        
        switch(strength) {
        case DISCONNECTED:
        case CONNECTING:
            if(lastStrengthRelatedEvent == ConnectionLifecycleEventType.NO_INTERNET) {
                strength = ConnectionStrength.NO_INTERNET;
            }    
        }
        
        switch(strength) {
        case CONNECTING:
        case WEAK:
            // if one of these four, see if we recently woke up from
            // idle, and if so, report as 'waking up' instead.
            long now = System.currentTimeMillis();
            if(now < lastIdleTime + 15 * 1000)
                strength = ConnectionStrength.MEDIUM;
        }
                
        return strength;
    }

    public boolean isUltrapeer() {
        return connectionManager.isSupernode();
    }

    @Override
    public void restart() {
        connectionManager.disconnect(true);
        connectionManager.connect();
    }
    
    @Override
    public ConnectionStrength getConnectionStrength() {
        return currentStrength;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
}
