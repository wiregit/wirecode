
package com.limegroup.gnutella.upelection;

import com.sun.java.util.collections.Comparator;


/**
 * the comparator needs to be overriden too because of 
 * decorator and null values support.
 */
class CandidatePriorityComparator implements Comparator {
    public int compare(Object extEndpoint1, Object extEndpoint2) {
    	
    	//afaics the contract doesn't provide for null elements. 
    	//however they are necessary in this case.
    	if (extEndpoint1 == null && extEndpoint2 == null) return 0;
    	if (extEndpoint1 == null && extEndpoint2 != null) return -1;
    	if (extEndpoint1 != null && extEndpoint2 == null) return 1;
    	
        Candidate a=(Candidate)extEndpoint1;
        Candidate b=(Candidate)extEndpoint2;
        return a.getUptime()-b.getUptime();
        
    }
}