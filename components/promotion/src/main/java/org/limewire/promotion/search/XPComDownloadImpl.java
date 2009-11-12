package org.limewire.promotion.search;

import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.IXPCOMError;
import org.mozilla.xpcom.XPCOMException;

public class XPComDownloadImpl implements XPComDownload {
    
    /** UUID for the component implementation.  This MUST be different than
     *  the interface IID.  
     */
    public static String CID = "{f083e1d0-62cf-45a2-93f5-926ba02aad73}";
    
    /** Unique identifier for the implementation. */
    public static String CONTRACT_ID = "@org.limewire/XPComDownload;1";
    
    @Override
    public void download(String fileName, String url, long size) {
        // TODO
    }

    @Override
    public nsISupports queryInterface(String uuid) {
        if (!uuid.equals(NS_ISUPPORTS_IID) && (!uuid.equals(XPCOMDOWNLOAD_IID))) {
            throw new XPCOMException(IXPCOMError.NS_ERROR_NOT_IMPLEMENTED);
        }
        return this;
    }
}
