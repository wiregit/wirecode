package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.AbstractEDTEvent;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class DisplayFriendsEvent extends AbstractEDTEvent {
    private final boolean shouldShow;
    
    public DisplayFriendsEvent(boolean shouldShow) {
        this.shouldShow = shouldShow;
    }

    public boolean shouldShow() {
        return shouldShow;
    }
}
