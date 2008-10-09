package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.event.AbstractEDTEvent;

public class DisplayFriendsToggleEvent extends AbstractEDTEvent {

    private Boolean visible;

    public DisplayFriendsToggleEvent() {

    }

    public DisplayFriendsToggleEvent(Boolean visible) {
        this.visible = visible;
    }

    /**
     * Returns whether or not the friends display should be visible. If null is
     * returned assume it should toggle from the current state.
     */
    public Boolean getVisible() {
        return visible;
    }

}
