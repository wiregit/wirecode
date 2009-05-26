package org.limewire.facebook.service;

import org.json.JSONObject;
import org.limewire.core.api.friend.Network;

public interface FacebookFriendFactory {
    FacebookFriend create(String presenceId, JSONObject friendInfo, Network network, boolean hasLimewireAppInstalled);
}
