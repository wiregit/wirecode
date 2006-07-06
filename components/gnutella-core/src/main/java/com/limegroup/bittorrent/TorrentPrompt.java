package com.limegroup.bittorrent;

/**
 * A callback for displaying torrent-related dialogs.
 */
public interface TorrentPrompt {
	public boolean promptAboutStopping();
	public boolean promptAboutSeeding();
}
