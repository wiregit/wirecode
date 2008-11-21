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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;
import org.limewire.ui.swing.friends.XMPPEventHandler;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc.
 */
@Singleton
public class LoginPanel extends JXPanel {

    private static final String SIGNIN_ENABLED_TEXT = tr("Sign in");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in ...");

    private static final String AUTHENTICATION_ERROR = tr("Please try again."); // See spec
    private static final String NETWORK_ERROR = tr("Network error. Please try again later.");

    private JComboBox serviceComboBox;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox autoLoginCheckBox;
    private JLabel authFailedLabel;
    private JButton signInButton;
    private final XMPPEventHandler xmppEventHandler;
    private final XMPPAccountConfigurationManager accountManager;
    private final SignInAction signinAction = new SignInAction();
    // private static final Log LOG = LogFactory.getLog(LoginPanel.class);

    @Inject
    LoginPanel(XMPPEventHandler xmppEventHandler,
            XMPPAccountConfigurationManager accountManager) {
        this.xmppEventHandler = xmppEventHandler;
        this.accountManager = accountManager;
        GuiUtils.assignResources(this);
        initComponents();
        EventAnnotationProcessor.subscribe(this);
    }
    
    @Inject void register(ServiceRegistry registry) {
        registry.register(new Service() {
            @Override
            public String getServiceName() {
                return tr("Friend Auto-Login");
            }
            @Override
            public void initialize() {
            }
            
            @Override
            public void start() {
                // If there's an auto-login account, select it and log in
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
                        serviceComboBox.setSelectedItem(auto.getLabel());
                        login(auto);
                    }
                });
            }
            
            @Override
            public void stop() {
            }
        });
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
        authFailedLabel.setOpaque(false);
        FontUtils.changeStyle(authFailedLabel, Font.ITALIC);
        
        setBackgroundPainter(new RectanglePainter<JXPanel>(2, 2, 2, 2, 5, 5, true, Color.LIGHT_GRAY, 0f, Color.LIGHT_GRAY));

        serviceComboBox.setSelectedItem("Gmail");
        setSignInComponentsEnabled(true);
    }

    @Inject void register(XMPPService xmppService) {
        xmppService.setXmppErrorListener(new EventListener<XMPPException>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPException event) {
                loginFailed(event);
            }
        });
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

    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        setSignInComponentsEnabled(true);
        populateInputs();
    }

    private void loginFailed(XMPPException e) {
        authFailedLabel.setVisible(true);
        if (e.getMessage().contains("authentication failed")) {
            authFailedLabel.setText(AUTHENTICATION_ERROR);
            passwordField.setText("");
        } else {
            authFailedLabel.setText(NETWORK_ERROR);
        }
        setSignInComponentsEnabled(true);
    }

    private void setSignInComponentsEnabled(boolean isEnabled) {
        signinAction.setEnabled(isEnabled);
        signinAction.putValue(Action.NAME, isEnabled ? SIGNIN_ENABLED_TEXT : SIGNIN_DISABLED_TEXT);
        usernameField.setEnabled(isEnabled);
        passwordField.setEnabled(isEnabled);
        autoLoginCheckBox.setEnabled(isEnabled);
    }

    @EventSubscriber
    public void handleSigninEvent(XMPPConnectionEstablishedEvent event) {
        setSignInComponentsEnabled(true);
    }

    private void login(final XMPPAccountConfiguration config) {
        setSignInComponentsEnabled(false);
        authFailedLabel.setVisible(false);
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
            if(config != null) {
                setIcon(config.getIcon());
            }
            return this;
        }
    }
}