package org.limewire.ui.swing.friends;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Resource;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import static org.limewire.ui.swing.util.I18n.tr;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPErrorListener;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * TODO: Swap labels on network button press?
 */
@Singleton
public class LoginPanel extends JPanel implements Displayable, XMPPErrorListener {
    @Resource private Icon gmail;
    @Resource private Icon facebook;

    private static final String SIGNIN_ENABLED_TEXT = tr("Sign in");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in ...");

    private JToggleButton googleTalkButton;
    private JToggleButton facebookButton;
    private JPasswordField passwordField;
    private JCheckBox rememberMeCheckbox;
    private JButton signInButton;
    private JTextField userNameField;
    private JPanel topPanel;
    private final XMPPEventHandler xmppEventHandler;
    private static final String GMAIL_SERVICE_NAME = "gmail.com";
    private static final String FACEBOOK_SERVICE_NAME = "facebook.com";
    private static final Log LOG = LogFactory.getLog(LoginPanel.class);
    private ButtonGroup networkGroup;

    @Inject
    public LoginPanel(XMPPEventHandler xmppEventHandler) {
        this.xmppEventHandler = xmppEventHandler;
        GuiUtils.assignResources(this);

        initComponents();
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    private void initComponents() {
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
        setInitialButtonState();

        FormLayout layout = new FormLayout("7dlu, p, 7dlu", "7dlu, p, 10dlu, p, 7dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        
        topPanel = new JPanel();
        topPanel.add(normalTopPanel());
        builder.add(topPanel, cc.xy(2, 2));
        builder.add(getDetailsPanel(), cc.xy(2, 4));
        add(builder.getPanel());
        
        populateInputs();
    }

    /**
     * Method used to set initial button characteristics when login panel ui loads up
     */
    private void setInitialButtonState() {
        setSignInComponentsEnabled(!(xmppEventHandler.hasAutoLogin(GMAIL_SERVICE_NAME)));
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
        XMPPConnectionConfiguration config = xmppEventHandler.getConfig(GMAIL_SERVICE_NAME);
        
        //FIXME: Temporary guard 
        if (config == null) {
            return;
        }
        
        if (config.isAutoLogin()) {
            userNameField.setText(config.getUsername());
            passwordField.setText(config.getPassword());
        }
        rememberMeCheckbox.setSelected(true);
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
        FormLayout layout = new FormLayout("p, 4dlu, p:g", "p, 3dlu, p, p, p");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        //builder.addLabel(tr("Have a Gmail or Facebook account?"), cc.xyw(1, 1, 3));
        builder.addLabel(tr("Have a Gmail account?"), cc.xyw(1, 1, 3));
        builder.addLabel(tr("- Access your friends' libraries"), cc.xy(3, 3));
        builder.addLabel(tr("- See what new files they have"), cc.xy(3, 4));
        builder.addLabel(tr("- Chat with your friends"), cc.xy(3, 5));
        return builder.getPanel();
    }
    
    private void loginFailed(final XMPPException e1) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                String errorMsg = e1.getMessage();
                final LoginErrorState error = errorMsg.contains("authentication failed") ? 
                LoginErrorState.UsernameOrPasswordError : LoginErrorState.NetworkError;
        
                setTopPanelMessage(noConnectionAvailablePanel(error));
                if (error == LoginErrorState.UsernameOrPasswordError) {
                    passwordField.setText("");
                }
                setSignInComponentsEnabled(true);
            }

        });
    }

    private void setTopPanelMessage(JPanel messagePanel) {
        topPanel.removeAll();
        topPanel.add(messagePanel);
    }
    
    private JPanel noConnectionAvailablePanel(LoginErrorState error) {
        FormLayout layout = new FormLayout("c:p:g", "7dlu,p, p, 7dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.nextLine();
        JLabel label = builder.addLabel(tr("Could not log you in."));
        //A pretty crimson
        label.setForeground(new Color(112, 13, 37));
        FontUtils.changeSize(label, 2.0f);
        builder.nextLine();
        builder.addLabel(tr(error.getMessage()));
        return builder.getPanel();
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
        
        networkGroup = new ButtonGroup();
        googleTalkButton = new JToggleButton(tr("Gmail"), gmail);
        googleTalkButton.setActionCommand(GMAIL_SERVICE_NAME);
        networkGroup.add(googleTalkButton);
        facebookButton = new JToggleButton(tr("Facebook"), facebook);
        facebookButton.setActionCommand(FACEBOOK_SERVICE_NAME);
        //networkGroup.add(facebookButton);
        networkGroup.setSelected(googleTalkButton.getModel(), true);
        
        FormLayout layout = new FormLayout("l:p, 2dlu, p", "p");
        PanelBuilder networksBuilder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        networksBuilder.add(googleTalkButton, cc.xy(1, 1));
        //networksBuilder.add(facebookButton, cc.xy(3, 1));
        JPanel networkPanel = networksBuilder.getPanel();
        
        layout = new FormLayout("l:p", "p, p");
        PanelBuilder detailsPanelBuilder = new PanelBuilder(layout);
        detailsPanelBuilder.add(networkPanel, cc.xy(1, 1));
        
        layout = new FormLayout("7dlu, l:p, 7dlu", "7dlu, p, p, 2dlu, p, p, 2dlu, p, 3dlu, p, 7dlu");
        PanelBuilder inputBuilder = new PanelBuilder(layout);
        inputBuilder.addLabel(tr("Gmail address"), cc.xy(2, 2));
        inputBuilder.add(userNameField, cc.xy(2, 3));
        inputBuilder.addLabel(tr("Password"), cc.xy(2, 5));
        inputBuilder.add(passwordField, cc.xy(2, 6));
        inputBuilder.add(rememberMeCheckbox, cc.xy(2, 8));
        inputBuilder.add(signInButton, cc.xy(2, 10));
        
        JPanel inputPanel = inputBuilder.getPanel();
        inputPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        detailsPanelBuilder.add(inputPanel, cc.xy(1, 2));
        return detailsPanelBuilder.getPanel();
    }
    
    private static enum LoginErrorState {
        UsernameOrPasswordError("<html><center>Incorrect email/password combination.<br/>Please try again</center></html>"),
        NetworkError("Network error. Please try again later");
        
        private final String message;
        LoginErrorState(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    class SignInAction extends AbstractAction {
        public SignInAction() {
            super();
        }
        
        public void actionPerformed(ActionEvent e) {
            setSignInComponentsEnabled(false);
            String userNameFieldValue = userNameField.getText().trim();
            final String serviceName = networkGroup.getSelection().getActionCommand();
            if (GMAIL_SERVICE_NAME.equals(serviceName)) {
                // TODO handle empty String
                if(!userNameFieldValue.endsWith("@gmail.com")) {
                    // TODO ignoreCase?
                    userNameFieldValue += "@gmail.com";
                }
            }
            final String userName = userNameFieldValue;
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    try {
                        xmppEventHandler.login(serviceName, userName,
                                new String(passwordField.getPassword()), rememberMeCheckbox.isSelected());

                        //Reset the top panel incase the last go-round was a bad password or
                        //network error case.
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                setTopPanelMessage(normalTopPanel());
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