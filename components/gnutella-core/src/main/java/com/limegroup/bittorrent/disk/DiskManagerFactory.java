package com.limegroup.bittorrent.disk;

import java.util.Map;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.TorrentFile;

public class DiskManagerFactory {
	private static DiskManagerFactory instance;
	public static DiskManagerFactory instance() {
		if (instance == null)
			instance = new DiskManagerFactory();
		return instance;
	}
	
	protected DiskManagerFactory(){}
	
	public TorrentDiskManager getManager(BTMetaInfo info,
			Map serializedData,
			boolean complete) {
		return new VerifyingFolder(info, 
				complete,
				serializedData, 
				new RAFDiskController<TorrentFile>());
	}
}
