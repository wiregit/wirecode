package com.limegroup.gnutella.chat;
/**
 * a class that establishes a connection for a chat, either
 * incoming or outgoing, and also maintains a list of all the 
 * chats currently in progress.
 *
 *@author rsoule
 */

import com.sun.java.util.collections.*;
import com.limegroup.gnutella.*;
import com.limegroup.gnutella.gui.chat.*;
import java.net.*;
import java.io.*;

public class ChatManager {

	// Attributes
	private static ChatManager _chatManager = new ChatManager();
	/* a list of InstantMessenger objects */
	private List _chatsInProgress 
		= Collections.synchronizedList(new LinkedList());
	/* a list of strings that are the hosts that are blocked */
	private List _blockedHosts 
		= Collections.synchronizedList(new LinkedList());
	private ActivityCallback _activityCallback;

	// Operations
	public static ChatManager instance() {
		return _chatManager;
	}

	/** sets the activity callback so that the chats can 
		communicate with the gui */
	public void setActivityCallback(ActivityCallback callback) {
		_activityCallback = callback;
	}

	/** accepts the given socket for a one-to-one
		chat connection, like an instant messanger */
	public void accept(Socket socket) {
		// the Acceptor class recieved a message already, 
		// and asks the ChatManager to create an InstantMessager
		boolean allowChats = SettingsManager.instance().getChatEnabled();

		// see if chatting is turned off
		if (! allowChats) {
			try {
				socket.close();
			} catch (IOException e) {
			}
			return;
		}

		// do a check to see it the host has been blocked
		String host = socket.getInetAddress().getHostAddress();
		SettingsManager sm = SettingsManager.instance();
		String[] bannedIPs = sm.getBannedIps();
		List bannedList = Arrays.asList(bannedIPs);
		if (bannedList.contains(host) ) {
			try {
  				socket.close();
  			} catch (IOException e) {
  			}
  			return;
		}

		try {
			InstantMessenger im = new InstantMessenger(socket, this, 
													   _activityCallback);
			// insert the newly created InstantMessager into the list
			_chatsInProgress.add(im);
			_activityCallback.acceptChat(im);
			im.start();
		} catch (IOException e) {
			try {
				socket.close();
			} catch (IOException ee) {
			}
		} // catch (ConnectException ce) {
		
		// }

	}

	/** request a chat connection from the host specified */
	public Chatter request(String host, int port) {
		InstantMessenger im = null;
		try {
			im = new InstantMessenger(host, port, this, 
									  _activityCallback);
			// insert the newly created InstantMessager into the list
			_chatsInProgress.add(im);
			_activityCallback.acceptChat(im);
			im.start();
		} catch (IOException e) {
		} // catch (ConnectException ce) {
		// }
		return im;
	}

	/** remove the instance of chat from the list of chats
		in progress */
	public void removeChat(InstantMessenger chat) {
		_chatsInProgress.remove(chat);
	}

	/** blocks incoming connections from a particular ip address  */
	public void blockHost(String host) {
		SettingsManager sm = SettingsManager.instance();
		String[] bannedIPs = sm.getBannedIps();
		Arrays.sort(bannedIPs);		
		synchronized (this) {
			if ( Arrays.binarySearch(bannedIPs, host) < 0 ) {
				String[] more_banned = new String[bannedIPs.length+1];
				System.arraycopy(bannedIPs, 0, more_banned, 0, 
								 bannedIPs.length);
				more_banned[bannedIPs.length] = host;
				sm.setBannedIps(more_banned);
			}
		}
	}
	
	public void unblockHost(String host) {
		SettingsManager sm = SettingsManager.instance();
		String[] bannedIPs = sm.getBannedIps();
		List bannedList = Arrays.asList(bannedIPs);
		synchronized (this) {
			if (bannedList.remove(host) )
				sm.setBannedIps((String[])bannedList.toArray() );
		}
	}

	// Private Classes
	private ChatManager() {
	}

}
