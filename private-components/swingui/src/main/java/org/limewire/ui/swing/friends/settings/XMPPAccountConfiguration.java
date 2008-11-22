package org.limewire.ui.swing.friends.settings;

import javax.swing.ImageIcon;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;

/**
 * Extends the XMPPConnectionConfiguration interface with methods for
 * describing and configuring an XMPP account through the UI.
 */
public interface XMPPAccountConfiguration extends XMPPConnectionConfiguration {
    
    /**
     * Returns a URL where users can register new accounts with the service
     * provider.
     */
    public String getRegistrationURL();
    
    /**
     * Returns an icon associated with the account, such as the logo of the
     * service provider.
     */
    public ImageIcon getIcon();
    
    /**
     * Sets the username of the account.
     */
    public void setUsername(String username);
    
    /**
     * Sets the password of the account.
     */
    public void setPassword(String password);
}
