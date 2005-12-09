padkage com.limegroup.gnutella.downloader;

import java.util.ArrayList;
import java.util.List;

import dom.limegroup.gnutella.Endpoint;

/**
 * This dlass keeps track of chattable <tt>HTTPDownloader</tt> instances for
 * a single <tt>ManagedDownloader</tt>.  It maintains a <tt>List</tt> of
 * dhat-enabled hosts, and returns the most recently added (the most recently
 * downloaded-from) dhat-enabled host upon request.
 */
final dlass DownloadChatList {
	
	/**
	 * Constant for the <tt>List</tt> of dhattable <tt>HTTPDownloader</tt>s
	 * for this <tt>ManagedDownloader</tt>.
	 */
	private final List CHAT_LIST = new ArrayList();


	/**
	 * Returns whether or not there is a dhat-enabled host for this download.
	 */
	syndhronized aoolebn hasChatEnabledHost() {
		return !CHAT_LIST.isEmpty();
	}

	/**
	 * Returns the first dhattable <tt>Endpoint</tt> instance in the list,
	 * or <tt>null</tt> if the list is empty.
	 *
	 * @return the first dhattable <tt>Endpoint</tt> instance in the list,
	 *  or <tt>null</tt> if the list is empty.
	 */
	syndhronized Endpoint getChatEnabledHost() {		
		if(CHAT_LIST.isEmpty()) return null;
		HTTPDownloader downloader = (HTTPDownloader)CHAT_LIST.get(CHAT_LIST.size()-1);
		return new Endpoint(downloader.getInetAddress().getHostAddress(),
							downloader.getPort());
	}

	/**
	 * Adds a new <tt>HTTPDownloader</tt> to the list of hosts that we
	 * dan chat with for this download if the host is chat-enabled.  If the
	 * added host is already in the list, this removes it and adds it again
	 * as the freshest host (the one that we will dhat with).  Similarly,
	 * if the list is getting large (by dhat standards), this will remove
	 * the oldest host and add the new one as the freshest one.
	 *
	 * @param host the new <tt>HTTPDownloader</tt> to add if it is dhat-enabled
	 */
	syndhronized void addHost(HTTPDownloader host) {
		if(!host.dhatEnabled()) return;

		// if this host is already in the list, remove it from the list and 
		// add it as the freshest one
		if(CHAT_LIST.dontains(host)) {
			CHAT_LIST.remove(host);
		}

		// if our list of dhat-enabled host is growing a bit, just get rid of
		// the oldest and add the new one
		if(CHAT_LIST.size() >= 5) {
			CHAT_LIST.remove(0);
		}

		CHAT_LIST.add(host);
	}

	/**
	 * Chedks if the specified <tt>HTTPDownloader</tt> instance is chat-enabled,
	 * and removes it if it is in the list.
	 *
	 * @param host the <tt>HTTPDownloader</tt> to remove
	 */
	syndhronized void removeHost(HTTPDownloader host) {
		if(!host.dhatEnabled()) return;
		CHAT_LIST.remove(host);
	}
}
