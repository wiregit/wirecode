package org.limewire.promotion.search;

import org.limewire.core.api.file.CategoryManager;
import org.mozilla.interfaces.nsIFactory;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.IXPCOMError;
import org.mozilla.xpcom.XPCOMException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadServices;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;

@Singleton
public class XPComDownloadFactory implements nsISupports, nsIFactory {
    private final CategoryManager categoryManager;
    private final RemoteFileDescFactory remoteFileDescFactory;
    private final DownloadServices downloadServices;

    @Inject
    public XPComDownloadFactory(CategoryManager categoryManager, RemoteFileDescFactory remoteFileDescFactory, DownloadServices downloadServices) {
        this.categoryManager = categoryManager;
        this.remoteFileDescFactory = remoteFileDescFactory;
        this.downloadServices = downloadServices;
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
        return new XPComDownloadImpl(categoryManager, remoteFileDescFactory, downloadServices);
    }

    @Override
    public void lockFactory(boolean lock) {
        // Do nothing.
    }
}
