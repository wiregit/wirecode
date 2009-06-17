package org.limewire.ui.swing.friends.actions;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.AddFriendDialog;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;

public class AddFriendAction extends AbstractAction {

    private final XMPPService xmppService;

    @Inject
    public AddFriendAction(XMPPService xmppService) {
        super(I18n.tr("Add Friend..."));
        this.xmppService = xmppService;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        new AddFriendDialog(xmppService.getActiveConnection());
    }
}
