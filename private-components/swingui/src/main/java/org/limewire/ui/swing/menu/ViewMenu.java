package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.VisibilityType;
import org.limewire.ui.swing.util.VisibleComponent;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ViewMenu extends MnemonicMenu {
    private final Provider<LoginPopupPanel> friendsSignInPanel;
    private final Provider<AutoLoginService> autoLoginServiceProvider;
    private final XMPPService xmppService;
    
    @Inject
    public ViewMenu(final ChatFrame chatFrame, Provider<LoginPopupPanel> friendsSignInPanel, XMPPService xmppService, 
            Provider<AutoLoginService> autoLoginServiceProvider) {
        super(I18n.tr("&View"));
        this.friendsSignInPanel = friendsSignInPanel;
        this.xmppService = xmppService;
        this.autoLoginServiceProvider = autoLoginServiceProvider;
        add(buildShowHideAction(chatFrame, I18n.tr("Hide &Chat Window"), I18n.tr("Show &Chat Window")));
        add(buildAlwaysShowDownloadTray(I18n.tr("Always Show Download Tray")));
    }

    private JCheckBoxMenuItem buildAlwaysShowDownloadTray(String name) {
        final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem();
        
        Action action = new AbstractAction(name) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.setValue(menuItem.isSelected());
            }
        };
        
        menuItem.setAction(action);
        menuItem.setText(name);
        
        menuItem.setSelected(DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue());
        return menuItem;
    }

    private Action buildShowHideAction(final VisibleComponent component, final String visibleName,
            final String notVisibleName) {
        final Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!component.isVisible() && !xmppService.isLoggedIn() && !xmppService.isLoggingIn()
                        && !autoLoginServiceProvider.get().isAttemptingLogin()) {
                        friendsSignInPanel.get().setVisible(true);
                } else {
                    // TODO: nothing happens if we are logging in, seems strange.
                    if (!autoLoginServiceProvider.get().isAttemptingLogin() && !xmppService.isLoggingIn()) {
                        component.toggleVisibility();
                    }
                }
                
            }
        };

        addVisibilityListener(component, action, visibleName, notVisibleName);
        setInitialText(component, action, visibleName, notVisibleName);

        return action;
    }

    private void addVisibilityListener(VisibleComponent component, final Action action,
            final String visibleName, final String notVisibleName) {
        EventListener<VisibilityType> listener = new EventListener<VisibilityType>() {
            @Override
            public void handleEvent(VisibilityType visibilityType) {
                if (visibilityType == VisibilityType.VISIBLE) {
                    action.putValue(Action.NAME, visibleName);
                } else {
                    action.putValue(Action.NAME, notVisibleName);
                }
            }
        };
        component.addVisibilityListener(listener);
    }

    private void setInitialText(VisibleComponent component, final Action action,
            final String visibleName, final String notVisibleName) {
        if (component.isVisible()) {
            action.putValue(Action.NAME, visibleName);
        } else {
            action.putValue(Action.NAME, notVisibleName);
        }
    }
}
