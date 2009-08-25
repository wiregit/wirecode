package org.limewire.ui.swing.search.store;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Dialog.ModalityType;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsICookie;
import org.mozilla.interfaces.nsICookieManager;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;

/**
 * Main container for the Login dialog for the Lime Store.
 */
public class StoreLoginPanel extends JPanel {

    private final StoreController storeController;
    
    private MozillaPanel mozillaPanel;
    
    /**
     * Constructs a StoreLoginPanel using the specified services.
     */
    public StoreLoginPanel(StoreController storeController) {
        this.storeController = storeController;
        
        initComponents();
        
        // TODO add listeners to handle action links like login success
        
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        
        mozillaPanel = new LoginBrowser();
        mozillaPanel.setPreferredSize(new Dimension(480, 480));
        
        add(mozillaPanel, BorderLayout.CENTER);
    }
    
    /**
     * Displays the Login dialog for the Lime Store.
     */
    public void display() {
        // Load login page into browser.
        mozillaPanel.load(storeController.getLoginURI());
        
        // Get main frame.
        Frame owner = GuiUtils.getMainFrame();
        
        // Create dialog.
        JDialog dialog = new LimeJDialog(owner);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setTitle(I18n.tr("Log In"));

        // Set content.
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        
        // Position dialog and display.
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
    
    /**
     * An extension of the MozSwing browser used for store login.  LoginBrowser
     * saves the user cookie on successful login. 
     */
    private class LoginBrowser extends Browser {
        
        public LoginBrowser() {
            super(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN,
                    VisibilityMode.DEFAULT);            
        }
        
        /**
         * Overrides superclass method to install event listener to retrieve
         * cookie.
         */
        @Override
        public void onAttachBrowser(ChromeAdapter chromeAdapter,
                ChromeAdapter parentChromeAdapter) {
            super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
            
            // Get DOM event target.
            nsIDOMEventTarget eventTarget = XPCOMUtils.qi(
                    chromeAdapter.getWebBrowser().getContentDOMWindow(),
                    nsIDOMWindow2.class).getWindowRoot();
            
            // Add load event listener.
            eventTarget.addEventListener("load", new LoadDOMListener(), true);
        }
        
        private class LoadDOMListener implements nsIDOMEventListener {

            @Override
            public void handleEvent(nsIDOMEvent event) {
                String url = getUrl();
                System.out.println("handleEvent-load: url=" + url); // TODO REMOVE
                if (url.toLowerCase().contains("home")) {
                    // Get cookie service.
                    nsICookieManager cookieService = XPCOMUtils.getServiceProxy(
                            "@mozilla.org/cookiemanager;1", nsICookieManager.class);
                    
                    // Inspect all cookies.
                    nsISimpleEnumerator enumerator = cookieService.getEnumerator();
                    int count = 0;
                    while (enumerator.hasMoreElements()) {
                        nsICookie cookie = XPCOMUtils.proxy(enumerator.getNext(), nsICookie.class);
                        System.out.println("cookie name=" + cookie.getName() + ", value=" + cookie.getValue() + ", host=" + cookie.getHost());
                        count++;
                    }
                    System.out.println("cookie count = " + count);
                }
                
                // TODO implement to save cookie in store controller/manager?
            }

            @Override
            public nsISupports queryInterface(String uuid) {
                return Mozilla.queryInterface(this, uuid);
            }
        }
    }
}
