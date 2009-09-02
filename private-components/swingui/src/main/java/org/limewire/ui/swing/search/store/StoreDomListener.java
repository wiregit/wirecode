package org.limewire.ui.swing.search.store;

import org.mozilla.browser.XPCOMUtils;
import org.mozilla.interfaces.nsICookie;
import org.mozilla.interfaces.nsICookieManager;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsIDOMWindow;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;
import org.w3c.dom.Document;

/**
 * Listener to handle DOM events on the MozSwing browser.
 */
public class StoreDomListener implements nsIDOMEventListener {

    private final StoreBrowserPanel browserPanel;
    
    /**
     * Constructs a StoreDomListener for the specified browser panel.
     */
    public StoreDomListener(StoreBrowserPanel browserPanel) {
        this.browserPanel = browserPanel;
    }
    
    @Override
    public void handleEvent(nsIDOMEvent event) {
        // Get event type.
        String type = event.getType();
        
        // Process event type.
        if ("click".equals(type)) {
            // TODO handle click events
            
        } else if ("load".equals(type)) {
            String url = browserPanel.getUrl();
            System.out.println("load event: type=" + event.getType() + ", url=" + url); // TODO REMOVE
            
            // TODO handle load events - for successful login, we retrieve
            // the cookies and save them
            
            if (url.toLowerCase().contains("home")) {
                // Get cookie service.
                nsICookieManager cookieService = XPCOMUtils.getServiceProxy(
                        "@mozilla.org/cookiemanager;1", nsICookieManager.class);

                // Inspect all cookies.
                nsISimpleEnumerator enumerator = cookieService.getEnumerator();
                int count = 0;
                while (enumerator.hasMoreElements()) {
                    nsICookie cookie = XPCOMUtils.proxy(enumerator.getNext(), nsICookie.class);
//                    System.out.println("cookie name=" + cookie.getName() + 
//                            ", value=" + cookie.getValue() + ", host=" + cookie.getHost());
                    // TODO implement to save cookie in store controller/manager?
                    count++;
                }
                //System.out.println("cookie count = " + count);
            }
            
            nsIDOMWindow window = browserPanel.getChromeAdapter().getWebBrowser().getContentDOMWindow();
            Document document = browserPanel.getDocument();
            
        } else if ("submit".equals(type)) {
            // TODO handle submit events
            
        }
    }

    @Override
    public nsISupports queryInterface(String uuid) {
        return Mozilla.queryInterface(this, uuid);
    }
}
