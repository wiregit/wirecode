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
	private List _chatsInProgress;
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
		try {
			InstantMessenger im = new InstantMessenger(socket, this, 
													   _activityCallback);
			// insert the newly created InstantMessager into the list
			_chatsInProgress.add((Chat)im);
			_activityCallback.acceptChat(im);
			im.start();
		} catch (IOException e) {
			System.out.println("error in accept: ");
			e.printStackTrace();
		}

	}

	/** request a chat connection from the host specified */
	public Chat request(String host, int port) {
		InstantMessenger im = null;
		try {
			im = new InstantMessenger(host, port, this, 
									  _activityCallback);
			// insert the newly created InstantMessager into the list
			_chatsInProgress.add((Chat)im);
			_activityCallback.acceptChat(im);
			im.start();
		} catch (IOException e) {
			System.out.println("error in request: ");
			e.printStackTrace();
		}
		return im;
	}

	/** remove the instance of chat from the list of chats
		in progress */
	public void removeChat(Chat chat) {
		_chatsInProgress.remove(chat);
	}

	// Private Classes
	private ChatManager() {
		_chatsInProgress = new LinkedList();
	}

}
