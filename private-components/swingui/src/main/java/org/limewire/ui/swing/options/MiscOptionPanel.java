package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.UISettings;
import org.limewire.ui.swing.friends.XMPPEventHandler;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

import com.google.inject.Inject;

/**
 * Misc Option View
 */
public class MiscOptionPanel extends OptionPanel {

    private final XMPPEventHandler xmppEventHandler;
    
    private NotificationsPanel notificationsPanel;
    private FriendsChatPanel friendsChatPanel;
    
    @Inject
    public MiscOptionPanel(XMPPEventHandler xmppEventHandler) {
        this.xmppEventHandler = xmppEventHandler;
        
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
            add(new JLabel(I18n.tr("Show popup system notifications")), "wrap");
            
            add(playNotificationsCheckBox);
            add(new JLabel(I18n.tr("Play notification sounds")), "wrap");
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
        private JComboBox serviceComboBox;
        private JTextField usernameTextField;
        private JPasswordField passwordField;
        
        public FriendsChatPanel() {
            super(I18n.tr("Friends and Chat"));
            
            signIntoOnStartupCheckBox = new JCheckBox();            
            signIntoOnStartupCheckBox.setContentAreaFilled(false);
            signIntoOnStartupCheckBox.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    serviceComboBox.setEnabled(signIntoOnStartupCheckBox.isSelected());
                    usernameTextField.setEnabled(signIntoOnStartupCheckBox.isSelected());
                    passwordField.setEnabled(signIntoOnStartupCheckBox.isSelected());
                }
            });
            
            serviceComboBox = new JComboBox();
            ArrayList<String> services = new ArrayList<String>(); 
            for(XMPPConnection connection : xmppEventHandler.getAllConnections()) {
                XMPPConnectionConfiguration config = connection.getConfiguration();
                services.add(config.getFriendlyName());
            }
            Collections.sort(services);
            for(String friendlyName : services)
                serviceComboBox.addItem(friendlyName); // FIXME: icons?
            serviceComboBox.setSelectedItem("Gmail");
            serviceComboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    String friendlyName = (String)serviceComboBox.getSelectedItem();
                    XMPPConnectionConfiguration config =
                        xmppEventHandler.getConfigByFriendlyName(friendlyName);
                    usernameTextField.setText(config.getMyID());
                    passwordField.setText(config.getPassword());
                }
            });
            
            usernameTextField = new JTextField(30);
            passwordField = new JPasswordField(30);            
            
            add(signIntoOnStartupCheckBox, "split");
            add(new JLabel(I18n.tr("Sign in when LimeWire starts")), "wrap");
            
            add(new JLabel(I18n.tr("Sign in using")), "gapleft 25, split");
            add(serviceComboBox, "wrap");
            
            add(new JLabel(I18n.tr("Username")), "gapleft 25, split");
            add(usernameTextField, "wrap");
            
            add(new JLabel(I18n.tr("Password")), "gapleft 25, split");
            add(passwordField);
        }
        
        @Override
        void applyOptions() {
            String friendlyName = (String)serviceComboBox.getSelectedItem();
            XMPPConnectionConfiguration config =
                xmppEventHandler.getConfigByFriendlyName(friendlyName);
            config.setUsername(usernameTextField.getText());
            config.setPassword(new String(passwordField.getPassword()));
            config.setAutoLogin(signIntoOnStartupCheckBox.isSelected());
            //TODO: some checking here for invalid states, null string, etc..
            //      also check if autologin checked but username/password null
        }

        @Override
        boolean hasChanged() {
            String friendlyName = (String)serviceComboBox.getSelectedItem();
            XMPPConnectionConfiguration config =
                xmppEventHandler.getConfigByFriendlyName(friendlyName);
            return config.isAutoLogin() != signIntoOnStartupCheckBox.isSelected()
                    || !usernameTextField.getText().equals(config.getUsername())
                    || !new String(passwordField.getPassword()).equals(config.getPassword());
        }

        @Override
        public void initOptions() { 
            String friendlyName = (String)serviceComboBox.getSelectedItem();
            XMPPConnectionConfiguration config =
                xmppEventHandler.getConfigByFriendlyName(friendlyName);
            signIntoOnStartupCheckBox.setSelected(config.isAutoLogin());
            serviceComboBox.setEnabled(config.isAutoLogin());
            usernameTextField.setEnabled(config.isAutoLogin());
            passwordField.setEnabled(config.isAutoLogin());
            usernameTextField.setText(config.getUsername());
            passwordField.setText(config.getPassword());
        }
    }
}
