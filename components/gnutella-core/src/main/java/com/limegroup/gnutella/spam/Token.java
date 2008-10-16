package com.limegroup.gnutella.spam;

import java.io.Serializable;

/**
 * From every RFD we can extract a set of tokens,
 */
public interface Token extends Serializable {
    
    public static enum Rating {
        USER_MARKED_SPAM, USER_MARKED_GOOD, CLEARED;
    };

	/**
     * Returns a rating between 0 and 1 representing the probability that
     * the file associated with this token is spam
     * 
     * @return the spam rating of this token
     */
	public float getRating();

	/**
	 * Adjusts the spam rating of this token
	 * 
	 * @param rating a rating as defined in the <tt>Token</tt> interface,
     *        which will influence the rating stored in this <tt>Token</tt>
	 * @throws IllegalArgumentException if the argument is not one of the
     *         ratings defined in the <tt>Token</tt> interface
	 */
	public void rate(Rating rating);
}
