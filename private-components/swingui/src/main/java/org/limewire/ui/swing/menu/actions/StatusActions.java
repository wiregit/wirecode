package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StatusActions {
    private final JCheckBoxMenuItem available;

    private final JCheckBoxMenuItem dnd;

    public StatusActions() {
        available = new JCheckBoxMenuItem(new AbstractAction(I18n.tr("Available")) {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        dnd = new JCheckBoxMenuItem(new AbstractAction(I18n.tr("Do Not Disturb")) {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
        ButtonGroup group = new ButtonGroup();
        group.add(available);
        group.add(dnd);
    }

    @Inject
    void register(FriendActions actions, ListenerSupport<XMPPConnectionEvent> event) {
        event.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch (event.getType()) {
                case CONNECTED:
                case CONNECTING:
                    available.setEnabled(true);
                    dnd.setEnabled(true);
                    break;
                case CONNECT_FAILED:
                case DISCONNECTED:
                    available.setEnabled(false);
                    dnd.setEnabled(false);
                    break;
                }
            }
        });
    }

    public JMenuItem getAvailableAction() {
        return available;
    }

    public JMenuItem getDnDAction() {
        return dnd;
    }
}
