package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.UISettings;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import static org.limewire.ui.swing.util.I18n.tr;

import com.google.inject.Inject;

/**
 * Misc Option View
 */
public class MiscOptionPanel extends OptionPanel {

    private final XMPPAccountConfigurationManager accountManager;

    private NotificationsPanel notificationsPanel;
    private FriendsChatPanel friendsChatPanel;

    @Inject
    public MiscOptionPanel(XMPPAccountConfigurationManager accountManager) {
        this.accountManager = accountManager;

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
    boolean applyOptions() {
        boolean restart = getNotificationsPanel().applyOptions();
        restart |= getFriendChatPanel().applyOptions();
        return restart;
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
            super(tr("Notifications"));

            showNotificationsCheckBox = new JCheckBox(tr("Show popup system notifications"));
            showNotificationsCheckBox.setContentAreaFilled(false);
            playNotificationsCheckBox = new JCheckBox(tr("Play notification sounds"));
            playNotificationsCheckBox.setContentAreaFilled(false);

            add(showNotificationsCheckBox, "wrap");
            add(playNotificationsCheckBox, "wrap");
        }

        @Override
        boolean applyOptions() {
            UISettings.SHOW_NOTIFICATIONS.setValue(showNotificationsCheckBox.isSelected());
            UISettings.PLAY_NOTIFICATION_SOUND.setValue(playNotificationsCheckBox.isSelected());
            return false;
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

        private JCheckBox autoLoginCheckBox;
        private JComboBox serviceComboBox;
        private JLabel serviceLabel;
        private JTextField serviceField;
        private JTextField usernameField;
        private JPasswordField passwordField;

        public FriendsChatPanel() {
            super(tr("Friends and Chat"));

            autoLoginCheckBox = new JCheckBox(tr("Sign in when LimeWire starts"));            
            autoLoginCheckBox.setContentAreaFilled(false);
            autoLoginCheckBox.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    setComponentsEnabled(autoLoginCheckBox.isSelected());
                }
            });

            serviceComboBox = new JComboBox();
            for(String label : accountManager.getLabels())
                serviceComboBox.addItem(label);
            serviceComboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    populateInputs();
                }
            });
            serviceComboBox.setRenderer(new Renderer());
            serviceLabel = new JLabel(tr("Jabber Server:"));
            serviceField = new JTextField(18);
            usernameField = new JTextField(18);
            passwordField = new JPasswordField(18);

            add(autoLoginCheckBox, "split, wrap");

            add(new JLabel(tr("Using")), "gapleft 25, split");
            add(serviceComboBox, "wrap");

            add(serviceLabel, "gapleft 25, hidemode 3, split");
            add(serviceField, "hidemode 3, wrap");

            add(new JLabel(tr("Username:")), "gapleft 25, split");
            add(usernameField, "wrap");

            add(new JLabel(tr("Password:")), "gapleft 25, split");
            add(passwordField);

            // If there's an auto-login configuration, select it
            XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
            if(auto == null)
                serviceComboBox.setSelectedItem("Gmail");
            else
                serviceComboBox.setSelectedItem(auto.getLabel());
        }

        private void populateInputs() {
            String label = (String)serviceComboBox.getSelectedItem();
            if(label.equals(accountManager.getCustomConfigLabel())) {
                serviceLabel.setVisible(true);
                serviceField.setVisible(true);
            } else {
                serviceLabel.setVisible(false);
                serviceField.setVisible(false);
            }
            XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
            if(auto != null && label.equals(auto.getLabel())) {
                serviceField.setText(auto.getServiceName());
                usernameField.setText(auto.getUsername());
                passwordField.setText(auto.getPassword());
            } else {
                serviceField.setText("");
                usernameField.setText("");
                passwordField.setText("");
            }
        }

        private void setComponentsEnabled(boolean enabled) {
            serviceComboBox.setEnabled(enabled);
            serviceField.setEnabled(enabled);
            usernameField.setEnabled(enabled);
            passwordField.setEnabled(enabled);
        }

        @Override
        boolean applyOptions() {
            if(hasChanged()) {
                if(autoLoginCheckBox.isSelected()) {
                    String label = (String)serviceComboBox.getSelectedItem();
                    XMPPAccountConfiguration config = accountManager.getConfig(label);
                    config.setServiceName(serviceField.getText().trim());
                    config.setUsername(usernameField.getText().trim());
                    config.setPassword(new String(passwordField.getPassword()));
                    accountManager.setAutoLoginConfig(config);
                } else {
                    accountManager.setAutoLoginConfig(null);
                }
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
            if(auto == null) {
                return autoLoginCheckBox.isSelected();
            } else {
                if(!autoLoginCheckBox.isSelected())
                    return true;
                String label = (String)serviceComboBox.getSelectedItem();
                if(!label.equals(auto.getLabel()))
                    return true;
                String serviceName = serviceField.getText().trim();
                if(!serviceName.equals(auto.getServiceName()))
                    return true;
                String username = usernameField.getText().trim();
                if(!username.equals(auto.getUsername()))
                    return true;
                String password = new String(passwordField.getPassword());
                if(!password.equals(auto.getPassword()))
                    return true;
            }
            return false;
        }

        @Override
        public void initOptions() {
            populateInputs();
            XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
            setComponentsEnabled(auto != null);
            autoLoginCheckBox.setSelected(auto != null);
        }
    }

    private class Renderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            XMPPAccountConfiguration config = accountManager.getConfig(value.toString());
            if(config != null) {
                setIcon(config.getIcon());
            } else {
                setIcon(null);
            }
            return this;
        }
    }
}
