package com.limegroup.gnutella;

/** 
 * A filter to eliminate Gnutella spam.  Each Gnutella connection
 * has its own SpamFilter.  (Strategy pattern.) Subclass to implement
 * custom spam filters.
 */
public class SpamFilter {
    /**
     * Returns a new instance of a SpamFilter subclass based on 
     * the current settings manager.  (Factory method)
     */
    public static SpamFilter newInstance() {
	//TODO: use setttings manager.
	return new DuplicateFilter();
    }

    /** 
     * Returns true iff this is considered spam and should not be processed. 
     * The default implementation always returns true; subclass to override.
     */
    public boolean allow(Message m) {
	return true;
    }
}
