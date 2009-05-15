package org.limewire.ui.swing.action;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.client.FriendConnectionEvent;
import org.limewire.core.settings.XMPPSettings;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.friends.chat.IconLibrary;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

/**
 * Provides JMenuItems to be used for setting available or disabled status for
 * the users. These items are backed by a button group and JCheckBoxMenuItems
 */
public class StatusActions {
    private final Action availableAction;
    private final Action doNotDisturbAction;
    
    private final Set<JCheckBoxMenuItem> availableItems = new HashSet<JCheckBoxMenuItem>();
    private final Set<JCheckBoxMenuItem> doNotDisturbItems = new HashSet<JCheckBoxMenuItem>();
    
    private final EventBean<FriendConnectionEvent> connectionEventBean;

    @Inject
    public StatusActions(final EventBean<FriendConnectionEvent> connectionEventBean, final IconLibrary iconLibrary) {
        this.connectionEventBean = connectionEventBean;
        
        availableAction = new AbstractAction(I18n.tr("&Available")) {
            { 
                putValue(Action.SMALL_ICON, iconLibrary.getAvailable());
                setEnabled(false);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                FriendConnectionEvent connection = connectionEventBean.getLastEvent();
                if(connection != null && connection.getType() == FriendConnectionEvent.Type.CONNECTED) {
                    connection.getSource().setMode(FriendPresence.Mode.available).addFutureListener(new EventListener<FutureEvent<Void>>() {
                        @Override
                        public void handleEvent(FutureEvent<Void> event) {
                            if(event.getType() == FutureEvent.Type.SUCCESS) {
                                XMPPSettings.XMPP_DO_NOT_DISTURB.setValue(false);
                            }
                        }
                    });
                }
            }
        };

        
        doNotDisturbAction = new AbstractAction(I18n.tr("&Do Not Disturb")) {
            {
                putValue(Action.SMALL_ICON, iconLibrary.getDoNotDisturb());
                setEnabled(false);
            }
            @Override
            public void actionPerformed(ActionEvent e) {
                FriendConnectionEvent connection = connectionEventBean.getLastEvent();
                if(connection != null && connection.getType() == FriendConnectionEvent.Type.CONNECTED) {
                    connection.getSource().setMode(FriendPresence.Mode.dnd).addFutureListener(new EventListener<FutureEvent<Void>>() {
                        @Override
                        public void handleEvent(FutureEvent<Void> event) {
                            if(event.getType() == FutureEvent.Type.SUCCESS) {
                                XMPPSettings.XMPP_DO_NOT_DISTURB.setValue(true);
                            }
                        }
                    });
                }
            }
        };
        
        updateSelections();
        
        XMPPSettings.XMPP_DO_NOT_DISTURB.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateSelections();
                    }
                });
            }
        });
    }

    private void updateSelections() {
        FriendConnectionEvent connection = connectionEventBean.getLastEvent();
        if(connection != null && connection.getType() == FriendConnectionEvent.Type.CONNECTED) {
            if(connection.getSource().isLoggedIn()) {
                boolean dndBool = XMPPSettings.XMPP_DO_NOT_DISTURB.getValue();
                for ( JCheckBoxMenuItem item : availableItems ) {
                    item.setSelected(!dndBool);
                }

                for ( JCheckBoxMenuItem item : doNotDisturbItems ) {
                    item.setSelected(dndBool);
                }
            } else {
                //do not show selections when logged out

                for ( JCheckBoxMenuItem item : availableItems ) {
                    item.setSelected(false);
                }

                for ( JCheckBoxMenuItem item : doNotDisturbItems ) {
                    item.setSelected(false);
                }
            }
        }
    }

    @Inject
    void register(FriendActions actions, ListenerSupport<FriendConnectionEvent> event) {
        event.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch (event.getType()) {
                case CONNECTED:
                case CONNECTING:
                    availableAction.setEnabled(true);
                    doNotDisturbAction.setEnabled(true);
                    updateSelections();
                    break;
                case CONNECT_FAILED:
                case DISCONNECTED:
                    availableAction.setEnabled(false);
                    doNotDisturbAction.setEnabled(false);
                    updateSelections();
                    break;
                }
            }
        });
    }

    public JMenuItem getAvailableMenuItem() {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(availableAction);
        availableItems.add(item);
        return item;
    }

    public JMenuItem getDnDMenuItem() {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(doNotDisturbAction);
        doNotDisturbItems.add(item);
        return item;
    }
}
