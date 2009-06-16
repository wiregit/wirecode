package org.limewire.ui.swing.friends.login;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.friend.Network.Type;
import org.limewire.core.api.friend.client.FriendConnectionConfiguration;
import org.limewire.core.api.friend.client.FriendConnectionFactory;
import org.limewire.listener.EventListener;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.LimeDomListener;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.PromptPasswordField;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsICookieService;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsIIOService;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIURI;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class LoginPanel extends JXPanel implements SettingListener {
    
    private static final String SIGNIN_ENABLED_TEXT = tr("Sign In");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in...");
    private static final String AUTHENTICATION_ERROR = tr("Incorrect username or password.");
    private static final String NETWORK_ERROR = tr("Network error.");
    
    private static final String CONFIG = "limewire.configProperty";

    private LimeComboBox serviceComboBox;
    private PromptTextField serviceField;
    private PromptTextField usernameField;
    private PromptPasswordField passwordField;
    private JCheckBox autoLoginCheckBox;
    private JLabel authFailedLabel;
    private JXButton signInButton;
    private final FriendAccountConfigurationManager accountManager;
    private final FriendConnectionFactory friendConnectionFactory;
    private final SignInAction signinAction = new SignInAction();

    @Inject
    LoginPanel(FriendAccountConfigurationManager accountManager,
            FriendConnectionFactory friendConnectionFactory,
            ComboBoxDecorator comboFactory,
            ButtonDecorator buttonDecorator,
            BarPainterFactory barPainterFactory,
            TextFieldDecorator textFieldDecorator) {
        
        GuiUtils.assignResources(this);
        
        this.accountManager = accountManager;
        this.friendConnectionFactory = friendConnectionFactory;

        SwingUiSettings.XMPP_AUTO_LOGIN.addSettingListener(this);
        initComponents(comboFactory, buttonDecorator, textFieldDecorator, barPainterFactory);
    }
    
    private Action getActionForConfig(FriendAccountConfiguration config) {
        for(Action action : serviceComboBox.getActions()) {
            if(action.getValue(CONFIG).equals(config)) {
                return action;
            }
        }
        return null;
    }
    
    private Action getActionForLabel(String label) {
        for(Action action : serviceComboBox.getActions()) {
            if(((FriendAccountConfiguration)action.getValue(CONFIG)).getLabel().equals(label)) {
                return action;
            }
        }
        return null;
    }

    void autoLogin(FriendAccountConfiguration auto) {
        serviceComboBox.setSelectedAction(getActionForConfig(auto));
        login(auto);
    }    

    @Override
    public void setVisible(boolean flag) {
        boolean becameVisible = flag && !isVisible();
        super.setVisible(flag);
        if(becameVisible) {
            populateInputs();
            // Give focus to the first visible, empty text field
            if(serviceField.isVisible()) {
                if(serviceField.getText().trim().equals(""))
                    serviceField.requestFocusInWindow();
            } else {
                if(usernameField.getText().trim().equals(""))
                    usernameField.requestFocusInWindow();
            }
        }
    }

    private void initComponents(ComboBoxDecorator comboFactory, ButtonDecorator buttonDecorator,
            TextFieldDecorator textFieldDecorator, BarPainterFactory barPainterFactory) {
        
        JLabel titleLabel = new JLabel(tr("Sign in with"));
        titleLabel.setName("LoginPanel.titleLabel");
        
        List<Action> actions = new ArrayList<Action>();
        for (FriendAccountConfiguration config : accountManager.getConfigurations()) {
            Action action = new AbstractAction(config.getLabel(), config.getIcon()) {            
                @Override
                public void actionPerformed(ActionEvent e) {}
            };
            action.putValue(CONFIG, config);
            actions.add(action);
        }        
        serviceComboBox = new LimeComboBox(actions);
        comboFactory.decorateDarkFullComboBox(serviceComboBox, AccentType.NONE);
        
        serviceComboBox.addSelectionListener(new LimeComboBox.SelectionListener() {
            @Override
            public void selectionChanged(Action item) {
                populateInputs();
            }
        });
        ResizeUtils.looseForceHeight(serviceComboBox, 22);
        
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

        JButton hideButton = new IconButton();
        hideButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoginPanel.this.setVisible(false);
            }
        });
        hideButton.setName("LoginPanel.hideButton");

        setLayout(new MigLayout("nogrid, gap 0, insets 4 5 8 4, fill, alignx left"));
        
        add(titleLabel, "gaptop 1, gapleft 2, gapbottom 4");
        add(hideButton, "alignx right, aligny top, gapbefore push, wrap");
        add(authFailedLabel, "gapleft 2, wmin 0, hidemode 3, gapbottom 3, wrap");
        add(serviceComboBox, "gapbottom 8, wmin 0, wrap");
        add(serviceField, "gapbottom 8, hidemode 3, grow, wmin 0, wrap");
        add(usernameField, "gapbottom 8, grow, wrap");
        add(passwordField, "gapbottom 4, grow, wrap");
        add(autoLoginCheckBox, "gapbottom 3, wmin 0, wrap");
        add(signInButton);

        setBackgroundPainter(barPainterFactory.createFriendsBarPainter());

        serviceComboBox.setSelectedAction(getActionForLabel("Facebook"));
        setSignInComponentsEnabled(true);
    }

    private void populateInputs() {
        FriendAccountConfiguration config = (FriendAccountConfiguration)serviceComboBox.getSelectedAction().getValue(CONFIG);
        if(config.getLabel().equals("Jabber")) {
            serviceField.setVisible(true);
        } else {
            serviceField.setVisible(false);
        }
        
        if(config == accountManager.getAutoLoginConfig()) {
            serviceField.setText(config.getServiceName());
            usernameField.setText(config.getUserInputLocalID());
            passwordField.setText(config.getPassword());
            autoLoginCheckBox.setSelected(true);
        } else {
            serviceField.setText("");
            usernameField.setText("");
            passwordField.setText("");
            // Preserve prior state.
//            autoLoginCheckBox.setSelected(false);
        }
        validate();
        repaint();
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

    private void setSignInComponentsEnabled(boolean isEnabled) {
        signinAction.setEnabled(isEnabled);
        signinAction.putValue(Action.NAME, isEnabled ? SIGNIN_ENABLED_TEXT : SIGNIN_DISABLED_TEXT);
        serviceField.setEnabled(isEnabled);
        usernameField.setEnabled(isEnabled);
        passwordField.setEnabled(isEnabled);
        autoLoginCheckBox.setEnabled(isEnabled);
        serviceComboBox.setEnabled(isEnabled);
    }

    void connected(FriendConnectionConfiguration config) {
        setSignInComponentsEnabled(true);
    }

    private void login(final FriendAccountConfiguration config) {
        setSignInComponentsEnabled(false);
        authFailedLabel.setVisible(false);
        validate();
        repaint();
        friendConnectionFactory.login(config);
    }

    @Override
    public void settingChanged(SettingEvent evt) {
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                populateInputs();                
            }
        });
    }

    class SignInAction extends AbstractAction {
        public SignInAction() {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            final FriendAccountConfiguration config = (FriendAccountConfiguration)serviceComboBox.getSelectedAction().getValue(CONFIG);
            if (config.getType() == Type.XMPP) {
                String user = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                if(user.equals("") || password.equals("")) {
                    return;
                }            
                if(config.getLabel().equals("Jabber")) {
                    String service = serviceField.getText().trim();
                    if(service.equals(""))
                        return;
                    config.setServiceName(service);
                }
                config.setUsername(user);
                config.setPassword(password);
                if(autoLoginCheckBox.isSelected()) {
                    // Set this as the auto-login account
                    accountManager.setAutoLoginConfig(config);
                } else {
                    // If there was previously an auto-login account, delete it
                    accountManager.setAutoLoginConfig(null);
                }
                login(config);
            } else {
                JFrame dialog = new JFrame();
                final Browser browser = new Browser() {
                    @Override
                    public void onAttachBrowser(ChromeAdapter chromeAdapter,
                            ChromeAdapter parentChromeAdapter) {
                        super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
                        nsIDOMEventTarget eventTarget = XPCOMUtils.qi(chromeAdapter.getWebBrowser().getContentDOMWindow(),
                                nsIDOMWindow2.class).getWindowRoot();
                        
                        LimeDomListener limeDomListener = new LimeDomListener();
                        limeDomListener.addTargetedUrlAction("", new UriAction() {
                            @Override
                            public boolean uriClicked(TargetedUri targetedUri) {
                                NativeLaunchUtils.openURL(targetedUri.getUri());
                                return true;
                            }
                        });
                        eventTarget.addEventListener("click", limeDomListener, true);
                        
                        eventTarget.addEventListener("load", new nsIDOMEventListener() {
                            @Override
                            public void handleEvent(nsIDOMEvent event) {
                                String url = getUrl();
                                if (url.contains("desktopapp.php")) {
                                    nsICookieService cookieService = XPCOMUtils.getServiceProxy("@mozilla.org/cookieService;1",
                                            nsICookieService.class);
                                    nsIIOService ioService = XPCOMUtils.getServiceProxy("@mozilla.org/network/io-service;1", nsIIOService.class);
                                    nsIURI uri = ioService.newURI(url, null, null);
                                    String cookie = cookieService.getCookieStringFromHttp(uri, null, null);
                                    uri = ioService.newURI("http://facebook.com/", null, null);
                                    cookie = cookieService.getCookieStringFromHttp(uri, null, null);
                                    config.setAttribute("url", "http://facebook.com/");
                                    config.setAttribute("cookie", cookie);
                                    friendConnectionFactory.login(config);
                                } else if (url.contains("login")) {
                                    String script = "(function() {var input = document.createElement('input');" +
                                    "input.type='hidden';" +
                                    "input.name='persistent';" +
                                    "input.value='1';" +
                                    "document.forms[0].appendChild(input);"; 
                                    jsexec(script);
                                }
                            }
                            @Override
                            public nsISupports queryInterface(String uuid) {
                                return null;
                            }
                        }, true);
                    }
                };
                dialog.getContentPane().add(browser);
                dialog.pack();
                dialog.setSize(800, 600);
                dialog.setVisible(true);
                friendConnectionFactory.requestLoginUrl(config).addFutureListener(new EventListener<FutureEvent<String>>() {
                    @Override
                    public void handleEvent(FutureEvent<String> event) {
                        switch (event.getType()) {
                        case SUCCESS:
                            browser.load(event.getResult());
                            break;
                        default:
                            throw new IllegalStateException(event.getType().toString());
                        }
                    }
                });
            }
        }
    }
    
    public void connecting(FriendConnectionConfiguration config) {
        setSignInComponentsEnabled(false);
    }
    
}
