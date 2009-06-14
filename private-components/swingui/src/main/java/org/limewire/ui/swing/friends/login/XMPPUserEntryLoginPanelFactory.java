package org.limewire.ui.swing.friends.login;

import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;

public interface XMPPUserEntryLoginPanelFactory {
    public XMPPUserEntryLoginPanel create(XMPPAccountConfiguration accountConfig);
}
