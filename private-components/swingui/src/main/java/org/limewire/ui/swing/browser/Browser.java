package org.limewire.ui.swing.browser;

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

    private void initialize() {
        BrowserUtils.addDomListener(getChromeAdapter());
        addKeyListener(new MozillaKeyListener(getChromeAdapter()));
    }
    
    @Override
    public void onSetTitle(String title) {
        // Don't set it!
    }
    
  
    //overridden to remove LimeDomListener
    @Override
    public void onDetachBrowser() {
        BrowserUtils.removeDomListener(getChromeAdapter());
        super.onDetachBrowser();
    }
    
    //overridden for browser initialization that can not be done earlier
    @Override
    public void onAttachBrowser(ChromeAdapter chromeAdapter, ChromeAdapter parentChromeAdapter){
        super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
        initialize();
    }
  
}
   

