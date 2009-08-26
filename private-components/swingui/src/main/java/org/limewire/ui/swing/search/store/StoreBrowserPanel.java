package org.limewire.ui.swing.search.store;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Dialog.ModalityType;

import javax.swing.JDialog;

import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
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
 * Main container for the MozSwing browser used for the Lime Store.  
 * StoreBrowserPanel installs listeners to handle DOM events and update
 * the client application. 
 */
public class StoreBrowserPanel extends Browser {

    private final StoreController storeController;
    
    /**
     * Constructs a StoreBrowserPanel using the specified services.
     */
    public StoreBrowserPanel(StoreController storeController) {
        super(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN,
                VisibilityMode.DEFAULT);
        
        this.storeController = storeController;
        
        initComponents();
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        // TODO implement or remove
    }

    /**
     * Overrides superclass method to install listeners for DOM events.
     */
    @Override
    public void onAttachBrowser(ChromeAdapter chromeAdapter,
            ChromeAdapter parentChromeAdapter) {
        super.onAttachBrowser(chromeAdapter, parentChromeAdapter);

        // Get DOM event target.
        nsIDOMEventTarget eventTarget = XPCOMUtils.qi(
                chromeAdapter.getWebBrowser().getContentDOMWindow(),
                nsIDOMWindow2.class).getWindowRoot();

        // Add DOM listener for load events.  We do this to capture cookies
        // when a page is loaded.
        eventTarget.addEventListener("load", new LoadDOMListener(), true);
    }
    
    /**
     * Displays the store download dialog for the specified result.
     */
    public void showDownload(StoreResult storeResult) {
        // Load login page into browser.
        setPreferredSize(new Dimension(640, 480));
        load(storeController.getLoginURI());
        
        // Display dialog.
        showDialog(I18n.tr("Download"));
    }
    
    /**
     * Displays the store download dialog for the specified result.
     */
    public void showDownload(StoreTrackResult trackResult) {
        // Load login page into browser.
        setPreferredSize(new Dimension(640, 480));
        load(storeController.getLoginURI());
        
        // Display dialog.
        showDialog(I18n.tr("Download"));
    }
    
    /**
     * Displays the File Info dialog for the specified store result.
     */
    public void showInfo(VisualStoreResult vsr) {
        // Load result info into browser.
        setPreferredSize(new Dimension(420, 540));
        load(storeController.getInfoURI(vsr));
        
        // Display dialog.
        showDialog(I18n.tr("{0} Properties", vsr.getFileName()));
    }
    
    /**
     * Displays the Login dialog for the Lime Store.
     */
    public void showLogin() {
        // Load login page into browser.
        setPreferredSize(new Dimension(480, 480));
        load(storeController.getLoginURI());
        
        // Display dialog.
        showDialog(I18n.tr("Log In"));
    }
    
    /**
     * Displays this container in a model dialog with the specified title.
     */
    private void showDialog(String title) {
        // Get main frame.
        Frame owner = GuiUtils.getMainFrame();
        
        // Create dialog.
        JDialog dialog = new LimeJDialog(owner);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setTitle(title);

        // Set content.
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        
        // Position dialog and display.
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /**
     * Listener to handle load events on the DOM.
     */
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
                    //System.out.println("cookie name=" + cookie.getName() + ", value=" + cookie.getValue() + ", host=" + cookie.getHost());
                    count++;
                }
                //System.out.println("cookie count = " + count);
            }

            // TODO implement to save cookie in store controller/manager?
        }

        @Override
        public nsISupports queryInterface(String uuid) {
            return Mozilla.queryInterface(this, uuid);
        }
    }
}
