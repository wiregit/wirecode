
// Commented for the Learning branch

package com.limegroup.bittorrent.messages;

import java.io.IOException;

/**
 * Throw a BadBTMessageException if you're parsing the data of a BitTorent message from a remote computer and run into a mistake.
 * A BadBTMessageException is a special type of IOException.
 */
public class BadBTMessageException extends IOException {

	/** A unique number that identifies this version of BadBTMessageException objects serialized to disk. */
	private static final long serialVersionUID = -9138724347393610325L;

	/**
	 * Make a new BadBTMessageException.
	 */
	public BadBTMessageException() {

		// Call the IOException constructor
		super();
	}

	/**
	 * Make a new BadBTMessageException.
	 * 
	 * @param s A message to keep in it
	 */
	public BadBTMessageException(String s) {

		// Give the message to the IOException constructor
		super(s);
	}
}
