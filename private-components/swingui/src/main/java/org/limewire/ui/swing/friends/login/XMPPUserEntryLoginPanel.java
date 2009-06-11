package org.limewire.ui.swing.friends.login;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class XMPPUserEntryLoginPanel extends JPanel {
    
    private final LoginPopupPanel parent;
    
    @Inject
    public XMPPUserEntryLoginPanel(@Assisted XMPPAccountConfiguration accountConfig, LoginPopupPanel parent) {
    
        this.parent = parent;
        
        add(new JLabel(I18n.tr("Sign in with {0}",accountConfig.getLabel()),
                accountConfig.getLargeIcon(), JLabel.HORIZONTAL));
    }
}
