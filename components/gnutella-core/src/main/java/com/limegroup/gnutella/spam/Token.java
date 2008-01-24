package com.limegroup.gnutella.spam;

import java.io.Serializable;

/**
 * From every RFD we can extract a set of tokens,
 */
public interface Token extends Serializable, Comparable<Token> {
    
    public static enum TokenType {
        KEYWORD, URN, SIZE, ADDRESS, XML_KEYWORD, VENDOR;

    };

    public static enum Rating {
        USER_MARKED_SPAM, PROGRAM_MARKED_SPAM, USER_MARKED_GOOD, PROGRAM_MARKED_GOOD, CLEARED;
    };

	/**
     * This method returns the spam rating of this token.
     * 
     * @return a float between 0 and 1 that represents the probability this
     *         token is spam. Returns 0 if we believe this is not spam and 1 if
     *         we believe this is spam.
     */
	public float getRating();

	/**
	 * This method allows to influence the spam rating of a token.
	 * 
	 * @param rating
	 *            an int that must be one of the constant ratings defined in the
	 *            <tt>Token</tt> interface and that will be added to the
	 *            internal rating as stored in this <tt>Token</tt>
	 * @throws IllegalArgumentException
	 *             if the argument is not one of the ratings defined in the
	 *             <tt>Token</tt> interface
	 */
	public void rate(Rating rating);

	/**
	 * Returns the type of this token.
	 * 
	 * @return one of the constant types as defined in the <tt>Token</tt>
	 *         interface.
	 */
	public TokenType getType();

	/**
	 * Used to determine which tokens should be kept when there are too
     * many tokens in RatingTable.  The items with the lowest "importance"
     * values are discarded.
	 * 
	 * @return the importance metric value for this token  
	 */
	public double getImportance();
	
	/**
	 * increase the age of the token (measured in limewire sessions)
	 */
	public void incrementAge();
}
