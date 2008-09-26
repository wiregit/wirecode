package org.limewire.core.impl.library;

import org.limewire.core.api.library.Buddy;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.User;

public class UserBuddy implements Buddy {
    
    private final User user;
    
    UserBuddy(User user) {
        this.user = user;
    }
    
    @Override
    public String getId() {
        return user.getId();
    }
    
    @Override
    public String getName() {
        return user.getName();
    }
    
    @Override
    public String toString() {
        return StringUtils.toString(this);
    }

}
