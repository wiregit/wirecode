
package com.limegroup.gnutella.upelection;

import com.sun.java.util.collections.Comparator;

import com.limegroup.gnutella.Connection;

	/**
	 * compares leafs based their potential for
	 * being good ultrapeers.  
	 *
	 * The most important one is uptime.  The number of 
	 * shared files are also taken in account.
	 * 
	 * Proposed formula:
	 * score = uptime(minutes) - sharedFiles/4
	 * 
	 * Example: 
	 * a node has been up for 7 hours, sharing 400 files.
	 * score = 7*60 - 100 = 320
	 * 
	 * will score better than a node that has been up for 8 hours 
	 * but is sharing 800 files.
	 * score = 8*60 - 200 = 280
	 *
	 */
public class LeafComparator implements Comparator {
	
	
	public int compare(Object a, Object b){
		if (a==null)
			if (b==null)
				return 0;
			else
				return -1;
		if (b==null)
			return 1;
		
		Connection conn1 = (Connection)a;
		Connection conn2 = (Connection)b;
			
		int score1 = conn1.getUptime() - conn1.getCandidateHandler().getFileShared()/4;
		
		int score2 = conn2.getUptime() - conn2.getCandidateHandler().getFileShared()/4;
		
		return score1-score2;
			
	}
		
	
}
