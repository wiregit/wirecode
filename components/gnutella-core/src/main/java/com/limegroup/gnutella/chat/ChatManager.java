package com.limegroup.gnutella.chat;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ConnectionAcceptor;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.statistics.HTTPStat;
import com.limegroup.gnutella.util.Comparators;
import com.limegroup.gnutella.util.IOUtils;

/**
 * This class establishes a connection for a chat, either
 * incoming or outgoing, and also maintains a list of all the 
 * chats currently in progress.
 *
 * @author rsoule
 */
public final class ChatManager implements ConnectionAcceptor {

	/**
	 * Constant for the <tt>ChatManager</tt> instance, following
	 * singleton.
	 */
	private static final ChatManager CHAT_MANAGER = new ChatManager();

	/** 
	 * <tt>List</tt> of InstantMessenger objects.
	 */
	private List<InstantMessenger> _chatsInProgress 
		= Collections.synchronizedList(new LinkedList<InstantMessenger>());

	/**
	 * Instance accessor for the <tt>ChatManager</tt>.
	 */
	public static ChatManager instance() {
		return CHAT_MANAGER;
	}

	private ChatManager() {
		RouterService.getConnectionDispatcher().
		addConnectionAcceptor(this,
				new String[]{"CHAT"},
				false,
				false);
	}
	
	/** 
	 * Accepts the given socket for a one-to-one
	 * chat connection, like an instant messanger.
	 */
	public void accept(Socket socket) {
        Thread.currentThread().setName("IncomingChatThread");
		// the Acceptor class recieved a message already, 
		// and asks the ChatManager to create an InstantMessager
		boolean allowChats = ChatSettings.CHAT_ENABLED.getValue();

		// see if chatting is turned off
		if (! allowChats) {
		    IOUtils.close(socket);
			return;
		}

		// do a check to see it the host has been blocked
        if(!RouterService.getIpFilter().allow(socket.getInetAddress())) {
            IOUtils.close(socket);
            return;
        }
        
		try {
			ActivityCallback callback = RouterService.getCallback();
			InstantMessenger im = new InstantMessenger(socket, this, callback);
			// insert the newly created InstantMessager into the list
			_chatsInProgress.add(im);
			callback.acceptChat(im);
			im.start();
		} catch (IOException e) {
            IOUtils.close(socket);
		}
	}

	/** 
	 * Request a chat connection from the host specified 
	 * returns an uninitialized chat connection.  the callback
	 * will be called when the connection is established or
	 * the connection has died.
	 */
	public Chatter request(String host, int port) {
		InstantMessenger im = null;
		ActivityCallback callback = 
		    RouterService.getCallback();
		im = new InstantMessenger(host, port, this, callback);
		// insert the newly created InstantMessager into the list
		_chatsInProgress.add(im);
		im.start();
		return im;
	}

	/** 
	 * Remove the instance of chat from the list of chats
	 * in progress.
	 */
	public void removeChat(InstantMessenger chat) {
		_chatsInProgress.remove(chat);
	}

	/** blocks incoming connections from a particular ip address  */
	public void blockHost(String host) {
		String[] bannedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
		Arrays.sort(bannedIPs, Comparators.stringComparator());
		synchronized (this) {
			if ( Arrays.binarySearch(bannedIPs, host, 
									 Comparators.stringComparator()) < 0 ) {
				String[] more_banned = new String[bannedIPs.length+1];
				System.arraycopy(bannedIPs, 0, more_banned, 0, 
								 bannedIPs.length);
				more_banned[bannedIPs.length] = host;
                FilterSettings.BLACK_LISTED_IP_ADDRESSES.setValue(more_banned);
			}
		}
	}
	
	public void unblockHost(String host) {
		String[] bannedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
		List<String> bannedList = Arrays.asList(bannedIPs);
		synchronized (this) {
			if (bannedList.remove(host) )
                FilterSettings.BLACK_LISTED_IP_ADDRESSES.
                    setValue((String[])bannedList.toArray());
		}
	}

	public void acceptConnection(String word, Socket s) {
		HTTPStat.CHAT_REQUESTS.incrementStat();
		accept(s);
	}
}
