package com.limegroup.bittorrent.reader;

import com.limegroup.bittorrent.BTMessageHandler;

class ReaderData {
	
	private final BTMessageHandler handler;
	private final BTDataSource dataSource;
	private final PieceParseListener pieceListener;
	
	private boolean first = true;
	private int length;
	private BTReadMessageState entryState;
	
	ReaderData(BTMessageHandler handler, 
			BTDataSource buf,
			PieceParseListener listener) {
		this.dataSource = buf;
		this.handler = handler;
		this.pieceListener = listener;
	}
	
	public boolean isFirst() {
		return first;
	}
	
	public void clearFirst() {
		this.first = false;
	}
	
	public int getLength() {
		return length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}
	
	public void setEntryState(BTReadMessageState defaultState) {
		this.entryState = defaultState;
	}
	
	public BTReadMessageState getEntryState() {
		return entryState;
	}
	
	public BTMessageHandler getHandler() {
		return handler;
	}
	
	public BTDataSource getDataSource() {
		return dataSource;
	}
	
	public PieceParseListener getPieceListener() {
		return pieceListener;
	}
	
	public String toString() {
		return "reader for "+handler;
	}
}
