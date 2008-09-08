package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.xmpp.api.client.Presence.Mode;

public class AwayOption extends AbstractAction {
    public AwayOption() {
        super(tr("Away (checkable)"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new SelfAvailabilityUpdateEvent(Mode.away).publish();
    }
}