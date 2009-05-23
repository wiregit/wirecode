package org.limewire.facebook.service.livemessage;

import org.limewire.facebook.service.FacebookFriendConnection;

public interface DiscoInfoHandlerFactory {
    DiscoInfoHandler create(FacebookFriendConnection connection);
}
