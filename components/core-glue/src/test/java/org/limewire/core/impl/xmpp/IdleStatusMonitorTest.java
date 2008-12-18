package org.limewire.core.impl.xmpp;


import junit.framework.TestCase;

import org.limewire.lifecycle.Service;
import org.limewire.xmpp.activity.ActivityEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

public class IdleStatusMonitorTest extends TestCase {
    private MockIdleTime idleTime;
    private MockThreadSleeper sleeper;
    private Runnable monitorRunnable;
    private MockActivityEventBroadcaster activityBroadcaster;
    private MockListenerSupport connectionSupport;

    @Override
    protected void setUp() throws Exception {
        MockScheduledExecutorService backgroundExecutor = new MockScheduledExecutorService();
        idleTime = new MockIdleTime();
        sleeper = new MockThreadSleeper();
        activityBroadcaster = new MockActivityEventBroadcaster();
        connectionSupport = new MockListenerSupport();
        IdleStatusMonitor monitor = new IdleStatusMonitor(backgroundExecutor, idleTime, sleeper, activityBroadcaster);
        MockServiceRegistry registry = new MockServiceRegistry();
        
        //trigger the monitoring code
        monitor.register(registry);
        monitor.register(connectionSupport);
        Service service = registry.registeredService;
        service.start();
        monitorRunnable = backgroundExecutor.scheduleRunnable;
    }

    public void testIdleTimeNotSupported() {
        idleTime.supportsIdleTimeReturn = false;
        monitorRunnable.run();
        assertTrue("No event ever fired if system does not support idle time", activityBroadcaster.events.isEmpty());
    }

    public void testXMPPNotConnected() {
        idleTime.supportsIdleTimeReturn = true;
        monitorRunnable.run();
        assertTrue("No event ever fired if system does not support idle time", activityBroadcaster.events.isEmpty());
        sendConnectionEvent(XMPPConnectionEvent.Type.DISCONNECTED);
        assertTrue("No event ever fired if system does not support idle time", activityBroadcaster.events.isEmpty());
    }

    private void sendConnectionEvent(org.limewire.xmpp.api.client.XMPPConnectionEvent.Type connectionState) {
        connectionSupport.listener.handleEvent(new XMPPConnectionEvent(new MockXMPPConnection(null), connectionState));
    }
    
    public void testIdleStateIfInactiveAfter20Min() {
        idleTime.supportsIdleTimeReturn = true;
        sendConnectionEvent(XMPPConnectionEvent.Type.CONNECTED);
        idleTime.getIdleTimeReturn.add(0l);
        idleTime.getIdleTimeReturn.add(Long.MAX_VALUE);
        idleTime.getIdleTimeReturn.add(0l);
        idleTime.getIdleTimeReturn.add(0l);
        monitorRunnable.run();
        assertEquals(2, activityBroadcaster.events.size());
        assertEquals(ActivityEvent.ActivityState.Idle, activityBroadcaster.events.get(0).getSource());
        assertEquals(ActivityEvent.ActivityState.Active, activityBroadcaster.events.get(1).getSource());
    }
}
