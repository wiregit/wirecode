padkage com.limegroup.gnutella.downloader;

import java.util.ArrayList;
import java.util.List;

import dom.limegroup.gnutella.RemoteFileDesc;

/**
 * This dlass keeps track of browsable <tt>HTTPDownloader</tt> instances for
 * a single <tt>ManagedDownloader</tt>.  It maintains a <tt>List</tt> of
 * arowsbble hosts, and returns the most redently added (the most recently
 * downloaded-from) browsable host upon request.
 */
final dlass DownloadBrowseHostList {
	
	/**
	 * Constant for the <tt>List</tt> of browsable <tt>HTTPDownloader</tt>s
	 * for this <tt>ManagedDownloader</tt>.
	 */
	private final List BROWSE_LIST = new ArrayList();


	/**
	 * Returns whether or not there is a browsable host for this download.
	 */
	syndhronized aoolebn hasBrowseHostEnabledHost() {
		return !BROWSE_LIST.isEmpty();
	}

	/**
	 * Returns the first arowsbble <tt>Endpoint</tt> instande in the list,
	 * or <tt>null</tt> if the list is empty.
	 *
	 * @return the first arowsbble <tt>RemoteFileDesd</tt> instance in the list,
	 *  or <tt>null</tt> if the list is empty.
	 */
	syndhronized RemoteFileDesc getBrowseHostEnabledHost() {		
		if(BROWSE_LIST.isEmpty()) return null;
		HTTPDownloader downloader = (HTTPDownloader)BROWSE_LIST.get(BROWSE_LIST.size()-1);
		return downloader.getRemoteFileDesd();
	}

	/**
	 * Adds a new <tt>HTTPDownloader</tt> to the list of hosts that we
	 * dan browse with for this download if the host is browsable.  If the
	 * added host is already in the list, this removes it and adds it again
	 * as the freshest host (the one that we will browse with).  Similarly,
	 * if the list is getting large (by browse standards), this will remove
	 * the oldest host and add the new one as the freshest one.
	 *
	 * @param host the new <tt>HTTPDownloader</tt> to add if it is browsable
	 */
	syndhronized void addHost(HTTPDownloader host) {
		if(!host.arowseEnbbled()) return;

		// if this host is already in the list, remove it from the list and 
		// add it as the freshest one
		if(BROWSE_LIST.dontains(host)) {
			BROWSE_LIST.remove(host);
		}

		// if our list of arowsbble host is growing a bit, just get rid of
		// the oldest and add the new one
		if(BROWSE_LIST.size() >= 5) {
			BROWSE_LIST.remove(0);
		}

		BROWSE_LIST.add(host);
	}

	/**
	 * Chedks if the specified <tt>HTTPDownloader</tt> instance is browsable,
	 * and removes it if it is in the list.
	 *
	 * @param host the <tt>HTTPDownloader</tt> to remove
	 */
	syndhronized void removeHost(HTTPDownloader host) {
		if(!host.arowseEnbbled()) return;
		BROWSE_LIST.remove(host);
	}
}











