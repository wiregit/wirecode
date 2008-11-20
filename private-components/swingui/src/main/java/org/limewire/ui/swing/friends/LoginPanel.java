package org.limewire.ui.swing.friends;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.concurrent.ThreadExecutor;
//import org.limewire.logging.Log;
//import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * TODO: Swap labels on network button press?
 */
@Singleton
public class LoginPanel extends JPanel
implements Displayable, XMPPErrorListener, ActionListener {

    private static final String SIGNIN_ENABLED_TEXT = tr("Sign in");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in ...");

    private static final String AUTHENTICATION_ERROR = tr("Please try again."); // See spec
    private static final String NETWORK_ERROR = tr("Network error. Please try again later.");
    private static final String BLANK_CREDENTIALS = tr("Please enter your username and password.");

    private JComboBox serviceComboBox;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox autoLoginCheckBox;
    private JButton signInButton;
    private JButton registerButton;
    private JPanel normalTopPanel;
    private JPanel detailsPanel;
    private final XMPPEventHandler xmppEventHandler;
    private final XMPPAccountConfigurationManager accountManager;
    // private static final Log LOG = LogFactory.getLog(LoginPanel.class);

    @Inject
    public LoginPanel(XMPPEventHandler xmppEventHandler,
            XMPPAccountConfigurationManager accountManager) {
        this.xmppEventHandler = xmppEventHandler;
        this.accountManager = accountManager;
        GuiUtils.assignResources(this);
        initComponents();
        EventAnnotationProcessor.subscribe(this);
    }

    private void initComponents() {
        serviceComboBox = new JComboBox();
        for(String label : accountManager.getLabels())
            serviceComboBox.addItem(label); // FIXME: icons?
        serviceComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                populateInputs();
            }
        });
        serviceComboBox.setRenderer(new Renderer());
        usernameField = new JTextField(18);
        passwordField = new JPasswordField(18);

        SignInAction signinAction = new SignInAction();
        passwordField.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "signin");
        passwordField.getActionMap().put("signin", signinAction);

        autoLoginCheckBox = new JCheckBox(tr("Remember me"));
        signInButton = new JButton(signinAction);
        registerButton = new JButton(tr("Sign up"));

        setLayout(new MigLayout());
        normalTopPanel = normalTopPanel();
        detailsPanel = getDetailsPanel();
        add(normalTopPanel, "wrap");
        add(detailsPanel);

        // If there's an auto-login account, select it and log in
        XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
        if(auto == null) {
            serviceComboBox.setSelectedItem("Gmail");
            setSignInComponentsEnabled(true);
        } else {
            serviceComboBox.setSelectedItem(auto.getLabel());
            login(auto);
        }
    }

    @Override
    public void error(XMPPException exception) {
        loginFailed(exception);
    }

    @Inject
    public void register(XMPPService xmppService) {
        xmppService.setXmppErrorListener(this);
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
    }

    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        setTopPanelMessage(normalTopPanel);
        setSignInComponentsEnabled(true);
        populateInputs();
    }

    @Override
    public void handleDisplay() {
        usernameField.requestFocusInWindow();
        populateInputs(); // Config may have been changed in options dialog
    }

    private JPanel normalTopPanel() {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout());
        p.add(new JLabel(tr("Have a Jabber account?")), "wrap");
        p.add(new JLabel(tr("- Access your friends' libraries")), "wrap");
        p.add(new JLabel(tr("- See what new files they have")), "wrap");
        p.add(new JLabel(tr("- Chat with your friends")));
        return p;
    }

    private void loginFailed(final XMPPException e) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (e.getMessage().contains("authentication failed")) {
                    setTopPanelMessage(loginErrorPanel(AUTHENTICATION_ERROR));
                    passwordField.setText("");
                } else {
                    setTopPanelMessage(loginErrorPanel(NETWORK_ERROR));
                }
                setSignInComponentsEnabled(true);
            }

        });
    }

    private void setTopPanelMessage(JPanel messagePanel) {
        removeAll();
        add(messagePanel, "wrap");
        add(detailsPanel);
        validate();
    }

    private JPanel loginErrorPanel(String message) {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout());
        JLabel label = new JLabel(tr("Could not sign you in."));
        //A pretty crimson
        label.setForeground(new Color(112, 13, 37));
        FontUtils.changeSize(label, 2.0f);
        p.add(label, "wrap");
        p.add(new JLabel(message));
        return p;
    }

    private void setSignInComponentsEnabled(boolean isEnabled) {
        signInButton.setEnabled(isEnabled);
        signInButton.setText(isEnabled ? SIGNIN_ENABLED_TEXT : SIGNIN_DISABLED_TEXT);
        registerButton.setEnabled(isEnabled);
        usernameField.setEnabled(isEnabled);
        passwordField.setEnabled(isEnabled);
        autoLoginCheckBox.setEnabled(isEnabled);
    }

    @EventSubscriber
    public void handleSigninEvent(XMPPConnectionEstablishedEvent event) {
        setSignInComponentsEnabled(true);
    }

    private JPanel getDetailsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout());
        p.add(new JLabel(tr("Sign in using")), "split");
        p.add(serviceComboBox, "wrap");
        p.add(new JLabel(tr("Username")), "split");
        p.add(usernameField, "wrap");
        p.add(new JLabel(tr("Password")), "split");
        p.add(passwordField, "wrap");
        JPanel sign = new JPanel();
        sign.setLayout(new MigLayout());
        sign.add(autoLoginCheckBox, "wrap");
        sign.add(signInButton);
        p.add(sign, "split");
        JPanel reg = new JPanel();
        reg.setBorder(new LineBorder(Color.BLACK));
        reg.setLayout(new MigLayout());
        reg.add(new JLabel(tr("Don't have an account?")), "wrap");
        reg.add(registerButton);
        p.add(reg);
        registerButton.addActionListener(this);
        return p;
    }

    // ActionListener for registerButton
    @Override
    public void actionPerformed(ActionEvent e) {
        String label = (String)serviceComboBox.getSelectedItem();
        XMPPAccountConfiguration config = accountManager.getConfig(label);
        NativeLaunchUtils.openURL(config.getRegistrationURL());
    }

    private void login(final XMPPAccountConfiguration config) {
        setSignInComponentsEnabled(false);
        ThreadExecutor.startThread(new Runnable() {
            public void run() {
                xmppEventHandler.login(config);
            }
        }, "xmpp-login");            
    }

    class SignInAction extends AbstractAction {
        public SignInAction() {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            String user = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if(user.equals("") || password.equals("")) {
                setTopPanelMessage(loginErrorPanel(BLANK_CREDENTIALS));
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
                // If this was previously the auto-login account, delete it
                XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
                if(auto != null && auto.getLabel().equals(label))
                    accountManager.setAutoLoginConfig(null);
            }
            login(config);
        }
    }
    
    class Renderer extends JLabel implements ListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            String label = value.toString();
            setText(label);
            XMPPAccountConfiguration config = accountManager.getConfig(label);
            if(config != null)
                setIcon(config.getIcon());
            return this;
        }
    }
}