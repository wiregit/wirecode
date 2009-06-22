package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Provider;

class LogoutAction extends AbstractAction {

    private final Provider<XMPPService> xmppService;
    
    @Inject
    public LogoutAction(Provider<XMPPService> xmppService) {
        super(I18n.tr("Logout"));
        
        this.xmppService = xmppService;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        //logout of service
        xmppService.get().logout();
    }
}
