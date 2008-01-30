package com.limegroup.bittorrent.disk;

import com.google.inject.Singleton;
import com.limegroup.bittorrent.TorrentContext;
import com.limegroup.bittorrent.TorrentFile;
import com.limegroup.bittorrent.settings.BittorrentSettings;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;

@Singleton
public class DiskManagerFactory {
	
	public TorrentDiskManager getManager(TorrentContext context,
			BTDiskManagerMemento memento,
			boolean complete) {
		return new VerifyingFolder(context, 
				complete,
				memento, 
				BittorrentSettings.TORRENT_USE_MMAP.getValue() ?
						new MMDiskController<TorrentFile>() :
							new RAFDiskController<TorrentFile>());
	}
}
