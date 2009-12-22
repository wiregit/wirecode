package org.limewire.promotion.search;

import org.limewire.listener.EventBroadcaster;
import org.mozilla.interfaces.nsIFactory;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.IXPCOMError;
import org.mozilla.xpcom.XPCOMException;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StoreAuthStateFactory implements nsISupports, nsIFactory {
    
    private final EventBroadcaster<org.limewire.core.api.search.store.StoreAuthState> broadcaster;

    @Inject
    public StoreAuthStateFactory(EventBroadcaster<org.limewire.core.api.search.store.StoreAuthState> broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public nsISupports queryInterface(String uuid) {
        if (!uuid.equals(NS_IFACTORY_IID) && !uuid.equals(NS_ISUPPORTS_IID)) {
            throw new XPCOMException(IXPCOMError.NS_ERROR_NOT_IMPLEMENTED);
        }
        return this;
    }

    @Override
    public nsISupports createInstance(nsISupports aOuter, String iid) {
        if (aOuter != null) {
            throw new XPCOMException(IXPCOMError.NS_ERROR_NO_AGGREGATION);
        }
        if (!iid.equals(StoreAuthState.STOREAUTHSTATE_IID) && !iid.equals(nsISupports.NS_ISUPPORTS_IID)) {
            throw new XPCOMException(IXPCOMError.NS_ERROR_INVALID_ARG);
        }
        return new StoreAuthStateImpl(broadcaster);
    }

    @Override
    public void lockFactory(boolean lock) {
        // Do nothing.
    }
}
