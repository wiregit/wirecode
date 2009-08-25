package org.limewire.facebook.service;

import org.limewire.ui.swing.friends.settings.FacebookFriendAccountConfiguration;

public interface FacebookFriendConnectionFactory {
    FacebookFriendConnection create(FacebookFriendAccountConfiguration configuration);    
}
