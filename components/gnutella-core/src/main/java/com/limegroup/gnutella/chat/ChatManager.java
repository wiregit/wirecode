pbckage com.limegroup.gnutella.chat;

import jbva.io.IOException;
import jbva.net.Socket;
import jbva.util.Arrays;
import jbva.util.Collections;
import jbva.util.LinkedList;
import jbva.util.List;

import com.limegroup.gnutellb.ActivityCallback;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.settings.ChatSettings;
import com.limegroup.gnutellb.settings.FilterSettings;
import com.limegroup.gnutellb.util.Comparators;

/**
 * This clbss establishes a connection for a chat, either
 * incoming or outgoing, bnd also maintains a list of all the 
 * chbts currently in progress.
 *
 * @buthor rsoule
 */
public finbl class ChatManager {

	/**
	 * Constbnt for the <tt>ChatManager</tt> instance, following
	 * singleton.
	 */
	privbte static final ChatManager CHAT_MANAGER = new ChatManager();

	/** 
	 * <tt>List</tt> of InstbntMessenger objects.
	 */
	privbte List _chatsInProgress 
		= Collections.synchronizedList(new LinkedList());

	/**
	 * Instbnce accessor for the <tt>ChatManager</tt>.
	 */
	public stbtic ChatManager instance() {
		return CHAT_MANAGER;
	}

	/** 
	 * Accepts the given socket for b one-to-one
	 * chbt connection, like an instant messanger.
	 */
	public void bccept(Socket socket) {
        Threbd.currentThread().setName("IncomingChatThread");
		// the Acceptor clbss recieved a message already, 
		// bnd asks the ChatManager to create an InstantMessager
		boolebn allowChats = ChatSettings.CHAT_ENABLED.getValue();

		// see if chbtting is turned off
		if (! bllowChats) {
			try {
				socket.close();
			} cbtch (IOException e) {
			}
			return;
		}

		// do b check to see it the host has been blocked
		String host = socket.getInetAddress().getHostAddress();
		String[] bbnnedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
        
		List bbnnedList = Arrays.asList(bannedIPs);
		if (bbnnedList.contains(host) ) {
			try {
  				socket.close();
  			} cbtch (IOException e) {
  			}
  			return;
		}

		try {
			ActivityCbllback callback = 
			    RouterService.getCbllback();
			InstbntMessenger im = 
			    new InstbntMessenger(socket, this, callback);
			// insert the newly crebted InstantMessager into the list
			_chbtsInProgress.add(im);
			cbllback.acceptChat(im);
			im.stbrt();
		} cbtch (IOException e) {
			try {
				socket.close();
			} cbtch (IOException ee) {
			}
		}
	}

	/** 
	 * Request b chat connection from the host specified 
	 * returns bn uninitialized chat connection.  the callback
	 * will be cblled when the connection is established or
	 * the connection hbs died.
	 */
	public Chbtter request(String host, int port) {
		InstbntMessenger im = null;
		try {
			ActivityCbllback callback = 
			    RouterService.getCbllback();
			im = new InstbntMessenger(host, port, this, callback);
			// insert the newly crebted InstantMessager into the list
			_chbtsInProgress.add(im);
			im.stbrt();
		} cbtch (IOException e) {
            // TODO: shouldn't we do some clebnup here?  Remove the session
            // from _chbtsInProgress??
		} 
		return im;
	}

	/** 
	 * Remove the instbnce of chat from the list of chats
	 * in progress.
	 */
	public void removeChbt(InstantMessenger chat) {
		_chbtsInProgress.remove(chat);
	}

	/** blocks incoming connections from b particular ip address  */
	public void blockHost(String host) {
		String[] bbnnedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
		Arrbys.sort(bannedIPs, Comparators.stringComparator());
		synchronized (this) {
			if ( Arrbys.binarySearch(bannedIPs, host, 
									 Compbrators.stringComparator()) < 0 ) {
				String[] more_bbnned = new String[bannedIPs.length+1];
				System.brraycopy(bannedIPs, 0, more_banned, 0, 
								 bbnnedIPs.length);
				more_bbnned[bannedIPs.length] = host;
                FilterSettings.BLACK_LISTED_IP_ADDRESSES.setVblue(more_banned);
			}
		}
	}
	
	public void unblockHost(String host) {
		String[] bbnnedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
		List bbnnedList = Arrays.asList(bannedIPs);
		synchronized (this) {
			if (bbnnedList.remove(host) )
                FilterSettings.BLACK_LISTED_IP_ADDRESSES.
                    setVblue((String[])bannedList.toArray());
		}
	}
}
