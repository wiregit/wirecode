
// Edited for the Learning branch

package com.limegroup.bittorrent.settings;

import java.io.File;

// TODO: move non-gnutella specific settings out
import com.limegroup.gnutella.settings.FileSetting;
import com.limegroup.gnutella.settings.IntSetting;
import com.limegroup.gnutella.settings.LimeProps;
import com.limegroup.gnutella.settings.SharingSettings;

/**
 * Bittorrent settings
 * 
 */
public class BittorrentSettings extends LimeProps {

	/** Don't make a BittorrentSettings object, just use the static member objects. */
	private BittorrentSettings() {}

	/**
	 * 5 minutes in seconds.
	 * After contacting a tracker, we'll wait at least 5 minutes before contacting it again.
	 */
	public static IntSetting TRACKER_MIN_REASK_INTERVAL = FACTORY.createIntSetting("TRACKER_MIN_REASK_INTERVAL", 5 * 60);

	/**
	 * 2 hours in seconds.
	 * After contacting a tracker, we'll contact it again within the next 2 hours.
	 */
	public static IntSetting TRACKER_MAX_REASK_INTERVAL = FACTORY.createIntSetting("TRACKER_MAX_REASK_INTERVAL", 2 * 60 * 60);

	//do

	/**
	 * maximum uploads per torrent
	 */
	public static IntSetting TORRENT_MAX_UPLOADS = FACTORY.createIntSetting("TORRENT_MAX_UPLOADS", 6);

	//done

	/**
	 * 4, we'll give 4 connections for each torrent data, even if they're not giving us any.
	 * The number of uploads to allow to random hosts, ignoring tit-for-tat.
	 */
	public static IntSetting TORRENT_MIN_UPLOADS = FACTORY.createIntSetting("TORRENT_MIN_UPLOADS", 4);

	/**
	 * 100, the maximum number of connections for each torrent.
	 * We won't have more than 100 TCP socket connections to other computers sharing the same torrent as us.
	 */
	public static IntSetting TORRENT_MAX_CONNECTIONS = FACTORY.createIntSetting("TORRENT_MAX_CONNECTIONS", 100);

	//do

	/**
	 * number of reserved LIME slots
	 */
	public static IntSetting TORRENT_RESERVED_LIME_SLOTS = FACTORY.createIntSetting("TORRENT_RESERVED_LIME_SLOTS", 5);

	/**
	 * A file with a snapshot of current downloading torrents.
	 */
	public static final FileSetting TORRENT_SNAPSHOT_FILE =
		FACTORY.createFileSetting(
			"TORRENT_SNAPSHOT_FILE",
			(new File(
				SharingSettings.INCOMPLETE_DIRECTORY.getValue(),
				"torrents.dat")));

	/**
	 * A file with a snapshot of current downloading torrents.
	 */
	public static final FileSetting TORRENT_SNAPSHOT_BACKUP_FILE =
		FACTORY.createFileSetting(
			"TORRENT_SNAPSHOT_BACKUP_FILE",
			(new File(
				SharingSettings.INCOMPLETE_DIRECTORY.getValue(),
				"torrents.bak")));
}
