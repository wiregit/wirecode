package org.limewire.ui.swing.friends.login;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.PromptPasswordField;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class XMPPUserEntryLoginPanel extends JPanel {
    
    private static final String SIGNIN_ENABLED_TEXT = tr("Sign In");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in...");
    private static final String AUTHENTICATION_ERROR = tr("Incorrect username or password.");
    private static final String NETWORK_ERROR = tr("Network error.");
    
    private PromptTextField serviceField;
    private PromptTextField usernameField;
    private PromptPasswordField passwordField;
    private JCheckBox autoLoginCheckBox;
    private JLabel authFailedLabel;
    private JXButton signInButton;
    private final SignInAction signinAction = new SignInAction();
    
    private ListenerSupport<XMPPConnectionEvent> connectionSupport = null;
    
    private final XMPPAccountConfiguration accountConfig;
    private final LoginPopupPanel parent;
    private final XMPPService xmppService;
    private final XMPPAccountConfigurationManager accountManager;
    private EventListener<XMPPConnectionEvent> connectionListener;
    
    @Inject
    public XMPPUserEntryLoginPanel(@Assisted XMPPAccountConfiguration accountConfig, LoginPopupPanel parent,
            XMPPService xmppService, XMPPAccountConfigurationManager accountManager,
            ButtonDecorator buttonDecorator,
            TextFieldDecorator textFieldDecorator) {
    
        super(new BorderLayout());
        
        this.accountConfig = accountConfig;
        this.parent = parent;
        this.xmppService = xmppService;
        this.accountManager = accountManager;
        
        add(new JLabel(I18n.tr("Sign in with {0}",accountConfig.getLabel()),
                accountConfig.getLargeIcon(), JLabel.HORIZONTAL), BorderLayout.NORTH);
        
        initComponents(buttonDecorator, textFieldDecorator);
        populateInputs();
        setSignInComponentsEnabled(true);
    }
    
    @Inject
    void registerListener(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        
        this.connectionSupport = connectionSupport;
        
        connectionListener = new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case CONNECTING:
                    connecting(event.getSource().getConfiguration());
                    break;
                case CONNECTED:
                    connected(event.getSource().getConfiguration());
                    break;
                case DISCONNECTED:
                case CONNECT_FAILED:
                    // Ignore duplicate events caused by authentication
                    // errors and events caused by deliberately signing
                    // out or switching user
                    Exception reason = event.getException();
                    if(reason != null) {
                        disconnected(reason);
                    }
                }
            }
        };
        
        connectionSupport.addListener(connectionListener);
    }
    
    private void unregisterListener() {
        if (connectionSupport != null) {
            connectionSupport.removeListener(connectionListener);
        }
    }
   
    private void initComponents(ButtonDecorator buttonDecorator,
            TextFieldDecorator textFieldDecorator) {
        
        serviceField = new PromptTextField(tr("Domain"));
        textFieldDecorator.decoratePromptField(serviceField, AccentType.NONE);
        
        usernameField = new PromptTextField(tr("Username"));
        textFieldDecorator.decoratePromptField(usernameField, AccentType.NONE);
        passwordField = new PromptPasswordField(tr("Password"));
        textFieldDecorator.decoratePromptField(passwordField, AccentType.NONE);
        passwordField.setAction(signinAction);
        
        ResizeUtils.forceSize(usernameField, new Dimension(139, 22));
        ResizeUtils.forceSize(passwordField, new Dimension(139, 22));

        autoLoginCheckBox = new JCheckBox(tr("Remember me")); 
        autoLoginCheckBox.setSelected(SwingUiSettings.REMEMBER_ME_CHECKED.getValue());
        autoLoginCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // When the user clears the auto-login checkbox,
                // forget the auto-login config
                if(!autoLoginCheckBox.isSelected()) {
                    accountManager.setAutoLoginConfig(null);
                }
                SwingUiSettings.REMEMBER_ME_CHECKED.setValue(autoLoginCheckBox.isSelected());
            }
        });
        autoLoginCheckBox.setName("LoginPanel.autoLoginCheckBox");
        autoLoginCheckBox.setOpaque(false);

        signInButton = new JXButton(signinAction);
        buttonDecorator.decorateDarkFullButton(signInButton, AccentType.NONE);
        signInButton.setBorder(BorderFactory.createEmptyBorder(0,15,2,15));
        ResizeUtils.looseForceHeight(signInButton, 22);
        
        authFailedLabel = new MultiLineLabel();
        authFailedLabel.setVisible(false);
        authFailedLabel.setName("LoginPanel.authFailedLabel");

        HyperlinkButton goBackButton = new HyperlinkButton(new AbstractAction(I18n.tr("Choose another account")) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                unregisterListener();
                parent.restart();                
            }
        });
        
        JPanel contentPanel = new JPanel(new MigLayout("nogrid, gap 0, insets 4 5 8 4, fill, alignx left"));
        
        contentPanel.add(authFailedLabel, "gapleft 2, wmin 0, hidemode 3, gapbottom 3, wrap");
        contentPanel.add(serviceField, "gapbottom 8, hidemode 3, grow, wmin 0, wrap");
        contentPanel.add(usernameField, "gapbottom 8, grow, wrap");
        contentPanel.add(passwordField, "gapbottom 4, grow, wrap");
        contentPanel.add(autoLoginCheckBox, "gapbottom 3, wmin 0, wrap");
        contentPanel.add(signInButton, "wrap");
        contentPanel.add(goBackButton);
        
        add(contentPanel, BorderLayout.CENTER);
    }
    
    private void setSignInComponentsEnabled(boolean isEnabled) {
        signinAction.setEnabled(isEnabled);
        signinAction.putValue(Action.NAME, isEnabled ? SIGNIN_ENABLED_TEXT : SIGNIN_DISABLED_TEXT);
        serviceField.setEnabled(isEnabled);
        usernameField.setEnabled(isEnabled);
        passwordField.setEnabled(isEnabled);
        autoLoginCheckBox.setEnabled(isEnabled);
    }
    
    private void populateInputs() {
        if(accountConfig.getLabel().equals("Jabber")) {
            serviceField.setVisible(true);
        } else {
            serviceField.setVisible(false);
        }
        
        if(accountConfig == accountManager.getAutoLoginConfig()) {
            serviceField.setText(accountConfig.getServiceName());
            usernameField.setText(accountConfig.getUserInputLocalID());
            passwordField.setText(accountConfig.getPassword());
            autoLoginCheckBox.setSelected(true);
        } else {
            serviceField.setText("");
            usernameField.setText("");
            passwordField.setText("");
        }
        validate();
        repaint();
    }
    
    private void login(final XMPPAccountConfiguration config) {
        setSignInComponentsEnabled(false);
        authFailedLabel.setVisible(false);
        validate();
        repaint();
        xmppService.login(config);         
    }

    void connected(XMPPConnectionConfiguration config) {
        unregisterListener();
        parent.finished();
    }

    void disconnected(Exception reason) {
        setSignInComponentsEnabled(true);
        populateInputs();
        if(reason !=null && reason.getMessage() != null && reason.getMessage().toLowerCase(Locale.US).contains("auth")) {
            authFailedLabel.setText(AUTHENTICATION_ERROR);
            passwordField.setText("");
        } else {
            authFailedLabel.setText(NETWORK_ERROR);
        }
        authFailedLabel.setVisible(true);
        validate();
        repaint();
    }
    
    public void connecting(XMPPConnectionConfiguration config) {
        setSignInComponentsEnabled(false);
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
            if(accountConfig.getLabel().equals("Jabber")) {
                String service = serviceField.getText().trim();
                if(service.equals(""))
                    return;
                accountConfig.setServiceName(service);
            }
            accountConfig.setUsername(user);
            accountConfig.setPassword(password);
            if(autoLoginCheckBox.isSelected()) {
                // Set this as the auto-login account
                accountManager.setAutoLoginConfig(accountConfig);
            } else {
                // If there was previously an auto-login account, delete it
                accountManager.setAutoLoginConfig(null);
            }
            login(accountConfig);
        }
    }
}
