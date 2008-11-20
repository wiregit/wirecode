package org.limewire.ui.swing.friends;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.xmpp.api.client.Presence.Mode;

public class DndOption extends AbstractAction {
    public DndOption() {
        super(tr(Mode.dnd.toString()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new SelfAvailabilityUpdateEvent(Mode.dnd).publish();
    }
}