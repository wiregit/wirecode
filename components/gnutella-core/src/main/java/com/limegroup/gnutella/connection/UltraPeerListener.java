package com.limegroup.gnutella.connection;

/**
 * This interface defines the basic behavior of a class that wishes
 * to be notified when this node becomes an UltraPeer.
 */
public interface UltraPeerListener {

	/**
	 * Notifies the listener that this node has become an UltraPeer.
	 */
	void ultraPeerConnectionEstablished();
}
