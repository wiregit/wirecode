package org.limewire.core.impl.xmpp;

import java.util.ArrayList;

import org.limewire.listener.EventBroadcaster;
import org.limewire.xmpp.activity.ActivityEvent;

public class MockActivityEventBroadcaster implements EventBroadcaster<ActivityEvent> {

    public ArrayList<ActivityEvent> events = new ArrayList<ActivityEvent>();
    @Override
    public void broadcast(ActivityEvent event) {
        this.events.add(event);
    }
}
