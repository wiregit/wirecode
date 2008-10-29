package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.ui.swing.components.NumericTextField;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Proxy Option View
 */
@Singleton
public class ProxyOptionPanel extends OptionPanel {
    
    private JRadioButton noProxyRadioButton;
    private JRadioButton socksV4RadionButton;
    private JRadioButton socksV5RadioButton;
    private JRadioButton httpRadioButton;
    
    private ButtonGroup buttonGroup;
    
    private JTextField proxyTextField;
    private JTextField portTextField;
    
    private JCheckBox authenticationCheckBox;
    private JTextField userNameTextField;
    private JPasswordField passwordField;
    
    private JLabel proxyLabel;
    private JLabel portLabel;
    private JLabel enableLabel;
    private JLabel userNameLabel;
    private JLabel passwordLabel;
    
    @Inject
    public ProxyOptionPanel() {
        super();
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getProxyPanel(), "pushx, growx");
    }
    
    private JPanel getProxyPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));
        
        noProxyRadioButton = new JRadioButton();
        noProxyRadioButton.addItemListener(new ProxyButtonListener());
        socksV4RadionButton = new JRadioButton();
        socksV4RadionButton.addItemListener(new ProxyButtonListener());
        socksV5RadioButton = new JRadioButton();
        socksV5RadioButton.addItemListener(new ProxyButtonListener());
        httpRadioButton = new JRadioButton();
        httpRadioButton.addItemListener(new ProxyButtonListener());
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(noProxyRadioButton);
        buttonGroup.add(socksV4RadionButton);
        buttonGroup.add(socksV5RadioButton);
        buttonGroup.add(httpRadioButton);
        
        proxyTextField = new JTextField(15);
        portTextField = new NumericTextField(5);
        authenticationCheckBox = new JCheckBox();
        authenticationCheckBox.addItemListener(new ProxyButtonListener());
        userNameTextField = new JTextField(15);
        passwordField = new JPasswordField(15);
        
        proxyLabel = new JLabel(I18n.tr("Proxy:"));
        portLabel = new JLabel(I18n.tr("Port:"));
        enableLabel = new JLabel(I18n.tr("Enable Authentication"));
        userNameLabel = new JLabel(I18n.tr("Username:"));
        passwordLabel = new JLabel(I18n.tr("Password:"));
        
        p.add(noProxyRadioButton, "split");
        p.add(new JLabel(I18n.tr("No Proxy")), "wrap");
        
        p.add(socksV4RadionButton, "split");
        p.add(new JLabel(I18n.tr("Socks v4")), "wrap");
        
        p.add(socksV5RadioButton, "split");
        p.add(new JLabel(I18n.tr("Socks v5")), "wrap");
        
        p.add(httpRadioButton, "split");
        p.add(new JLabel(I18n.tr("HTTP")), "wrap");
        
        p.add(proxyLabel, "split");
        p.add(proxyTextField, "gap unrelated");
        p.add(portLabel);
        p.add(portTextField, "wrap");
        
        p.add(authenticationCheckBox, "gapleft 25, split");
        p.add(enableLabel, "wrap");
        
        p.add(userNameLabel, "gapleft 25, split");
        p.add(userNameTextField, "wrap");
        p.add(passwordLabel, "gapleft 25, split");
        p.add(passwordField);
        
        return p;
    }
    
    @Override
    void applyOptions() {
        int connectionMethod = ConnectionSettings.C_NO_PROXY;

        if (socksV4RadionButton.isSelected())
            connectionMethod = ConnectionSettings.C_SOCKS4_PROXY;
        else if (socksV5RadioButton.isSelected())
            connectionMethod = ConnectionSettings.C_SOCKS5_PROXY;
        else if (httpRadioButton.isSelected())
            connectionMethod = ConnectionSettings.C_HTTP_PROXY;

        final int proxyPort = Integer.parseInt(portTextField.getText());
        final String proxy = proxyTextField.getText();

        ConnectionSettings.PROXY_PORT.setValue(proxyPort);
        ConnectionSettings.CONNECTION_METHOD.setValue(connectionMethod);
        ConnectionSettings.PROXY_HOST.setValue(proxy);
    }

    @Override
    boolean hasChanged() {
        if(ConnectionSettings.PROXY_PORT.getValue() != Integer.parseInt(portTextField.getText()))
            return true;
        if(!ConnectionSettings.PROXY_HOST.getValue().equals(proxyTextField.getText()))
            return true;
        switch(ConnectionSettings.CONNECTION_METHOD.getValue()) {
            case ConnectionSettings.C_SOCKS4_PROXY:
                return !socksV4RadionButton.isSelected();
            case ConnectionSettings.C_SOCKS5_PROXY:
                return !socksV5RadioButton.isSelected();
            case ConnectionSettings.C_HTTP_PROXY:
                return !httpRadioButton.isSelected();
            case ConnectionSettings.C_NO_PROXY:
                return !noProxyRadioButton.isSelected();
            default:
                return true;
        }
    }

    @Override
    public void initOptions() {
        int connectionMethod = ConnectionSettings.CONNECTION_METHOD.getValue();
        String proxy = ConnectionSettings.PROXY_HOST.getValue();
        int proxyPort = ConnectionSettings.PROXY_PORT.getValue();

        noProxyRadioButton.setSelected(connectionMethod == ConnectionSettings.C_NO_PROXY);
        socksV4RadionButton.setSelected(connectionMethod == ConnectionSettings.C_SOCKS4_PROXY);
        socksV5RadioButton.setSelected(connectionMethod == ConnectionSettings.C_SOCKS5_PROXY);
        httpRadioButton.setSelected(connectionMethod == ConnectionSettings.C_HTTP_PROXY);
        
        proxyTextField.setText(proxy);
        portTextField.setText(Integer.toString(proxyPort));
        
        updateState();
    }
    
    private void updateState() {
        if(noProxyRadioButton.isSelected()) {
            updateProxy(false);
            updateAuthentication(false);
        } else {
            updateProxy(true);
            if(httpRadioButton.isSelected()) {
                updateAuthentication(false);
            } else {
                updateAuthentication(true);
            }
        }
    }
    
    private void updateProxy(boolean value) {
        proxyTextField.setEnabled(value);
        portTextField.setEnabled(value);
        
        proxyLabel.setVisible(value);
        proxyTextField.setVisible(value);
        portLabel.setVisible(value);
        portTextField.setVisible(value);
    }
    
    private void updateAuthentication(boolean value) {
        authenticationCheckBox.setEnabled(value);
        authenticationCheckBox.setVisible(value);
        enableLabel.setVisible(value);
        
        if(authenticationCheckBox.isSelected() && authenticationCheckBox.isEnabled()) {
            userNameTextField.setEnabled(value);
            passwordField.setEnabled(value);
            
            userNameLabel.setVisible(value);
            userNameTextField.setVisible(value);
            passwordLabel.setVisible(value);
            passwordField.setVisible(value);
        } else {
            userNameTextField.setEnabled(false);
            passwordField.setEnabled(false);
            
            userNameLabel.setVisible(false);
            userNameTextField.setVisible(false);
            passwordLabel.setVisible(false);
            passwordField.setVisible(false);
        }
    }
    
    private class ProxyButtonListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            updateState();
        }
    }
}
