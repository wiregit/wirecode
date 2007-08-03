package com.limegroup.bittorrent;

import java.util.EventObject;

public class TorrentEvent extends EventObject {

	private static final long serialVersionUID = 5166816249517367147L;

	public enum Type {STARTING,STARTED,DOWNLOADING,STOPPED,COMPLETE,STOP_REQUESTED, STOP_APPROVED}
	
	private final Type type;
	private final ManagedTorrent torrent;
    private final String description;
    public TorrentEvent(Object source, Type type, ManagedTorrent torrent) {
        this(source, type, torrent, null);
    }
    
	public TorrentEvent(Object source, Type type, ManagedTorrent torrent, String description) {
		super(source);
		this.type = type;
		this.torrent = torrent;
        this.description = description;
	}
	
	public Type getType() {
		return type;
	}
	
	public ManagedTorrent getTorrent() {
		return torrent;
	}
    
    public String getDescription() {
        return description;
    }
}
