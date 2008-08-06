package org.limewire.ui.swing.browser;

import org.limewire.ui.swing.util.SwingUtils;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.impl.ChromeAdapter;


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
            }
        });
    }
  
}
   

