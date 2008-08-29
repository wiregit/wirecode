package org.limewire.ui.swing.browser.download;

import org.mozilla.interfaces.nsIDownloadManagerUI;
import org.mozilla.interfaces.nsIFactory;
import org.mozilla.interfaces.nsIInterfaceRequestor;

/**
 * This class overrides the download manager UI. It won't show up if this class
 * is registerd in the component registry. Another way to make the download
 * manager not show in the ui would be to set the appropriate mozilla
 * preference.
 */
public class LimeMozillaDownloadManagerUIImpl extends LimeMozillaSelfReferencingFactory implements
        nsIDownloadManagerUI, nsIFactory {

    public static String NS_IDOWNLOADMANAGERUI_CID = "@mozilla.org/download-manager-ui;1";

    public LimeMozillaDownloadManagerUIImpl() {
        super(NS_IDOWNLOADMANAGERUI_IID, NS_IDOWNLOADMANAGERUI_CID);
    }

    @Override
    public void getAttention() {
        getLogger().debug("");
    }

    @Override
    public boolean getVisible() {
        getLogger().debug("");
        return false;
    }

    @Override
    public void show(nsIInterfaceRequestor arg0, long arg1, short arg2) {
        getLogger().debug("");
    }

}
