
// Commented for the Learning branch

package com.limegroup.bittorrent;

import com.limegroup.gnutella.downloader.Interval;

/**
 * A BTInterval clips out a range of bytes within a numbered file piece on BitTorrent.
 * 
 * BitTorrent splits a file into many pieces.
 * The pieces are all the same size, except for the last one.
 * The piece size is 256 KB by default, and larger for very large files.
 * The first piece has piece number 0.
 * 
 * A BTInterval object specifies a numbered piece, and describes a range within that piece.
 * BTInterval extends Interval, which describes a range.
 * Interval has two indices, low and high, that point to the first and last byte that make up the range.
 * 
 * A range within a piece looks like this:
 * 
 * piece number  57
 *                  bbbbbbbbbbbbbbbbbbbbbbbbbbb
 *                        iiiiiii
 * low            6 ----->
 * high          12 ----------->
 * 
 * The piece number is blockId, and the indices are inherited from Interval and named low and high.
 * 
 * BitTorrent Request, Piece, and Cancel messages use the information a BTInterval message can hold.
 * The range that low and high clip out has to be within a single piece, and can't extend to more than one piece.
 */
public class BTInterval extends Interval {

	/** The piece number of the file block this interval is within. */
	final Integer blockId;

	/**
	 * The Java hash code we've calculated and saved for this BTInterval object.
	 * This hash code isn't the same as the SHA1 hash of any file data.
	 */
	private int hashCode;

	/**
	 * Make a new BTInterval object, which identifies a piece, and a range of bytes within it.
	 * 
	 * @param low  The distance in bytes from the start of the piece to the start of the interval
	 * @param high The distance in bytes from the start of the piece to the end of the interval
	 * @param id   The piece number the low and high indices are in
	 */
	public BTInterval(long low, long high, int id) {

		// Save the range in the Interval object
		super(low, high);

		// Wrap the given piece number in an Integer object, and save it
		blockId = new Integer(id);
	}

	/**
	 * Make a new BTInterval object, which identifies a piece, and a range of bytes within it.
	 * 
	 * @param other An Interval object that clips out a range measured from the start of a piece
	 * @param id    The piece number the range is in
	 */
	public BTInterval(Interval other, int id) {

		// Call the previous constructor, getting the low and high indices from the given Interval object
		this(other.low, other.high, id);
	}

	/**
	 * Make a new BTInterval object that identifies a single byte in a BitTorrent file.
	 * 
	 * @param singleton The distance in bytes from the start of a piece to the byte we want to identify
	 * @param id        The piece number the byte is in
	 */
	public BTInterval(long singleton, int id) {

		// Call the Interval constructor, giving it the distance to the single byte
		super(singleton); // Sets both low and high to the distance singleton, making a range that is a single byte

		// Save the piece number in this new BTInterval object
		blockId = new Integer(id);
	}

	/**
	 * Get the piece number this range is within.
	 * 
	 * @return The BitTorrent file piece number
	 */
	public int getId() {

		// Get the number we stored in the blockId Integer object
		return blockId.intValue();
	}

	/**
	 * Determine if this BTInterval object is the same as a given one.
	 * Compares their piece numbers, and their low and high ranges.
	 * 
	 * @return True if they have the same information, false if they are different.
	 */
	public boolean equals(Object other) {

		// Make sure the given object is a BTInterval object
		if (!(other instanceof BTInterval)) return false;

		// Make sure the ranges are in the same pieces
		BTInterval o = (BTInterval)other;
		if (getId() != o.getId()) return false; // The piece numbers are different

		// Return true if the low and high ranges are both the same
		return super.equals(other); // Calls Interval.equals()
	}

	/**
	 * Compute a hash code of this BTInternal object.
	 * Java will use this hash code to spread out BTInterval objects in a collections class.
	 * This isn't the same thing as the SHA1 hash of the file piece in the .torrent file.
	 * 
	 * @return The hash code Java needs of this BTInternal object
	 */
	public int hashCode() {

		// If we haven't computed the hash code yet
		if (hashCode == 0) {

			// Compute it, using prime numbers like 17 and 37
			hashCode = 17 * getId();
			hashCode *= 37 + low;
			hashCode *= 37 + high;
		}

		// Return the hash code we saved
		return hashCode;
	}

	/**
	 * Express this BTInternal object as a String.
	 * If this BTInterval object represents the range 5-10 in piece number 25, composes text like "25:5-10".
	 * 
	 * @return A String
	 */
	public String toString() {

		// Compose and return the text
		return getId() + ":" + super.toString();
	}
}
