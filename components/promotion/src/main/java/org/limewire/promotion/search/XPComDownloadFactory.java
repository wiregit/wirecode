package org.limewire.promotion.search;

import org.mozilla.interfaces.nsIFactory;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.IXPCOMError;
import org.mozilla.xpcom.XPCOMException;

public class XPComDownloadFactory implements nsISupports, nsIFactory {
    private static XPComDownloadFactory instance = new XPComDownloadFactory();
    
    private XPComDownloadFactory() {
    }
    
    /**
     * Returns the singleton instance of XPComDownloadFactory.
     */
    public static XPComDownloadFactory getInstance() {
        return instance;
    }
    
    @Override
    public nsISupports queryInterface(String uuid) {
        if (!uuid.equals(NS_IFACTORY_IID) && (!uuid.equals(NS_ISUPPORTS_IID))) {
            throw new XPCOMException(IXPCOMError.NS_ERROR_NOT_IMPLEMENTED);
        }
        return this;
    }

    @Override
    public nsISupports createInstance(nsISupports aOuter, String iid) {
        if (aOuter != null) {
            throw new XPCOMException(IXPCOMError.NS_ERROR_NO_AGGREGATION);
        }
        if (!iid.equals(XPComDownload.XPCOMDOWNLOAD_IID) && !iid.equals(nsISupports.NS_ISUPPORTS_IID)) {
            throw new XPCOMException(IXPCOMError.NS_ERROR_INVALID_ARG);
        }
        return new XPComDownloadImpl();
    }

    @Override
    public void lockFactory(boolean lock) {
        // Do nothing.
    }
}
