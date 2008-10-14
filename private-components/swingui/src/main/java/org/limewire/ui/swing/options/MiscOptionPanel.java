package org.limewire.ui.swing.options;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.UISettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Misc Option View
 */
@Singleton
public class MiscOptionPanel extends OptionPanel {

    private NotificationsPanel notificationsPanel;
    private FriendsChatPanel friendsChatPanel;
    
    @Inject
    public MiscOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getNotificationsPanel(), "pushx, growx");
        add(getFriendChatPanel(), "pushx, growx");
    }
    
    private OptionPanel getNotificationsPanel() {
        if(notificationsPanel == null) {
            notificationsPanel = new NotificationsPanel();
        }
        return notificationsPanel;
    }
    
    private OptionPanel getFriendChatPanel() {
        if(friendsChatPanel == null) {
            friendsChatPanel = new FriendsChatPanel();
        }
        return friendsChatPanel;
    }

    @Override
    void applyOptions() {
        getNotificationsPanel().applyOptions();
        getFriendChatPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getNotificationsPanel().hasChanged() || getFriendChatPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getNotificationsPanel().initOptions();
        getFriendChatPanel().initOptions();
    }
    
    private class NotificationsPanel extends OptionPanel {

        private JCheckBox showNotificationsCheckBox;
        private JCheckBox playNotificationsCheckBox;
        
        public NotificationsPanel() {
            super(I18n.tr("Notifications"));
            
            showNotificationsCheckBox = new JCheckBox();
            showNotificationsCheckBox.setContentAreaFilled(false);
            playNotificationsCheckBox = new JCheckBox();
            playNotificationsCheckBox.setContentAreaFilled(false);
            
            add(showNotificationsCheckBox);
            add(new JLabel("Show popup system notifications"), "wrap");
            
            add(playNotificationsCheckBox);
            add(new JLabel("Play notification sounds"), "wrap");
        }
        
        @Override
        void applyOptions() {
            UISettings.SHOW_NOTIFICATIONS.setValue(showNotificationsCheckBox.isSelected());
            UISettings.PLAY_NOTIFICATION_SOUND.setValue(playNotificationsCheckBox.isSelected());
        }

        @Override
        boolean hasChanged() {
            return showNotificationsCheckBox.isSelected() != UISettings.SHOW_NOTIFICATIONS.getValue() ||
                    playNotificationsCheckBox.isSelected() != UISettings.PLAY_NOTIFICATION_SOUND.getValue(); 
        }

        @Override
        public void initOptions() {
            playNotificationsCheckBox.setSelected(UISettings.PLAY_NOTIFICATION_SOUND.getValue());
            showNotificationsCheckBox.setSelected(UISettings.SHOW_NOTIFICATIONS.getValue());
        }
    }
    
    private class FriendsChatPanel extends OptionPanel {

        private JCheckBox signIntoOnStartupCheckBox;
        private JCheckBox rememberPasswordCheckBox;
        private JTextField usernameTextField;
        private JPasswordField passwordField;
        
        public FriendsChatPanel() {
            super(I18n.tr("Friends and Chat"));
            
            signIntoOnStartupCheckBox = new JCheckBox();
            signIntoOnStartupCheckBox.setContentAreaFilled(false);
            rememberPasswordCheckBox = new JCheckBox();
            rememberPasswordCheckBox.setContentAreaFilled(false);
            usernameTextField = new JTextField(30);
            passwordField = new JPasswordField(30);
            
            add(signIntoOnStartupCheckBox, "split");
            add(new JLabel("Sign into Friends when LimeWire starts"), "wrap");

            add(rememberPasswordCheckBox, "split");
            add(new JLabel("Remember username and password"), "wrap");
            
            add(new JLabel("Username"), "gapleft 25, split");
            add(usernameTextField, "wrap");
            
            add(new JLabel("Password"), "gapleft 25, split");
            add(passwordField);
        }
        
        @Override
        void applyOptions() {

        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        public void initOptions() {

        }
    }
}
