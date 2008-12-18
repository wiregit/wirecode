package org.limewire.core.impl.xmpp;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.activity.ActivityEvent;
import org.limewire.xmpp.activity.ActivityEvent.ActivityState;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class IdleStatusMonitor {
    private static final int TWENTY_MINUTES_IN_MILLIS = 1200000;
    private final IdleTime idleTime;
    private final ScheduledExecutorService backgroundExecutor;
    private final ThreadSleeper sleeper;
    private final EventBroadcaster<ActivityEvent> activityBroadcaster;
    private boolean isXMPPConnected;

    @Inject
    public IdleStatusMonitor(@Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor, 
            IdleTime idleTime, ThreadSleeper sleeper, EventBroadcaster<ActivityEvent> activityBroadcaster) {
        this.backgroundExecutor = backgroundExecutor;
        this.idleTime = idleTime;
        this.sleeper = sleeper;
        this.activityBroadcaster = activityBroadcaster;
    }

    @Inject void register(ServiceRegistry registry) {
        registry.register(new Service() {

            @Override
            public String getServiceName() {
                return "XMPP Idle Status timer";
            }

            @Override
            public void initialize() {}

            @Override
            public void start() {
                backgroundExecutor.schedule(new Runnable() {
                    private boolean hasBecomeInactive;
                    @Override
                    public void run() {
                        while(idleTime.supportsIdleTime() && isXMPPConnected) {
                            if (idleTime.getIdleTime() > TWENTY_MINUTES_IN_MILLIS) {
                                activityBroadcaster.broadcast(new ActivityEvent(ActivityState.Idle));
                                hasBecomeInactive = true;
                            } else if (hasBecomeInactive) {
                                activityBroadcaster.broadcast(new ActivityEvent(ActivityState.Active));
                                hasBecomeInactive = false;
                            }
                            try {
                                sleeper.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }, 0, TimeUnit.MILLISECONDS);
            }

            @Override
            public void stop() {}
        });
    }
    
    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTED:
                    isXMPPConnected = true;
                    break;
                case DISCONNECTED:
                    isXMPPConnected = false;
                    break;
                }
            }
        });
    }
}
