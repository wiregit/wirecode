package com.limegroup.bittorrent;

import org.limewire.collection.BitField;
import org.limewire.collection.BitFieldSet;
import org.limewire.collection.BitSet;

import com.limegroup.bittorrent.disk.TorrentDiskManager;
import com.limegroup.gnutella.ProviderHacks;

public class BTContext implements TorrentContext {

	private final BTMetaInfo info;
	private final BitSet fullSet = new FullBitSet();
	private final BitField fullBitField;
	
	private TorrentDiskManager diskManager;
	
	public BTContext(BTMetaInfo info) {
		this.info = info;
		info.setContext(this);
		fullBitField = new BitFieldSet(fullSet, info.getNumBlocks());
		initializeDiskManager(false);
	}
	
	public TorrentDiskManager getDiskManager() {
		return diskManager;
	}

	public TorrentFileSystem getFileSystem() {
		return info.getFileSystem();
	}

	public BitField getFullBitField() {
		return fullBitField;
	}

	public BitSet getFullBitSet() {
		return fullSet;
	}

	public BTMetaInfo getMetaInfo() {
		return info;
	}

	public void initializeDiskManager(boolean complete) {
		diskManager = ProviderHacks.getDiskManagerFactory().getManager(this, 
				info.getDiskManagerData(), complete);
	}

	/**
	 * A bitset that has fixed size and every bit in it is set.
	 */
	private class FullBitSet extends BitSet {
		private static final long serialVersionUID = -2621319856548383315L;
		public void set(int i) {}
		public void clear(int i){}
		public boolean get(int i) {
			return true;
		}
		public int cardinality() {
			return info.getNumBlocks();
		}
		public int length() {
			return info.getNumBlocks();
		}
		public int nextSetBit(int i) {
			if (i >= cardinality())
				return -1;
			return i;
		}
	}
}
