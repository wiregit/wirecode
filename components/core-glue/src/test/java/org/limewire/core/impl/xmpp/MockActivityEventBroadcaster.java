package org.limewire.core.impl.xmpp;

import org.limewire.listener.EventBroadcaster;
import org.limewire.xmpp.activity.ActivityEvent;

public class MockActivityEventBroadcaster implements EventBroadcaster<ActivityEvent> {

    public ActivityEvent event;
    @Override
    public void broadcast(ActivityEvent event) {
        this.event = event;
    }
}
