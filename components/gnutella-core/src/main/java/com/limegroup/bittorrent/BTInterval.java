package com.limegroup.bittorrent;

import com.limegroup.gnutella.downloader.Interval;

/**
 * An interval within a block. 
 */
public class BTInterval extends Interval {
	private static final long serialVersionUID = 6565199693843714608L;

	private final int blockId;
	
	private int hashCode;

	public BTInterval(long low, long high, int id) {
		super(low, high);
		if (id < 0)
			throw new IllegalArgumentException("negative id");
		blockId = id;
	}
	
	public BTInterval(Interval other, int id) {
		this(other.low, other.high, id);
	}

	public BTInterval(long singleton, int id) {
		super(singleton);
		blockId = id;
	}
	
	public int getId() {
		return blockId;
	}
	
	public boolean equals(Object other) {
		if (! (other instanceof BTInterval))
			return false;
		
		BTInterval o = (BTInterval) other;
		if (getId() != o.getId())
			return false;
		
		return super.equals(other);
	}
	
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = 17 * getId();
			hashCode *= 37 + low;
			hashCode *= 37 + high;
		}
		return hashCode; 
	}
	
	public int getBlockId() {
		return blockId;
	}
	
	public String toString() {
		return getId()+":"+super.toString();
	}
}
