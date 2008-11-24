package org.limewire.ui.swing.friends.login;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc.
 */
@Singleton
class LoginPanel extends JXPanel {

    // private static final Log LOG = LogFactory.getLog(LoginPanel.class);
    
    private static final String SIGNIN_ENABLED_TEXT = tr("Sign in");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in ...");

    private static final String AUTHENTICATION_ERROR = tr("Invalid username/password."); // See spec
    private static final String NETWORK_ERROR = tr("Network error. Please try again later.");

    private JComboBox serviceComboBox;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox autoLoginCheckBox;
    private JLabel authFailedLabel;
    private JButton signInButton;
    private final XMPPAccountConfigurationManager accountManager;
    private final XMPPService xmppService;
    private final SignInAction signinAction = new SignInAction();

    @Inject
    LoginPanel(XMPPAccountConfigurationManager accountManager, XMPPService xmppService) {
        this.accountManager = accountManager;
        this.xmppService = xmppService;
        GuiUtils.assignResources(this);
        initComponents();
    }
    
    void autoLogin(XMPPAccountConfiguration auto) {
        serviceComboBox.setSelectedItem(auto.getLabel());
        login(auto);
    }    
    
    @Override
    public void setVisible(boolean flag) {
        boolean becameVisible = flag && !isVisible();
        super.setVisible(flag);
        if(becameVisible) {
            populateInputs();
            usernameField.requestFocusInWindow();
        }
    }

    private void initComponents() {
        serviceComboBox = new JComboBox();
        for(String label : accountManager.getLabels()) {
            serviceComboBox.addItem(label); // FIXME: icons?
        }
        serviceComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                populateInputs();
            }
        });
        serviceComboBox.setRenderer(new Renderer());
        usernameField = new JTextField(18);
        passwordField = new JPasswordField(18);

        passwordField.setAction(signinAction);

        autoLoginCheckBox = new JCheckBox(tr("Remember me"));
        autoLoginCheckBox.setOpaque(false);
        autoLoginCheckBox.setMargin(new Insets(0, 0, 0, 0));
        autoLoginCheckBox.setBorder(BorderFactory.createEmptyBorder());
        
        signInButton = new JButton(signinAction);
        signInButton.setOpaque(false);
        
        authFailedLabel = new JLabel();
        
        JLabel hideButton = new ActionLabel(new AbstractAction(("X")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoginPanel.this.setVisible(false);
            }
        }, true);

        setLayout(new MigLayout("gap 0, fill"));
        add(hideButton, "alignx right, wrap");
        add(new JLabel(tr("Using")), "alignx left, split");
        add(serviceComboBox, "wmin 0, wrap");
        add(new JLabel(tr("Username")), "alignx left, wrap");
        add(usernameField, "alignx left, wmin 0, wrap");
        add(new JLabel(tr("Password")), "alignx left, wrap");
        add(passwordField, "alignx left, wmin 0, wrap");
        add(autoLoginCheckBox, "gaptop 2, alignx left, wmin 0, wrap");
        add(authFailedLabel, "alignx left, wmin 0, hidemode 3, wrap");
        add(signInButton, "gaptop 2, alignx left, wmin 0");
        
        authFailedLabel.setVisible(false);
        authFailedLabel.setForeground(Color.RED);
        FontUtils.changeStyle(authFailedLabel, Font.ITALIC);
        
        setBackgroundPainter(new RectanglePainter<JXPanel>(2, 2, 2, 2, 5, 5, true, Color.LIGHT_GRAY, 0f, Color.LIGHT_GRAY));

        serviceComboBox.setSelectedItem("Gmail");
        setSignInComponentsEnabled(true);
    }

    private void populateInputs() {
        String label = (String)serviceComboBox.getSelectedItem();
        XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
        if(auto != null && label.equals(auto.getLabel())) {
            usernameField.setText(auto.getUsername());
            passwordField.setText(auto.getPassword());
            autoLoginCheckBox.setSelected(true);
        } else {
            usernameField.setText("");
            passwordField.setText("");
            autoLoginCheckBox.setSelected(false);
        }
        authFailedLabel.setVisible(false);
    }

    void disconnected(Exception reason) {
        setSignInComponentsEnabled(true);
        populateInputs();
        if(reason != null) {
            loginFailed(reason);
        } else {
            authFailedLabel.setVisible(false);
        }
    }

    private void loginFailed(Exception reason) {
        authFailedLabel.setVisible(true);
        if (reason.getMessage().contains("authentication failed")) {
            authFailedLabel.setText(AUTHENTICATION_ERROR);
            passwordField.setText("");
        } else {
            authFailedLabel.setText(NETWORK_ERROR);
        }
    }

    private void setSignInComponentsEnabled(boolean isEnabled) {
        signinAction.setEnabled(isEnabled);
        signinAction.putValue(Action.NAME, isEnabled ? SIGNIN_ENABLED_TEXT : SIGNIN_DISABLED_TEXT);
        usernameField.setEnabled(isEnabled);
        passwordField.setEnabled(isEnabled);
        autoLoginCheckBox.setEnabled(isEnabled);
    }

    void connected(XMPPConnectionConfiguration config) {
        setSignInComponentsEnabled(true);
    }

    private void login(final XMPPAccountConfiguration config) {
        setSignInComponentsEnabled(false);
        authFailedLabel.setVisible(false);
        BackgroundExecutorService.execute(new Runnable() {
            public void run() {
                xmppService.login(config);
            }
        });            
    }

    class SignInAction extends AbstractAction {
        public SignInAction() {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            String user = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if(user.equals("") || password.equals("")) {
                return;
            }            
            String label = (String)serviceComboBox.getSelectedItem();
            XMPPAccountConfiguration config = accountManager.getConfig(label);
            config.setUsername(user);
            config.setPassword(password);
            if(autoLoginCheckBox.isSelected()) {
                // Set this as the auto-login account
                accountManager.setAutoLoginConfig(config);
            } else {
                passwordField.setText("");
                
                // If this was previously the auto-login account, delete it
                XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
                if(auto != null && auto.getLabel().equals(label)) {
                    accountManager.setAutoLoginConfig(null);
                }
            }
            login(config);
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

    public void connecting(XMPPConnectionConfiguration config) {
        setSignInComponentsEnabled(false);
    }
}