package org.limewire.ui.swing.search.store;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Dialog.ModalityType;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.LimeDomListener;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.mozilla.browser.IMozillaWindow;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsICookie;
import org.mozilla.interfaces.nsICookieManager;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;
import org.w3c.dom.Document;

/**
 * Main container for the MozSwing browser used for the Lime Store.  
 * StoreBrowserPanel installs listeners to handle DOM events and update
 * the client application. 
 */
public class StoreBrowserPanel extends Browser {

    private final StoreController storeController;
    
    private LimeDomListener clickDomListener;
    private LoadDOMListener loadDomListener;
    
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
        // Create DOM listener for click events.
        clickDomListener = new LimeDomListener();
        
        // Create DOM listener for load events.
        loadDomListener = new LoadDOMListener();
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

        // Add DOM listeners.
        eventTarget.addEventListener("load", loadDomListener, true);
        eventTarget.addEventListener("click", clickDomListener, true);
    }
    
    /**
     * Overrides superclass method to remove listeners for DOM events.
     */
    @Override
    public void onDetachBrowser() {
        if (getChromeAdapter() != null) {
            // Get DOM event target.
            nsIDOMEventTarget eventTarget = XPCOMUtils.qi(
                    getChromeAdapter().getWebBrowser().getContentDOMWindow(),
                    nsIDOMWindow2.class).getWindowRoot();
            
            // Remove DOM listeners.
            eventTarget.removeEventListener("load", loadDomListener, true);
            eventTarget.removeEventListener("click", clickDomListener, true);
            
            // TODO remove actions on click listener?
        }
        
        super.onDetachBrowser();
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
        // Add actions to click listener.
        clickDomListener.addTargetedUrlAction("", new ClickUriAction());
        
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
     * Displays this container in a modal dialog with the specified title.
     */
    private void showDialog(String title) {
        // Get main frame.
        Frame owner = GuiUtils.getMainFrame();
        
        // Create dialog.
        BrowserDialog dialog = new BrowserDialog(owner, this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setTitle(title);

        // Position dialog and display.
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
    
    /**
     * Disposes of the dialog. 
     */
    private void disposeDialog() {
        Container ancestor = getTopLevelAncestor();
        if (ancestor instanceof BrowserDialog) {
            ((BrowserDialog) ancestor).dispose();
        }
    }

    /**
     * Dialog to display store browser.
     */
    private static class BrowserDialog extends LimeJDialog implements IMozillaWindow {
        private final MozillaPanel mozillaPanel;
        
        public BrowserDialog(Frame owner, MozillaPanel mozillaPanel) {
            super(owner);
            
            this.mozillaPanel = mozillaPanel;
            
            // Set dialog content to browser.
            setLayout(new BorderLayout());
            add(mozillaPanel, BorderLayout.CENTER);
            mozillaPanel.setContainerWindow(this);
        }

        @Override
        public MozillaPanel getPanel() {
            return mozillaPanel;
        }
    }
    
    /**
     * Listener to handle load events on the DOM.
     */
    private class LoadDOMListener implements nsIDOMEventListener {

        @Override
        public void handleEvent(nsIDOMEvent event) {
            String url = getUrl();
            System.out.println("load event: type=" + event.getType() + ", url=" + url); // TODO REMOVE
            
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
                    // TODO implement to save cookie in store controller/manager?
                    count++;
                }
                //System.out.println("cookie count = " + count);
            }
            
            nsIDOMWindow window = getChromeAdapter().getWebBrowser().getContentDOMWindow();
            Document document = getDocument();
            
            // TODO maybe get document attributes to set browser size?
            // Does not work.
            //String script = "resizeWindow();";
            //jsexec(script);
            // This works.
            //getChromeAdapter().sizeBrowserTo(420, 540);
        }

        @Override
        public nsISupports queryInterface(String uuid) {
            return Mozilla.queryInterface(this, uuid);
        }
    }
    
    /**
     * Action to process click events on the DOM.
     */
    private class ClickUriAction implements UriAction {

        @Override
        public boolean uriClicked(TargetedUri targetedUri) {
            System.out.println("uriClicked: EDT=" + SwingUtilities.isEventDispatchThread() + 
                    ", target=" + targetedUri.getTarget() + ", uri=" + targetedUri.getUri());
            
            // TODO implement for real
            
            String uri = targetedUri.getUri();
            if (uri.toLowerCase().contains("#more")) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        disposeDialog();
                    }
                });
            }
            
            // Return true to prevent further processing.
            return true;
        }
    }
}
