package org.limewire.ui.swing.browser.download;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.interfaces.nsIFactory;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.xpcom.Mozilla;

/**
 * This class provides helper methods for extending classes to register
 * themselves with mozilla.
 */
public abstract class LimeMozillaSingletonFactory implements nsIFactory, nsISupports {

    private final Log LOG;

    private final String IID;

    private final String CID;

    public LimeMozillaSingletonFactory(String IID, String CID) {
        this.IID = IID;
        this.CID = CID;
        LOG = LogFactory.getLog(this.getClass());
    }

    /**
     * Returns the UID for this xpcom object.
     */
    public String getIID() {
        getLogger().debug("getIID");
        return IID;
    }

    /**
     * Returns the contract id for this xpcom object.
     */
    public String getCID() {
        getLogger().debug("getCID");
        return CID;
    }

    @Override
    public nsISupports queryInterface(String aIID) {
        getLogger().debug("queryInterface");
        return Mozilla.queryInterface(this, aIID);
    }

    @Override
    public nsISupports createInstance(nsISupports arg0, String arg1) {
        getLogger().debug("createInstance");
        return this;
    }

    @Override
    public void lockFactory(boolean arg0) {
        getLogger().debug("lockFactory");
    }

    /**
     * Returns the component name of this xpcom object.
     */
    public String getComponentName() {
        getLogger().debug("getComponentName");
        return toString();
    }

    /**
     * Returns a logger constructed for use by this class.
     */
    public Log getLogger() {
        return LOG;
    }

}
