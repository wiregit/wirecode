package com.limegroup.gnutella.spam;

import java.io.Serializable;

/**
 * From every RFD we can extract a set of tokens,
 */
public interface Token extends Serializable, Comparable<Token> {
	/**
	 * A Token representing a keyword
	 */
	public static final int TYPE_KEYWORD = 1;

	/**
	 * A Token representing an urn
	 */
	public static final int TYPE_URN = 2;

	/**
	 * A Token representing the file size
	 */
	public static final int TYPE_SIZE = 3;

	/**
	 * A Token representing the host address
	 */
	public static final int TYPE_ADDRESS = 4;

	/**
	 * A Token representing a xml keyword
	 */
	public static final int TYPE_XML_KEYWORD = 5;

    /**
     * A Token representing the vendor
     */
    public static final int TYPE_VENDOR = 6;

	/**
	 * the user marked this token as spam
	 */
	public static final int RATING_USER_MARKED_SPAM = 1;

	/**
	 * the spam filter decided this token is spam
	 */
	public static final int RATING_SPAM = 2;

	/**
	 * the user decided this token is okay.
	 */
	public static final int RATING_USER_MARKED_GOOD = 3;

	/**
	 * the filter decided this token is okay.
	 */
	public static final int RATING_GOOD = 4;

	/**
	 * this token is to be cleared completely
	 */
	public static final int RATING_CLEARED = 5;

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
	public void rate(int rating);

	/**
	 * Returns the type of this token.
	 * 
	 * @return one of the constant types as defined in the <tt>Token</tt>
	 *         interface.
	 */
	public int getType();

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
