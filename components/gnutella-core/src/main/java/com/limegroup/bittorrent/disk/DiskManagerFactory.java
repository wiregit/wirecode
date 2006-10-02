package com.limegroup.bittorrent.disk;

import java.io.Serializable;

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
			Serializable serializedData,
			boolean complete) {
		return new VerifyingFolder(info, 
				complete,
				serializedData, 
				new RAFDiskController<TorrentFile>());
	}
}
