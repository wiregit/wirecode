package org.limewire.ui.swing.friends.login;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
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
    
    private final XMPPAccountConfiguration accountConfig;
    private final LoginPopupPanel parent;
    private final XMPPService xmppService;
    private final XMPPAccountConfigurationManager accountManager;
    
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

        JPanel contentPanel = new JPanel(new MigLayout("nogrid, gap 0, insets 4 5 8 4, fill, alignx left"));
        
        contentPanel.add(authFailedLabel, "gapleft 2, wmin 0, hidemode 3, gapbottom 3, wrap");
        contentPanel.add(serviceField, "gapbottom 8, hidemode 3, grow, wmin 0, wrap");
        contentPanel.add(usernameField, "gapbottom 8, grow, wrap");
        contentPanel.add(passwordField, "gapbottom 4, grow, wrap");
        contentPanel.add(autoLoginCheckBox, "gapbottom 3, wmin 0, wrap");
        contentPanel.add(signInButton);
        
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
