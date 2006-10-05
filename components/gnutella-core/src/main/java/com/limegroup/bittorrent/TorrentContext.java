package com.limegroup.bittorrent;

import com.limegroup.bittorrent.disk.TorrentDiskManager;
import com.limegroup.gnutella.util.BitField;
import com.limegroup.gnutella.util.BitSet;

public interface TorrentContext {
	TorrentFileSystem getFileSystem();
	TorrentDiskManager getDiskManager();
	BTMetaInfo getMetaInfo();
	
	BitField getFullBitField();
	BitSet getFullBitSet();
	
	void initializeDiskManager(boolean complete);
}
