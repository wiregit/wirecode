package com.limegroup.bittorrent;

import java.util.EventObject;

public class TorrentEvent extends EventObject {

	private static final long serialVersionUID = 5166816249517367147L;

	public enum Type {STARTING,STARTED,STOPPED,COMPLETE,STOP_REQUESTED, STOP_APPROVED}
	
	private final Type type;
	private final ManagedTorrent torrent;
	public TorrentEvent(Object source, Type type, ManagedTorrent torrent) {
		super(source);
		this.type = type;
		this.torrent = torrent;
	}
	
	public Type getType() {
		return type;
	}
	
	public ManagedTorrent getTorrent() {
		return torrent;
	}
}
