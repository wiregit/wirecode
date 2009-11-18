package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

import org.limewire.core.settings.UploadSettings;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.friends.chat.ChatMediator;
import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.mainframe.ChangeLanguageAction;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

class ViewMenu extends MnemonicMenu {
    
    private static final String visibleText = I18n.tr("Hide &Chat Window");
    private static final String notVisibleText = I18n.tr("Show &Chat Window");
    
    private final Provider<LoginPopupPanel> friendsSignInPanelProvider;
    private final Provider<AutoLoginService> autoLoginServiceProvider;
    private final EventBean<FriendConnectionEvent> friendConnectionEventBean;
    
    private final Provider<HideTransferTrayAction> hideTransferTrayTrayActionProvider;
    private final Provider<ShowDownloadOnlyTrayAction> showHideDownloadTrayActionProvider;
    private final Provider<ShowDownloadAndUploadTrayAction> uploadTrayActionProvider;
    private final Provider<ChatMediator> chatFrameProvider;
    private final Provider<ChangeLanguageAction> changeLanguageActionProvider;
    
    @Inject
    public ViewMenu(Provider<LoginPopupPanel> friendsSignInPanel,
            Provider<AutoLoginService> autoLoginServiceProvider,
            EventBean<FriendConnectionEvent> friendConnectionEventBean,
            Provider<HideTransferTrayAction> hideTransferTrayTrayActionProvider,
            Provider<ShowDownloadOnlyTrayAction> showHideDownloadTrayAction,
            Provider<ShowDownloadAndUploadTrayAction> uploadTrayActionProvider,
            Provider<ChatMediator> chatFrameProvider,
            Provider<ChangeLanguageAction> changeLanguageActionProvider) {
        
        super(I18n.tr("&View"));
        
        this.friendsSignInPanelProvider = friendsSignInPanel;
        this.hideTransferTrayTrayActionProvider = hideTransferTrayTrayActionProvider;
        this.autoLoginServiceProvider = autoLoginServiceProvider;
        this.friendConnectionEventBean = friendConnectionEventBean;
        
        this.showHideDownloadTrayActionProvider = showHideDownloadTrayAction;
        this.uploadTrayActionProvider = uploadTrayActionProvider;
        this.chatFrameProvider = chatFrameProvider;
        this.changeLanguageActionProvider = changeLanguageActionProvider;
    }
    
    @Override
    public void createMenuItems() {
        JRadioButtonMenuItem hideTransferTray =  new JRadioButtonMenuItem(hideTransferTrayTrayActionProvider.get());
        JRadioButtonMenuItem showDownloads =  new JRadioButtonMenuItem(showHideDownloadTrayActionProvider.get());
        JRadioButtonMenuItem showDownloadsAndUploads =  new JRadioButtonMenuItem(uploadTrayActionProvider.get());
        
        boolean showTransfers = SwingUiSettings.SHOW_TRANSFERS_TRAY.getValue();
        boolean showUploads = UploadSettings.SHOW_UPLOADS_IN_TRAY.getValue();
        
        hideTransferTray.setSelected(!showTransfers);
        showDownloads.setSelected(showTransfers && !showUploads);
        showDownloadsAndUploads.setSelected(showTransfers && showUploads);
        
        ButtonGroup group = new ButtonGroup();
        group.add(hideTransferTray);
        group.add(showDownloads);
        group.add(showDownloadsAndUploads);
        
        add(buildShowHideChatWindowAction(chatFrameProvider));
        addSeparator();
        add(hideTransferTray);
        add(showDownloads);
        add(showDownloadsAndUploads);
        addSeparator();
        add(changeLanguageActionProvider.get());
    }

    /**
     * @return if there is a connection that is either logged in, logging in or
     * a login service provider is attempting to log in.
     */
    private boolean hasActiveConnection() {
        if (autoLoginServiceProvider.get().isAttemptingLogin()) {
            return true;
        }
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null) {
            return friendConnection.isLoggedIn() || friendConnection.isLoggingIn();
        }
        return false;
    }
    
    private boolean isLoggingIn() {
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        return friendConnection != null && friendConnection.isLoggingIn();
    }

    private Action buildShowHideChatWindowAction(final Provider<ChatMediator> chatFrameProvider) {
        
        Action action = new AbstractAction(chatFrameProvider.get().isVisible() ? visibleText : notVisibleText) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!chatFrameProvider.get().isVisible() && !hasActiveConnection()) {
                        friendsSignInPanelProvider.get().setVisible(true);
                } else {
                    // TODO: nothing happens if we are logging in, seems strange.
                    if (!autoLoginServiceProvider.get().isAttemptingLogin() && !isLoggingIn()) {
                        chatFrameProvider.get().setVisible(!chatFrameProvider.get().isVisible());
                    }
                }
                
            }
        };
        
        return action;
    }
}
