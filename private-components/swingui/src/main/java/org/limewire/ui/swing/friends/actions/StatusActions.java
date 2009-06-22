package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import org.jdesktop.application.Resource;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.settings.XMPPSettings;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.api.client.XMPPPresence.Mode;

import com.google.inject.Inject;

/**
 * Provides JMenuItems to be used for setting available or disabled status for
 * the users. These items are backed by a button group and JCheckBoxMenuItems
 */
@LazySingleton
public class StatusActions {
    @Resource
    private Icon available;

    @Resource
    private Icon doNotDisturb;

    private final Action availableAction;

    private final Action doNotDisturbAction;

    private final JCheckBoxMenuItem availableItem;

    private final JCheckBoxMenuItem doNotDisturbItem;

    private final XMPPService xmppService;

    @Inject
    public StatusActions(final XMPPService xmppService) {
        this.xmppService = xmppService;

        GuiUtils.assignResources(this);

        availableAction = new AbstractAction(I18n.tr("&Available")) {
            {
                putValue(Action.SMALL_ICON, available);
                setEnabled(false);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                xmppService.setMode(Mode.available).addFutureListener(
                        new EventListener<FutureEvent<Void>>() {
                            @Override
                            public void handleEvent(FutureEvent<Void> event) {
                                if (event.getType() == FutureEvent.Type.SUCCESS) {
                                    XMPPSettings.XMPP_DO_NOT_DISTURB.setValue(false);
                                }
                            }
                        });
            }
        };

        doNotDisturbAction = new AbstractAction(I18n.tr("&Do Not Disturb")) {
            {
                putValue(Action.SMALL_ICON, doNotDisturb);
                setEnabled(false);
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                xmppService.setMode(Mode.dnd).addFutureListener(
                        new EventListener<FutureEvent<Void>>() {
                            @Override
                            public void handleEvent(FutureEvent<Void> event) {
                                if (event.getType() == FutureEvent.Type.SUCCESS) {
                                    XMPPSettings.XMPP_DO_NOT_DISTURB.setValue(true);
                                }
                            }
                        });
            }
        };

        this.availableItem = new JCheckBoxMenuItem(availableAction);
        ;
        this.doNotDisturbItem = new JCheckBoxMenuItem(doNotDisturbAction);

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
        if (xmppService.isLoggedIn()) {
            boolean dndBool = XMPPSettings.XMPP_DO_NOT_DISTURB.getValue();
            availableItem.setSelected(!dndBool);
            doNotDisturbItem.setSelected(dndBool);
        } else {
            // do not show selections when logged out
            availableItem.setSelected(false);
            doNotDisturbItem.setSelected(false);
        }
    }

    @Inject
    void register(ListenerSupport<XMPPConnectionEvent> event) {
        event.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
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
        return availableItem;
    }

    public JMenuItem getDnDMenuItem() {
        return doNotDisturbItem;
    }
}
