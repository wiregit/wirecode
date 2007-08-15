package com.limegroup.bittorrent;


import org.limewire.collection.NECallable;

import com.limegroup.bittorrent.messages.BTMessage;

/**
 * A handler for received BTMessages.  Since Piece messages
 * are large, they are handled in a three step process:
 * 1. startReceivingPiece() is called when the piece header is parsed
 * 2. handlePiece() is called whenever there is data received that belongs
 * to the piece
 * 3. readBytes() is called whenver data that belongs to the piece is about
 * to be written to disk
 * 4. finishReceivingPiece() is called when the entire message is received.
 */
public interface BTMessageHandler {
	
	/**
	 * @param message the incoming message to process.
	 */
	public void processMessage(BTMessage message);

	/**
	 * handles a piece message and sends its payload to disk
	 * @param factory the<tt>BTPieceFactory</tt> that will
	 * create the piece.
	 */
	public void handlePiece(NECallable<BTPiece> factory);
	
	/**
	 * notification that some bytes belonging to a Piece message 
	 * are about to be written to disk.
	 */
	public void readBytes(int read);

	/**
	 * Notification to start receiving a Piece message 
	 * carrying the specified interval.
	 * @return true if the message was expected.
	 */
	public boolean startReceivingPiece(BTInterval piece);
	
	/**
	 * Notification to finish receiving a piece message.
	 */
	public void finishReceivingPiece();
}
