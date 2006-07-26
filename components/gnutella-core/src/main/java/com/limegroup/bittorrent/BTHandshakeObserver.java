package com.limegroup.bittorrent;

import com.limegroup.bittorrent.handshaking.BTHandshaker;
import com.limegroup.bittorrent.handshaking.IncomingBTHandshaker;

/**
 * An observer for events that happen to BTHandshakers
 */
public interface BTHandshakeObserver {

	/**
	 * Notification that the following incoming handshaker
	 * has started handshaking
	 */
	public void handshakerStarted(IncomingBTHandshaker shaker);

	/**
	 * Notification that the passed handshaker has completed.
	 */
	public void handshakerDone(BTHandshaker shaker);

}