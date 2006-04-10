package com.limegroup.bittorrent;

import com.limegroup.gnutella.downloader.Interval;

/**
 * An interval within a block. 
 */
public class BTInterval extends Interval {
	final Integer blockId;
	
	private int hashCode;

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
	
	public String toString() {
		return getId()+":"+super.toString();
	}
}
