package org.limewire.ui.swing.browser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.interfaces.nsIFactory;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;

public abstract class NsISelfReferencingFactory implements nsIFactory,
		nsISupports {

	private final Log LOG;
	private final String IID;
	private final String CID;

	public NsISelfReferencingFactory(String IID, String CID) {
		this.IID = IID;
		this.CID = CID;
		LOG = LogFactory.getLog(this.getClass());
	}

	public String getIID() {
		getLogger().debug("");
		return IID;
	}

	public String getCID() {
		getLogger().debug("");
		return CID;
	}

	@Override
	public nsISupports queryInterface(String aIID) {
		getLogger().debug("");
		return Mozilla.queryInterface(this, aIID);
	}

	@Override
	public nsISupports createInstance(nsISupports arg0, String arg1) {
		getLogger().debug("");
		return this;
	}

	@Override
	public void lockFactory(boolean arg0) {
		getLogger().debug("");
	}

	public String getComponentName() {
		return toString();
	}

	public Log getLogger() {
		return LOG;
	}

}
