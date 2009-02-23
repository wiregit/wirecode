package org.limewire.core.impl.mozilla;

import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsISupports;

import com.google.inject.Singleton;

/**
 * This class acts as a wrapper for the mozilla XPCOMUtils class. Because the
 * XPCOMUtils class has static methods it makes unit testing code that used it
 * very hard. By injecting this class instead, the code becomes testable.
 */
@Singleton
public class XPComUtility {

    public <T extends nsISupports> T proxy(nsISupports elem, Class<T> class1) {
        return XPCOMUtils.proxy(elem, class1);
    }

    public <T extends nsISupports> T getServiceProxy(String contractID, Class<T> class1) {
        return XPCOMUtils.getServiceProxy(contractID, class1);
    }

}
