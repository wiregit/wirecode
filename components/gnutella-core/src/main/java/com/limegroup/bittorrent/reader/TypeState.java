package com.limegroup.bittorrent.reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.bittorrent.messages.BTMessage;
import com.limegroup.bittorrent.messages.BadBTMessageException;

/**
 * State that parses the type of a BT message. 
 */
class TypeState extends BTReadMessageState {

	private static final Log LOG = LogFactory.getLog(TypeState.class);
	
	private byte type = -1;
	
	TypeState(ReaderData readerState) {
		super(readerState);
	}
	
	public BTReadMessageState addData() throws BadBTMessageException {
		BTDataSource buf = readerState.getDataSource();
		if (buf.size() < 1)
			return null;
		
		type = buf.get();
		
		if (LOG.isDebugEnabled())
			LOG.debug(this+" parsed type "+type);
		
		boolean wasFirst = readerState.isFirst();
		readerState.clearFirst();
		if (wasFirst && type == BTMessage.BITFIELD)
			return new BitFieldState(readerState); // only sent as first message if at all.
		else if (type == BTMessage.PIECE)
			return new PieceState(readerState);
		else 
			return new MessageState(readerState,type);
	}
	
	public String toString() {
		return "type state of "+readerState;
	}

}
