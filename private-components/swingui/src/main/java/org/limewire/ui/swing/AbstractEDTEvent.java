package org.limewire.ui.swing;

import org.bushe.swing.event.EventServiceLocator;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * 
 */
public abstract class AbstractEDTEvent {

    public void publish() {
        EventServiceLocator.getSwingEventService().publish(this);
    }
}
