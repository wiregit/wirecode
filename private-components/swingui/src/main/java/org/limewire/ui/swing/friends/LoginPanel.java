package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;

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
import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.api.client.XMPPConnection;
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
 * TODO: Use xmpp API
 */
@Singleton
public class LoginPanel extends JPanel {
    @Resource private Icon gmail;
    @Resource private Icon facebook;

    private JToggleButton googleTalkButton;
    private JToggleButton facebookButton;
    private JPasswordField passwordField;
    private JCheckBox rememberMeCheckbox;
    private JButton signInButton;
    private JTextField userNameField;
    private JPanel topPanel;
    private final XMPPService xmppService;

    @Inject
    public LoginPanel(XMPPService xmppService) {
        this.xmppService = xmppService;
        GuiUtils.assignResources(this);

        initComponents();
    }

    private void initComponents() {
        userNameField = new JTextField(18);
        passwordField = new JPasswordField(18);
        rememberMeCheckbox = new JCheckBox(tr("Remember me"));
        signInButton = new JButton(new SignInAction());

        FormLayout layout = new FormLayout("7dlu, p, 7dlu", "7dlu, p, 10dlu, p, 7dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        
        topPanel = new JPanel();
        topPanel.add(topPanel());
        builder.add(topPanel, cc.xy(2, 2));
        builder.add(getDetailsPanel(), cc.xy(2, 4));
        add(builder.getPanel());
    }
    
    private JPanel topPanel() {
        FormLayout layout = new FormLayout("p, 4dlu, p:g", "p, 3dlu, p, p, p");
        PanelBuilder builder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        builder.addLabel(tr("Have a Gmail or Facebook account?"), cc.xyw(1, 1, 3));
        builder.addLabel(tr("- Access your friends' libraries"), cc.xy(3, 3));
        builder.addLabel(tr("- See what new files they have"), cc.xy(3, 4));
        builder.addLabel(tr("- Chat with your friends"), cc.xy(3, 5));
        return builder.getPanel();
    }
    
    private void loginFailed() {
        topPanel.removeAll();
        topPanel.add(noConnectionAvailablePanel());
    }
    
    private JPanel noConnectionAvailablePanel() {
        FormLayout layout = new FormLayout("c:p:g", "7dlu,p, p, 7dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.nextLine();
        JLabel label = builder.addLabel(tr("Could not log you in"));
        //A pretty crimson
        label.setForeground(new Color(112, 13, 37));
        FontUtils.changeSize(label, 2.0f);
        builder.nextLine();
        builder.addLabel(tr("Please try again."));
        return builder.getPanel();
    }
    
    private JPanel getDetailsPanel() {
        
        ButtonGroup networkGroup = new ButtonGroup();
        googleTalkButton = new JToggleButton(tr("Gmail"), gmail);
        networkGroup.add(googleTalkButton);
        facebookButton = new JToggleButton(tr("Facebook"), facebook);
        networkGroup.add(facebookButton);
        
        FormLayout layout = new FormLayout("l:p, 2dlu, p", "p");
        PanelBuilder networksBuilder = new PanelBuilder(layout);
        CellConstraints cc = new CellConstraints();
        networksBuilder.add(googleTalkButton, cc.xy(1, 1));
        networksBuilder.add(facebookButton, cc.xy(3, 1));
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
    
    class SignInAction extends AbstractAction {
        public SignInAction() {
            super(tr("Sign in"));
        }
        
        public void actionPerformed(ActionEvent e) {
            ThreadExecutor.startThread(new Runnable() {
                public void run() {
                    synchronized (LoginPanel.this) {
                        List<XMPPConnection> connections = xmppService.getConnections();
                        for(XMPPConnection connection : connections) {
                            if(connection.getConfiguration().getServiceName().equals("gmail.com")) {
                                if(!connection.isLoggedIn()) {
                                    connection.getConfiguration().setUsername(userNameField.getText());
                                    connection.getConfiguration().setPassword(new String(passwordField.getPassword()));
                                    connection.getConfiguration().setAutoLogin(rememberMeCheckbox.isSelected());
                                    try {
                                        connection.login();
                                        
                                        new XMPPConnectionEstablishedEvent().publish();
                                        
                                    } catch (XMPPException e1) {
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                loginFailed();
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    }
                }
            }, "xmpp-login");            
        }
    }
}