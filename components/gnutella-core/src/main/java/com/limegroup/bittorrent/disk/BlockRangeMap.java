/**
 * 
 */
package com.limegroup.bittorrent.disk;

import java.util.HashMap;
import java.util.Map;

import org.limewire.collection.IntervalSet;

import com.limegroup.bittorrent.BTInterval;

public class BlockRangeMap extends HashMap<Integer, IntervalSet> {
	
	private static final long serialVersionUID = 4006274480019024111L;

	BlockRangeMap() {
		super();
	}
	
	private BlockRangeMap(int size) {
		super(size);
	}
	
	public void addInterval(BTInterval in) {
		IntervalSet s = get(in.getBlockId());
		if (s == null) {
			s = new IntervalSet();
			put(in.getBlockId(),s);
		}
		s.add(in);
	}
	
	public void removeInterval(BTInterval in) {
		IntervalSet s = get(in.getBlockId());
		if (s == null)
			return;
		s.delete(in);
		if (s.isEmpty())
			remove(in.getBlockId());
	}
	
	public long byteSize() {
		long ret = 0;
		for (IntervalSet set : values()) 
			ret += set.getSize();
		return ret;
	}
	
	@Override
    public BlockRangeMap clone() {
		BlockRangeMap clone = new BlockRangeMap(size());
		for (Map.Entry<Integer, IntervalSet> e : entrySet())
			clone.put(e.getKey(), e.getValue().clone());
		return clone;
	}
}