package com.limegroup.bittorrent.disk;

import java.io.Serializable;

import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.TorrentFile;
import com.limegroup.bittorrent.settings.BittorrentSettings;

public class DiskManagerFactory {
	private static DiskManagerFactory instance;
	public static DiskManagerFactory instance() {
		if (instance == null)
			instance = new DiskManagerFactory();
		return instance;
	}
	
	protected DiskManagerFactory(){}
	
	public TorrentDiskManager getManager(TorrentContext context,
			Serializable serializedData,
			boolean complete) {
		return new VerifyingFolder(context, 
				complete,
				serializedData, 
				BittorrentSettings.TORRENT_USE_MMAP.getValue() ?
						new MMDiskController<TorrentFile>() :
							new RAFDiskController<TorrentFile>());
	}
}
