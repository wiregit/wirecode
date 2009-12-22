package org.limewire.ui.swing.search.store;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.net.URISyntaxException;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.limewire.core.api.search.store.StoreDownloadToken;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.store.StoreDomListener.ClickAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsIDOMWindowInternal;
import org.w3c.dom.Node;

/**
 * Main container for the MozSwing browser used for the Lime Store.  
 * StoreBrowserPanel installs listeners to handle DOM events and update
 * the client application. 
 */
public class StoreBrowserPanel extends Browser {

    private final StoreController storeController;
    
    private StoreDomListener storeDomListener;
    
    private VisualStoreResult visualStoreResult;
    
    private TrackResult trackResult;
    
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
        // Create DOM listener.
        storeDomListener = new StoreDomListener(this);
        
        // Add listener for DOM click on hyperlink.
        storeDomListener.addTargetedUrlAction("", new ClickUriAction());
        
        // Add listener for DOM click on named element.
        storeDomListener.addClickListener("cancel", new CancelClickAction());
        storeDomListener.addClickListener("download", new DownloadClickAction());
        storeDomListener.addClickListener("downloadOk", new DownloadOkClickAction());
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
        eventTarget.addEventListener("load", storeDomListener, true);
        eventTarget.addEventListener("click", storeDomListener, true);
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
            eventTarget.removeEventListener("load", storeDomListener, true);
            eventTarget.removeEventListener("click", storeDomListener, true);
        }
        
        super.onDetachBrowser();
    }
    
    /**
     * Overrides superclass method to resize parent window when page is 
     * loaded successfully.
     */
    @Override
    public void pageLoadStopped(boolean failed) {
        // Call superclass method.
        super.pageLoadStopped(failed);
        
        if (!failed) {
            // Get DOM window size on success.  This may be set by the web 
            // page itself using JavaScript.
            nsIDOMWindow window = getChromeAdapter().getWebBrowser().getContentDOMWindow();
            nsIDOMWindowInternal internal = XPCOMUtils.qi(window, nsIDOMWindowInternal.class);
            final int width = internal.getOuterWidth();
            final int height = internal.getOuterHeight();
            
            // Resize parent window.
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    resizeDialog(width, height);
                }
            });
        }
    }
    
    /**
     * Displays the download approval dialog for the specified store result.
     */
    public void showDownload(StoreDownloadToken downloadToken, VisualStoreResult vsr) {
        // Save result.
        visualStoreResult = vsr;
        trackResult = null;
        
        showDownload(downloadToken);
    }
    
    /**
     * Displays the download approval dialog for the specified track result.
     */
    public void showDownload(StoreDownloadToken downloadToken, TrackResult trackResult) {
        // Save result.
        this.trackResult = trackResult;
        visualStoreResult = null;
        
        showDownload(downloadToken);
    }
    
    /**
     * Displays the download approval dialog using the specified download 
     * token.
     */
    private void showDownload(StoreDownloadToken downloadToken) {
        // Determine size and title.
        String title;
        switch (downloadToken.getStatus()) {
        case LOGIN_REQ:
            setPreferredSize(new Dimension(480, 480));
            title = I18n.tr("Log In");
            break;
        case CONFIRM_REQ:
            setPreferredSize(new Dimension(510, 192));
            title = I18n.tr("Confirm");
            break;
        default:
            throw new IllegalStateException("Unknown download status " + downloadToken.getStatus());
        }
        
        // Load URL into browser and display.
        load(downloadToken.getUrl());
        showDialog(title);
    }
    
    /**
     * Displays the File Info dialog for the specified store result.
     */
    public void showInfo(VisualStoreResult vsr) {
        // Save result.
        visualStoreResult = vsr;
        trackResult = null;
        
        // Load result info into browser.
        setPreferredSize(new Dimension(420, 540));
        load(storeController.getInfoURI(vsr));
        
        // Display dialog.
        showDialog(I18n.tr("{0} Properties", vsr.getFileName()));
    }
    
    /**
     * Displays the Login dialog for the Lime Store.
     */
    public void showLogin() throws URISyntaxException {
        // Load login page into browser.
        setPreferredSize(new Dimension(860, 600));
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
     * Resizes the parent window to the specified width and height.
     */
    private void resizeDialog(int width, int height) {
        Container ancestor = getTopLevelAncestor();
        if (ancestor instanceof Window) {
            setPreferredSize(new Dimension(width, height));
            ((Window) ancestor).pack();
            ((Window) ancestor).setLocationRelativeTo(GuiUtils.getMainFrame());
        }
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
    private static class BrowserDialog extends LimeJDialog {
        
        public BrowserDialog(Frame owner, MozillaPanel mozillaPanel) {
            super(owner);
            
            // Set dialog content to browser.
            setLayout(new BorderLayout());
            add(mozillaPanel, BorderLayout.CENTER);
        }
    }
    
    /**
     * Action to process click event on hyperlink.
     */
    private class ClickUriAction implements UriAction {

        @Override
        public boolean uriClicked(TargetedUri targetedUri) {
            // TODO REMOVE
            System.out.println("uriClicked: EDT=" + SwingUtilities.isEventDispatchThread() + 
                    ", target=" + targetedUri.getTarget() + ", uri=" + targetedUri.getUri());
            
            // Get URI text.
            String uri = targetedUri.getUri();
            
            if (uri.toLowerCase().contains("#cancel")) {
                // Close dialog if cancelled.
                SwingUtils.invokeNowOrLater(new Runnable() {
                    @Override
                    public void run() {
                        disposeDialog();
                    }
                });
                
            } else {
                // Forward to native browser by default.
                NativeLaunchUtils.openURL(targetedUri.getUri());
            }
            
            // Return true to prevent further processing.
            return true;
        }
    }
    
    /**
     * Action to process click event on cancel button.
     */
    private class CancelClickAction implements ClickAction {

        @Override
        public void nodeClicked(Node node) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    // Close current dialog.
                    disposeDialog();
                }
            });
        }
    }
    
    /**
     * Action to process click event on "download" button.
     */
    private class DownloadClickAction implements ClickAction {

        @Override
        public void nodeClicked(Node node) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    // Close current dialog.
                    disposeDialog();
                    
                    // Start download process.
                    if (visualStoreResult != null) {
                        storeController.download(visualStoreResult);
                    } else if (trackResult != null) {
                        storeController.downloadTrack(trackResult);
                    }
                }
            });
        }
    }
    
    /**
     * Action to process click event on "download approved" button.
     */
    private class DownloadOkClickAction implements ClickAction {

        @Override
        public void nodeClicked(Node node) {
            SwingUtils.invokeNowOrLater(new Runnable() {
                @Override
                public void run() {
                    // Close current dialog.
                    disposeDialog();
                    
                    // Start download process.
                    if (visualStoreResult != null) {
                        storeController.doDownload(visualStoreResult);
                    } else if (trackResult != null) {
                        storeController.doDownloadTrack(trackResult);
                    }
                }
            });
        }
    }
}
