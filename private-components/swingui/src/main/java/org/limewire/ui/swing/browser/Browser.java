package org.limewire.ui.swing.browser;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import org.limewire.ui.swing.util.SwingUtils;
import org.mozilla.browser.MozillaExecutor;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsIBaseWindow;


/**
 * Extension to Mozilla's browser that adds the correct listeners.
 */
public class Browser extends MozillaPanel {
    
    public Browser() {
        super();
    }

    public Browser(boolean attachNewBrowserOnCreation, VisibilityMode toolbarVisMode,
            VisibilityMode statusbarVisMode) {
        super(attachNewBrowserOnCreation, toolbarVisMode, statusbarVisMode);
    }

    public Browser(VisibilityMode toolbarVisMode, VisibilityMode statusbarVisMode) {
        super(toolbarVisMode, statusbarVisMode);
    }
    
    @Override
    public void onSetTitle(String title) {
        // Don't set it!
    }
    
  
    //overridden to remove LimeDomListener
    @Override
    public void onDetachBrowser() {
        if(getChromeAdapter() != null) {
            BrowserUtils.removeDomListener(getChromeAdapter());
        }
        super.onDetachBrowser();
    }
    
    //overridden for browser initialization that can not be done earlier
    @Override
    public void onAttachBrowser(final ChromeAdapter chromeAdapter, ChromeAdapter parentChromeAdapter){
        super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
        BrowserUtils.addDomListener(chromeAdapter);
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
        // Mozilla is not threadsafe so most mozilla methods must be called on the
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
  
}
   

