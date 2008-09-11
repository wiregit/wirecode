package org.limewire.ui.swing.search;

import org.limewire.ui.swing.AbstractEDTEvent;

/**
 * This class describes an event where
 * the search or filter criteria has changed.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class FilterEvent extends AbstractEDTEvent {

    private String text;

    public FilterEvent(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}