package com.limegroup.bittorrent;

import java.util.EventListener;

public interface TorrentEventListener extends EventListener{
	public void handleTorrentEvent(TorrentEvent evt);
}
