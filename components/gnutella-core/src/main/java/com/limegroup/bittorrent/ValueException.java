
// Commented for the Learning branch

package com.limegroup.bittorrent;

import java.io.IOException;

/**
 * Throw a ValueException if we can't find a part of a .torrent file that should be there.
 */
public class ValueException extends IOException {

	/** A long unique number that will identify this version of this object when it's serialized to disk. */
	private static final long serialVersionUID = 3990038438042291913L;

	/**
	 * Make a new ValueException to throw because we looked for a part of a .torrent file, and couldn't find it.
	 */
	public ValueException() {

		// Just call the IOException constructor
		super();
	}

	/**
	 * Make a new ValueException to throw because we looked for a part of a .torrent file, and couldn't find it.
	 * 
	 * @param arg0 A String message to keep in the exception
	 */
	public ValueException(String arg0) {

		// Have the IOException constructor save the given String message
		super(arg0);
	}
}
