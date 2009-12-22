package org.limewire.promotion.search;

import org.limewire.listener.EventBroadcaster;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.IXPCOMError;
import org.mozilla.xpcom.XPCOMException;

public class StoreAuthStateImpl implements StoreAuthState {
    
    /** UUID for the component implementation.  This MUST be different than
     *  the interface IID.  
     */
    public static String CID = "{ea64c524-349f-462d-a0f4-aa5960e31051}";
    
    /** Unique identifier for the implementation. */
    public static String CONTRACT_ID = "@org.limewire/StoreAuthState;1";
    private final EventBroadcaster<org.limewire.core.api.search.store.StoreAuthState> broadcaster;

    public StoreAuthStateImpl(EventBroadcaster<org.limewire.core.api.search.store.StoreAuthState> broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void updateAuthState(boolean isLoggedIn) {
        broadcaster.broadcast(new org.limewire.core.api.search.store.StoreAuthState(isLoggedIn));
    }

    @Override
    public nsISupports queryInterface(String uuid) {
        if (!uuid.equals(NS_ISUPPORTS_IID) && (!uuid.equals(STOREAUTHSTATE_IID))) {
            throw new XPCOMException(IXPCOMError.NS_ERROR_NOT_IMPLEMENTED);
        }
        return this;
    }
}
