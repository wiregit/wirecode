package org.limewire.ui.swing.friends;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractAction;
// import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
// import org.jdesktop.application.Resource;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.xmpp.api.client.XMPPConnection;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
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
public class LoginPanel extends JPanel implements Displayable, XMPPErrorListener {
//    @Resource private Icon gmail;
//    @Resource private Icon facebook;

    private static final String SIGNIN_ENABLED_TEXT = tr("Sign in");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in ...");
    
    private static final String AUTHENTICATION_ERROR = tr("Please try again."); // See spec
    private static final String NETWORK_ERROR = tr("Network error. Please try again later.");
    
    private JComboBox serviceComboBox;
    private JTextField userNameField;
    private JPasswordField passwordField;
    private JCheckBox rememberMeCheckbox;
    private JButton signInButton;
    private JPanel normalTopPanel;
    private JPanel detailsPanel;
    private final XMPPEventHandler xmppEventHandler;
    private static final Log LOG = LogFactory.getLog(LoginPanel.class);

    @Inject
    public LoginPanel(XMPPEventHandler xmppEventHandler) {
        this.xmppEventHandler = xmppEventHandler;
        GuiUtils.assignResources(this);
        initComponents();
        EventAnnotationProcessor.subscribe(this);
    }

    private void initComponents() {
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
                userNameField.setText(config.getMyID());
                passwordField.setText(config.getPassword());
                rememberMeCheckbox.setSelected(config.isAutoLogin());
            }
        });
        userNameField = new JTextField(18);
        passwordField = new JPasswordField(18);

        userNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                clearPassword();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                clearPassword();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                clearPassword();
            }

            private void clearPassword() { passwordField.setText(""); }
        });

        SignInAction signinAction = new SignInAction();
        passwordField.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "signin");
        passwordField.getActionMap().put("signin", signinAction);

        rememberMeCheckbox = new JCheckBox(tr("Remember me"));
        signInButton = new JButton(signinAction);

        setLayout(new MigLayout("gapy 10"));
        normalTopPanel = normalTopPanel();
        detailsPanel = getDetailsPanel();
        add(normalTopPanel, "wrap");
        add(detailsPanel);
        
        setSignInComponentsEnabled(true);
        populateInputs();
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
        String friendlyName = (String)serviceComboBox.getSelectedItem();
        XMPPConnectionConfiguration config =
            xmppEventHandler.getConfigByFriendlyName(friendlyName);
        if(config.isAutoLogin()) {
            userNameField.setText(config.getUsername());
            passwordField.setText(config.getPassword());
            rememberMeCheckbox.setSelected(true);
            setSignInComponentsEnabled(false);
        }
    }

    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        if (!rememberMeCheckbox.isSelected()) {
            userNameField.setText("");
            passwordField.setText("");
        }
    }

    @Override
    public void handleDisplay() {
        userNameField.requestFocusInWindow();
    }

    private JPanel normalTopPanel() {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout("gapy 10"));
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
    }

    private JPanel loginErrorPanel(String message) {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout("gapy 10"));
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

        userNameField.setEnabled(isEnabled);
        passwordField.setEnabled(isEnabled);
        rememberMeCheckbox.setEnabled(isEnabled);
    }

    @EventSubscriber
    public void handleSigninEvent(XMPPConnectionEstablishedEvent event) {
        setSignInComponentsEnabled(true);
    }

    private JPanel getDetailsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new MigLayout("gapy 10"));
        p.add(new JLabel(tr("Sign in using")), "split");
        p.add(serviceComboBox, "wrap");
        p.add(new JLabel(tr("Username")), "split");
        p.add(userNameField, "wrap");
        p.add(new JLabel(tr("Password")), "split");
        p.add(passwordField, "wrap");
        p.add(rememberMeCheckbox, "wrap");
        p.add(signInButton, "wrap");
        return p;
    }

    class SignInAction extends AbstractAction {
        public SignInAction() {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            setSignInComponentsEnabled(false);
            String userNameFieldValue = userNameField.getText().trim();
            String friendlyName = (String)serviceComboBox.getSelectedItem();
            XMPPConnectionConfiguration config =
                xmppEventHandler.getConfigByFriendlyName(friendlyName);
            // Some servers expect the domain to be included, others don't
            int at = userNameFieldValue.indexOf('@');
            if(config.requiresDomain() && at == -1)
                userNameFieldValue += "@" + config.getServiceName(); // Guess
            else if(!config.requiresDomain() && at > -1)
                userNameFieldValue = userNameFieldValue.substring(0, at);
            final String userName = userNameFieldValue;
            final String password = new String(passwordField.getPassword());
            final String serviceName = config.getServiceName();
            // TODO: handle empty username/password
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        xmppEventHandler.login(serviceName, userName,
                                password, rememberMeCheckbox.isSelected());

                        //Reset the top panel incase the last go-round was a bad password or
                        //network error case.
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setTopPanelMessage(normalTopPanel);
                                setSignInComponentsEnabled(true);
                            }
                        });

                    } catch (XMPPException e1) {
                        LOG.error("Unable to login", e1);
                        loginFailed(e1);
                    }
                }
            }, "xmpp-login");            
        }
    }
}