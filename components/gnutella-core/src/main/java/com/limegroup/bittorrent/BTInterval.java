package com.limegroup.bittorrent;

import com.limegroup.gnutella.downloader.Interval;

/**
 * An interval within a block. 
 */
public class BTInterval extends Interval {
	final Integer blockId;

	public BTInterval(long low, long high, int id) {
		super(low, high);
		blockId = new Integer(id);
	}
	
	public BTInterval(Interval other, int id) {
		this(other.low, other.high, id);
	}

	public BTInterval(long singleton, int id) {
		super(singleton);
		blockId = new Integer(id);
	}
	
	public int getId() {
		return blockId.intValue();
	}
}
