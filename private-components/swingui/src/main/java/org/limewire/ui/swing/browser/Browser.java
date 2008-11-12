package org.limewire.ui.swing.browser;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.limewire.ui.swing.util.SwingUtils;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsIBaseWindow;
import org.mozilla.interfaces.nsIRequest;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIURI;
import org.mozilla.interfaces.nsIWebProgress;
import org.mozilla.interfaces.nsIWebProgressListener;
import org.mozilla.xpcom.Mozilla;

/**
 * Extension to Mozilla's browser that adds the correct listeners.
 */
public class Browser extends MozillaPanel {
    
    private final Listener listener = new Listener();
    
    private volatile boolean lastRequestFailed = true;

    public Browser() {
        super();
    }

    public Browser(boolean attachNewBrowserOnCreation, VisibilityMode toolbarVisMode,
            VisibilityMode statusbarVisMode) {
        super(null, attachNewBrowserOnCreation, toolbarVisMode, statusbarVisMode);
    }

    public Browser(VisibilityMode toolbarVisMode, VisibilityMode statusbarVisMode) {
        super(toolbarVisMode, statusbarVisMode);
    }
    
    /** Returns true if the last request is currently in progress or succeeded. */
    public boolean isLastRequestSuccess() {
        return !lastRequestFailed;
    }

    @Override
    public void onSetTitle(String title) {
        // Don't set it!
    }

    // overridden to remove LimeDomListener
    @Override
    public void onDetachBrowser() {
        if (getChromeAdapter() != null) {
            BrowserUtils.removeDomListener(getChromeAdapter());
            getChromeAdapter().getWebBrowser().removeWebBrowserListener(listener, nsIWebProgressListener.NS_IWEBPROGRESSLISTENER_IID);
        }
        super.onDetachBrowser();
    }

    // overridden for browser initialization that can not be done earlier
    @Override
    public void onAttachBrowser(final ChromeAdapter chromeAdapter, ChromeAdapter parentChromeAdapter) {
        super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
        BrowserUtils.addDomListener(chromeAdapter);
        chromeAdapter.getWebBrowser().addWebBrowserListener(listener, nsIWebProgressListener.NS_IWEBPROGRESSLISTENER_IID);
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                addKeyListener(new MozillaKeyListener(chromeAdapter));
                addComponentListener(new VisibilityListener());
            }
        });
    }

    private class VisibilityListener extends ComponentAdapter {
        @Override
        public void componentHidden(ComponentEvent e) {
            setBrowserVisibility(false);
        }

        @Override
        public void componentShown(ComponentEvent e) {
            setBrowserVisibility(true);
        }

    }

    private void setBrowserVisibility(final boolean isVisible) {
        // Mozilla is not threadsafe so most mozilla methods must be called on
        // the
        // mozilla thread. This is asynchronous so as not to tie up the EDT.
        MozillaExecutor.mozAsyncExec(new Runnable() {
            public void run() {
                ChromeAdapter chromeAdapter = getChromeAdapter();
                if (chromeAdapter != null) {
                    nsIBaseWindow baseWindow = XPCOMUtils.qi(chromeAdapter.getWebBrowser(),
                            nsIBaseWindow.class);
                    baseWindow.setVisibility(isVisible);
                }
            }
        });

    }

    private class Listener implements nsIWebProgressListener {
        
        public void started() {
            lastRequestFailed = false;
        }

        public void stopped(nsIRequest aRequest,
                            long aStatus) {
            lastRequestFailed = aStatus != 0;
        }
        
        @Override
        public void onLocationChange(nsIWebProgress webProgress, nsIRequest request, nsIURI location) {
        }

        @Override
        public void onProgressChange(nsIWebProgress webProgress, nsIRequest request,
                int curSelfProgress, int maxSelfProgress, int curTotalProgress, int maxTotalProgress) {

        }

        @Override
        public void onSecurityChange(nsIWebProgress webProgress, nsIRequest request, long state) {
        }

        @Override
        public void onStateChange(nsIWebProgress webProgress, nsIRequest request, long stateFlags,
                long status) {
            if ((stateFlags & nsIWebProgressListener.STATE_IS_NETWORK) != 0
                    && (stateFlags & nsIWebProgressListener.STATE_START) != 0) {
                started();
            }

            if ((stateFlags & nsIWebProgressListener.STATE_IS_NETWORK) != 0
                    && (stateFlags & nsIWebProgressListener.STATE_STOP) != 0) {
                stopped(request, status);
            }
        }

        @Override
        public void onStatusChange(nsIWebProgress webProgress, nsIRequest request, long status,
                String message) {
            
        }

        @Override
        public nsISupports queryInterface(String iid) {
            return Mozilla.queryInterface(this, iid);
        }
    }

}
