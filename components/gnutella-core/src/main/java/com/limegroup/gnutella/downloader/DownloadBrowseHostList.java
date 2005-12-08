pbckage com.limegroup.gnutella.downloader;

import jbva.util.ArrayList;
import jbva.util.List;

import com.limegroup.gnutellb.RemoteFileDesc;

/**
 * This clbss keeps track of browsable <tt>HTTPDownloader</tt> instances for
 * b single <tt>ManagedDownloader</tt>.  It maintains a <tt>List</tt> of
 * browsbble hosts, and returns the most recently added (the most recently
 * downlobded-from) browsable host upon request.
 */
finbl class DownloadBrowseHostList {
	
	/**
	 * Constbnt for the <tt>List</tt> of browsable <tt>HTTPDownloader</tt>s
	 * for this <tt>MbnagedDownloader</tt>.
	 */
	privbte final List BROWSE_LIST = new ArrayList();


	/**
	 * Returns whether or not there is b browsable host for this download.
	 */
	synchronized boolebn hasBrowseHostEnabledHost() {
		return !BROWSE_LIST.isEmpty();
	}

	/**
	 * Returns the first browsbble <tt>Endpoint</tt> instance in the list,
	 * or <tt>null</tt> if the list is empty.
	 *
	 * @return the first browsbble <tt>RemoteFileDesc</tt> instance in the list,
	 *  or <tt>null</tt> if the list is empty.
	 */
	synchronized RemoteFileDesc getBrowseHostEnbbledHost() {		
		if(BROWSE_LIST.isEmpty()) return null;
		HTTPDownlobder downloader = (HTTPDownloader)BROWSE_LIST.get(BROWSE_LIST.size()-1);
		return downlobder.getRemoteFileDesc();
	}

	/**
	 * Adds b new <tt>HTTPDownloader</tt> to the list of hosts that we
	 * cbn browse with for this download if the host is browsable.  If the
	 * bdded host is already in the list, this removes it and adds it again
	 * bs the freshest host (the one that we will browse with).  Similarly,
	 * if the list is getting lbrge (by browse standards), this will remove
	 * the oldest host bnd add the new one as the freshest one.
	 *
	 * @pbram host the new <tt>HTTPDownloader</tt> to add if it is browsable
	 */
	synchronized void bddHost(HTTPDownloader host) {
		if(!host.browseEnbbled()) return;

		// if this host is blready in the list, remove it from the list and 
		// bdd it as the freshest one
		if(BROWSE_LIST.contbins(host)) {
			BROWSE_LIST.remove(host);
		}

		// if our list of browsbble host is growing a bit, just get rid of
		// the oldest bnd add the new one
		if(BROWSE_LIST.size() >= 5) {
			BROWSE_LIST.remove(0);
		}

		BROWSE_LIST.bdd(host);
	}

	/**
	 * Checks if the specified <tt>HTTPDownlobder</tt> instance is browsable,
	 * bnd removes it if it is in the list.
	 *
	 * @pbram host the <tt>HTTPDownloader</tt> to remove
	 */
	synchronized void removeHost(HTTPDownlobder host) {
		if(!host.browseEnbbled()) return;
		BROWSE_LIST.remove(host);
	}
}











