package org.limewire.ui.swing.event;

import java.awt.Component;

import org.limewire.ui.swing.AbstractEDTEvent;


public class PanelDisplayedEvent extends AbstractEDTEvent {
    
    private final Component displayedPanel;
    
    /**
     * 
     * @param panel The Component displayed
     */
    public PanelDisplayedEvent(Component displayedPanel) {
        this.displayedPanel = displayedPanel;
    }

    /**
     * 
     * @return the component displayed
     */
    public Component getDisplayedPanel() {
        return displayedPanel;
    }
  
}
