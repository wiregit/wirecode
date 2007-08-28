package com.limegroup.gnutella.chat;

import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import org.limewire.collection.Comparators;
import org.limewire.io.IOUtils;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.SpamServices;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.settings.ChatSettings;
import com.limegroup.gnutella.settings.FilterSettings;
import com.limegroup.gnutella.statistics.HTTPStat;

/**
 * This class establishes a connection for a chat, either
 * incoming or outgoing, and also maintains a list of all the 
 * chats currently in progress.
 *
 * @author rsoule
 */
@Singleton
public final class ChatManager implements ConnectionAcceptor {

    private final Provider<ConnectionDispatcher> connectionDispatcher;
    private final SpamServices spamServices;
    private final Provider<IPFilter> ipFilter;
    private final InstantMessengerFactory instantMessengerFactory;
        
    @Inject
	public ChatManager(@Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
            SpamServices spamServices, Provider<IPFilter> ipFilter,
            InstantMessengerFactory instantMessengerFactory) {
        this.connectionDispatcher = connectionDispatcher;
        this.spamServices = spamServices;
        this.ipFilter = ipFilter;
        this.instantMessengerFactory = instantMessengerFactory;
    }

    public void initialize() {
        connectionDispatcher.get().addConnectionAcceptor(this, false, false,
                "CHAT");
    }
    
	/**
     * Invoked by the acceptor to notify this class of a new connection. Accepts
     * the given socket for a one-to-one chat connection, like an instant
     * messenger.
     */
	public void accept(Socket socket) {
		boolean allowChats = ChatSettings.CHAT_ENABLED.getValue();
		if (!allowChats) {
		    IOUtils.close(socket);
			return;
		}

        if(!ipFilter.get().allow(socket.getInetAddress())) {
            IOUtils.close(socket);
            return;
        }
        
        InstantMessenger im = instantMessengerFactory.createIncomingInstantMessenger(socket);
        im.start();
	}

	/** 
	 * Request a chat connection from the host specified 
	 * returns an uninitialized chat connection.  the callback
	 * will be called when the connection is established or
	 * the connection has died.
	 */
	public Chatter request(String host, int port) {
        InstantMessenger im = instantMessengerFactory.createOutgoingInstantMessenger(host, port);
        return im;
	}

//	/** 
//	 * Remove the instance of chat from the list of chats
//	 * in progress.
//	 */
//	public void removeChat(InstantMessenger chat) {
//		_chatsInProgress.remove(chat);
//	}

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
                spamServices.reloadIPFilter();
			}
		}
	}
	
	public void unblockHost(String host) {
		String[] bannedIPs = FilterSettings.BLACK_LISTED_IP_ADDRESSES.getValue();
		List<String> bannedList = Arrays.asList(bannedIPs);
		synchronized (this) {
			if (bannedList.remove(host) ) {
                FilterSettings.BLACK_LISTED_IP_ADDRESSES.
                    setValue((String[])bannedList.toArray());
				spamServices.reloadIPFilter();
			}
		}
	}

	public void acceptConnection(String word, Socket s) {
		HTTPStat.CHAT_REQUESTS.incrementStat();
		accept(s);
	}
}
