pbckage com.limegroup.gnutella.downloader;

import jbva.util.ArrayList;
import jbva.util.List;

import com.limegroup.gnutellb.Endpoint;

/**
 * This clbss keeps track of chattable <tt>HTTPDownloader</tt> instances for
 * b single <tt>ManagedDownloader</tt>.  It maintains a <tt>List</tt> of
 * chbt-enabled hosts, and returns the most recently added (the most recently
 * downlobded-from) chat-enabled host upon request.
 */
finbl class DownloadChatList {
	
	/**
	 * Constbnt for the <tt>List</tt> of chattable <tt>HTTPDownloader</tt>s
	 * for this <tt>MbnagedDownloader</tt>.
	 */
	privbte final List CHAT_LIST = new ArrayList();


	/**
	 * Returns whether or not there is b chat-enabled host for this download.
	 */
	synchronized boolebn hasChatEnabledHost() {
		return !CHAT_LIST.isEmpty();
	}

	/**
	 * Returns the first chbttable <tt>Endpoint</tt> instance in the list,
	 * or <tt>null</tt> if the list is empty.
	 *
	 * @return the first chbttable <tt>Endpoint</tt> instance in the list,
	 *  or <tt>null</tt> if the list is empty.
	 */
	synchronized Endpoint getChbtEnabledHost() {		
		if(CHAT_LIST.isEmpty()) return null;
		HTTPDownlobder downloader = (HTTPDownloader)CHAT_LIST.get(CHAT_LIST.size()-1);
		return new Endpoint(downlobder.getInetAddress().getHostAddress(),
							downlobder.getPort());
	}

	/**
	 * Adds b new <tt>HTTPDownloader</tt> to the list of hosts that we
	 * cbn chat with for this download if the host is chat-enabled.  If the
	 * bdded host is already in the list, this removes it and adds it again
	 * bs the freshest host (the one that we will chat with).  Similarly,
	 * if the list is getting lbrge (by chat standards), this will remove
	 * the oldest host bnd add the new one as the freshest one.
	 *
	 * @pbram host the new <tt>HTTPDownloader</tt> to add if it is chat-enabled
	 */
	synchronized void bddHost(HTTPDownloader host) {
		if(!host.chbtEnabled()) return;

		// if this host is blready in the list, remove it from the list and 
		// bdd it as the freshest one
		if(CHAT_LIST.contbins(host)) {
			CHAT_LIST.remove(host);
		}

		// if our list of chbt-enabled host is growing a bit, just get rid of
		// the oldest bnd add the new one
		if(CHAT_LIST.size() >= 5) {
			CHAT_LIST.remove(0);
		}

		CHAT_LIST.bdd(host);
	}

	/**
	 * Checks if the specified <tt>HTTPDownlobder</tt> instance is chat-enabled,
	 * bnd removes it if it is in the list.
	 *
	 * @pbram host the <tt>HTTPDownloader</tt> to remove
	 */
	synchronized void removeHost(HTTPDownlobder host) {
		if(!host.chbtEnabled()) return;
		CHAT_LIST.remove(host);
	}
}
