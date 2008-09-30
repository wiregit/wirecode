package org.limewire.ui.swing.friends;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.xmpp.api.client.Presence.Mode;

public class AvailableOption extends AbstractAction {
    public AvailableOption() {
        super(tr(Mode.available.toString()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new SelfAvailabilityUpdateEvent(Mode.available).publish();
    }
}