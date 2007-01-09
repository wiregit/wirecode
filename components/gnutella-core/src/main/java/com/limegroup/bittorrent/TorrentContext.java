package com.limegroup.bittorrent;

import org.limewire.collection.BitField;
import org.limewire.collection.BitSet;

import com.limegroup.bittorrent.disk.TorrentDiskManager;

public interface TorrentContext {
	TorrentFileSystem getFileSystem();
	TorrentDiskManager getDiskManager();
	BTMetaInfo getMetaInfo();
	
	BitField getFullBitField();
	BitSet getFullBitSet();
	
	void initializeDiskManager(boolean complete);
}
