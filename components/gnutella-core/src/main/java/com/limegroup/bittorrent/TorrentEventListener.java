package com.limegroup.bittorrent;

public interface TorrentEventListener {
	public void handleTorrentEvent(TorrentEvent evt);
}
