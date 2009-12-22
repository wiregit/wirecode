package org.limewire.core.api.search.store;

import org.limewire.listener.DefaultDataEvent;

public class StoreAuthState extends DefaultDataEvent<Boolean> {

    public StoreAuthState(Boolean data) {
        super(data);
    }
    
    public boolean isLoggedIn() {
        return getData();
    }
}
