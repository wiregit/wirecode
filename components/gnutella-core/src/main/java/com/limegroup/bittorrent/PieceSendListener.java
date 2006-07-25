package com.limegroup.bittorrent;

/**
 * Listener that follows the sending of a piece message
 */
interface PieceSendListener {
	/** 
	 * Notification that data has been sent over the network
	 * @param written the number of bytes sent
	 */ 
	public void wroteBytes(int written);
	
	/**
	 * notification that a piece has been sent.
	 */
	public void pieceSent();
}
