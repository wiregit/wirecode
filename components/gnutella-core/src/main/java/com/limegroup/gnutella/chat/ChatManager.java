package com.limegroup.gnutella.chat;
/**
 * this class establishes a connection for a chat, either outgoing
 * or incoming, and maintains a list of all the chats currently
 * going on.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|


import java.net.*;
import java.io.*;
import com.sun.java.util.collections.*;
import com.limegroup.gnutella.gui.*;
import com.limegroup.gnutella.chat.gui.*;

import java.awt.Container;

public class ChatManager {

	
	/** variable for following the singleton pattern. */
	private static ChatManager _chatManager;
	/** a list of all the current chats in progress */
	private List _chatsInProgress;

	/**
	 * private constructor for singleton
	 */
	private ChatManager() {
		_chatsInProgress = new LinkedList();
	}
		
	/**
	 * return an instance of the chat manager class, following
	 * the singleton
	 */
	public static ChatManager instance() {
		if (_chatManager == null)
			_chatManager = new ChatManager();
		return _chatManager;
	}

	/**
	 * accepts the given socket for a one-to-one 
	 * chat connection, like an Instant Message
	 */
	public void acceptIM(Socket socket) {
		try {
			InstantMessage im = new InstantMessage(socket);
			// need to add to the list:
			// _chatsInProgress.add(im);
		} catch (IOException e) {
			// unable to recieve connection.
		}
		
		com.limegroup.gnutella.gui.MainFrame mf =
		com.limegroup.gnutella.gui.MainFrame.instance();
		ChatView view = new ChatView(mf);
		Container contentPane = mf.getContentPane();
		contentPane.add(view);
		view.setVisible(true);
		// contentPane.repaint();
		// mf.setVisible(true);

	}

	public void requestIM(String host, int port) {
		try {
			InstantMessage im = new InstantMessage(host, port);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("can't request IM");
			return;
		}


	}

	/**
	 * parses out the information for chatting from the chat request
	 */
	private static void parseChat(Socket socket) throws IOException {

	}

}
