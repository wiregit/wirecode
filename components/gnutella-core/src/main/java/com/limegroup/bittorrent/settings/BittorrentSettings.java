package com.limegroup.bittorrent.settings;


import com.limegroup.gnutella.settings.BooleanSetting;
import com.limegroup.gnutella.settings.IntSetting;
import com.limegroup.gnutella.settings.LimeProps;

/**
 * Bittorrent settings
 */
public class BittorrentSettings extends LimeProps {
	private BittorrentSettings() {
		// empty constructor
	}

	/**
	 * Setting whether LimeWire should manage the BT settings
	 * automatically.
	 */
	public static BooleanSetting AUTOMATIC_SETTINGS = 
		FACTORY.createBooleanSetting("BT_AUTOMATIC_SETTINGS", true);
				
	/**
	 * minimum tracker reask delay in seconds that we will use
	 */
	public static IntSetting TRACKER_MIN_REASK_INTERVAL = FACTORY
			.createIntSetting("TRACKER_MIN_REASK_INTERVAL", 5 * 60);

	/**
	 * maximum tracker reask delay in seconds
	 */
	public static IntSetting TRACKER_MAX_REASK_INTERVAL = FACTORY
			.createIntSetting("TRACKER_MAX_REASK_INTERVAL", 2 * 60 * 60);

	/**
	 * maximum uploads per torrent
	 */
	public static IntSetting TORRENT_MAX_UPLOADS = FACTORY.createIntSetting(
			"TORRENT_MAX_UPLOADS", 6);

	/**
	 * the number of uploads to allow to random hosts, ignoring tit-for-tat
	 */
	public static IntSetting TORRENT_MIN_UPLOADS = FACTORY.createIntSetting(
			"TORRENT_MIN_UPLOADS", 4);

	/**
	 * max connections per torrent
	 */
	public static IntSetting TORRENT_MAX_CONNECTIONS = FACTORY
			.createIntSetting("TORRENT_MAX_CONNECTIONS", 150);
	
	/**
	 * max active torrents
	 */
	public static IntSetting MAX_ACTIVE_TORRENTS = 
		FACTORY.createIntSetting("MAX_TORRENTS", 0);
}
