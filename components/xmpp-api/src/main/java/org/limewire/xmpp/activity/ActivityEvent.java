package org.limewire.xmpp.activity;

import org.limewire.listener.AbstractSourcedEvent;

public class ActivityEvent extends AbstractSourcedEvent<ActivityEvent.ActivityState>{
    public static enum ActivityState {
        Active, Idle
    }

    public ActivityEvent(ActivityState source) {
        super(source);
    }
}
