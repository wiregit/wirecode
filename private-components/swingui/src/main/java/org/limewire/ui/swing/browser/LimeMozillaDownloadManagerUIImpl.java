package org.limewire.ui.swing.browser;

import org.mozilla.interfaces.nsIDownloadManagerUI;
import org.mozilla.interfaces.nsIFactory;
import org.mozilla.interfaces.nsIInterfaceRequestor;

public class LimeMozillaDownloadManagerUIImpl extends LimeMozillaSelfReferencingFactory
		implements nsIDownloadManagerUI, nsIFactory {

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
