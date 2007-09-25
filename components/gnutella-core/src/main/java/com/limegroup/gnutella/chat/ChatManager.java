package com.limegroup.gnutella.chat;

import java.net.Socket;

import org.limewire.io.IOUtils;
import org.limewire.net.ConnectionAcceptor;
import org.limewire.net.ConnectionDispatcher;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.SpamServices;
import com.limegroup.gnutella.settings.ChatSettings;
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
    private final InstantMessengerFactory instantMessengerFactory;
        
    @Inject
	public ChatManager(@Named("global") Provider<ConnectionDispatcher> connectionDispatcher,
            SpamServices spamServices, InstantMessengerFactory instantMessengerFactory) {
        this.connectionDispatcher = connectionDispatcher;
        this.spamServices = spamServices;
        this.instantMessengerFactory = instantMessengerFactory;
    }

    public void start() {
        connectionDispatcher.get().addConnectionAcceptor(this, false, false,
                "CHAT");
    }

    public void stop() {
        connectionDispatcher.get().removeConnectionAcceptor("CHAT");
    }

	/**
     * Invoked by the acceptor to notify this class of a new connection. Accepts
     * the given socket for a one-to-one chat connection, like an instant
     * messenger.
     */
    public void acceptConnection(String word, Socket socket) {
        HTTPStat.CHAT_REQUESTS.incrementStat();

		boolean allowChats = ChatSettings.CHAT_ENABLED.getValue();
		if (!allowChats) {
		    IOUtils.close(socket);
			return;
		}

        if(!spamServices.isAllowed(socket.getInetAddress())) {
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

}
