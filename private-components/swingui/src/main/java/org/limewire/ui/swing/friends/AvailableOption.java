package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.xmpp.api.client.Presence.Mode;

public class AvailableOption extends AbstractAction {
    public AvailableOption() {
        super(tr("Available (checkable)"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        new PresenceChangeEvent(Mode.available).publish();
    }
}