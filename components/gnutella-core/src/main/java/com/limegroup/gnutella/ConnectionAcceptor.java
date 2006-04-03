package com.limegroup.gnutella;

import java.net.Socket;
import java.util.Collection;

/**
 * Objects of this type can be notified whenever a new
 * connection is established and the first word on the wire
 * is read.  
 * 
 * The objects are responsible for registering themselves
 * with the ConnectionDispatcher.
 */
public interface ConnectionAcceptor {
	/**
	 * Notification that a new incoming socket has been
	 * opened.
	 * @param word first word that arrived on the wire
	 * @param s the newly opened socket.
	 */
	public void acceptConnection(String word, Socket s);
	
	/**
	 * @return a Collection of words this understands
	 */
	public Collection getFirstWords();
	
	/**
	 * @return whether this acceptor should accept only
	 * localhost connections.
	 */
	public boolean localOnly();
	
	/**
	 * @return whether this acceptor is blocking (i.e. should
	 * run in a different thread).
	 */
	public boolean isBlocking();
}
