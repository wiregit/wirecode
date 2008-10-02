package org.limewire.ui.swing.friends;

import org.limewire.ui.swing.event.AbstractEDTEvent;

public class AddFriendEvent extends AbstractEDTEvent {
    private final String id;
    private final String name;
    
    public AddFriendEvent(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
