
// Commented for the Learning branch

package com.limegroup.gnutella.downloader;

import java.io.Serializable;

import com.limegroup.gnutella.ByteOrder;

/**
 * An Interval object clips out a range within a file.
 * 
 * A file composed of bytes of data looks like this:
 * 
 *       bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
 *               iiiiiiiiiiiii
 * low   ------->
 * high  ------------------->
 * 
 * An Interval object contains two indices, named low and high.
 * Together, they clip out a portion of the file, describing a specific interval.
 * low and high are measured in bytes from the start of the file.
 * 
 * low and high are inclusive on both ends.
 * low points to the start of the interval.
 * high doesn't point to the end of the interval, rather, it points to the last byte in the interval.
 * 
 * low has to be less than or equal to high.
 * if low equals high, the interval is the single byte both point to.
 */
public class Interval implements Serializable {

    /**
     * A long unique number that can identify an Interval object that has been written to a file on the disk.
     * Change this number when Interval changes, not breaking compatibility.
     */
    static final long serialVersionUID = -2562093104400487554L;

    /*
     * Always, low has to be less than or equal to high.
     */

    /**
     * The index of the start of the interval of data in the file.
     * 
     * low is the distance in bytes from the start of the file to the start of the interval.
     * The first byte in the file has the index 0.
     */
    public final int low;

    /**
     * The index of the end of the interval of data in the file.
     * The interval includes the byte high points to.
     * 
     * high is the distance in bytes from the start of the file to the last byte of the interval.
     * The first byte in the file has the index 0.
     */
    public final int high;

    /**
     * Make a new Interval object that clips out a range of data in a file.
     * 
     * low must be less than or equal to high.
     * low and high are passed as long numbers, but have to be small enough to fit into int numbers.
     * 
     * @param low  The distance in bytes from the start of the file to the start of the interval
     * @param high The distance in bytes from the start of the file to the last byte of the interval
     */
    public Interval(long low, long high) {

    	// Make sure low is less than or equal to high
    	if (high < low) throw new IllegalArgumentException("low: " + low + ", high: " + high);

    	// Make sure high and low are small enough to fit into int variables
        if (low < 0) throw new IllegalArgumentException("low < min int:" + low);
        if (high > Integer.MAX_VALUE) throw new IllegalArgumentException("high > max int:" + high);

        // Save the given indices
        this.low = (int)low; // Cast them to int, we made sure they're small enough
        this.high = (int)high;
    }

    /**
     * Make a new Interval object that clips out a single byte in a file.
     * the given index is a long, but must be small enough to fit safely in an int.
     * 
     * @param singleton The index of the byte that will be the whole interval
     */
    public Interval(long singleton) {

    	// Make sure the given index is small enough to fit in an int
        if (singleton < Integer.MIN_VALUE) throw new IllegalArgumentException("singleton < min:" + singleton);
        if (singleton > Integer.MAX_VALUE) throw new IllegalArgumentException("singleton > max int:" + singleton);

        // Save the given index as low and high, this clips out a single byte
        this.low = (int)singleton;
        this.high = (int)singleton;
    }

    /**
     * Determine if this Interval is entirely within a given Interval.
     * 
     * @return True if the bounds of this Interval are at or witin the given other Interval
     */
    public boolean isSubrange(Interval other) {

    	// Return true if the bounds of this Interval are at or within the given other Interval
        return
        	this.low >= other.low && // This Interval begins at or within the given Interval, and
        	this.high <= other.high; // This Interval ends at or before the given Interval
    }

    /**
     * Express this Interval object as text.
     * If the Interval is just 1 byte big, composes text with that byte's index, like "5".
     * If the Interval is a range of bytes, composes text like "5-10", this describes the 6 bytes between 5 and 11.
     * 
     * @return A String
     */
    public String toString() {

    	// Compose text with the low and high ranges of this Interval object
        if (low == high) return String.valueOf(low);
        else             return String.valueOf(low) + "-" + String.valueOf(high);
    }

    /**
     * Determine if this Interval is the same as a give one.
     * 
     * @param o The Interval object to compare this one to
     * @return  True if they are the same, false if different
     */
    public boolean equals(Object o) {

    	// Make sure the given object is also an Interval
        if (!(o instanceof Interval)) return false;

        // Return true if the low and high ranges match
        Interval other = (Interval)o;
        return low == other.low && high == other.high;
    }

    /**
     * Write this Interval object's low and high indices in a byte array.
     * 
     * Makes an 8-byte array like LLLLHHHH.
     * LLLL and HHHH are 4-byte ints in big endian order.
     * LLLL is the low index, and HHHH is the high index.
     * 
     * @return A byte array
     */
    public byte[] toBytes() {

    	// Make
    	byte[] res = new byte[8]; // Make an array 8 bytes big
    	toBytes(res, 0);
    	return res;
    }

    /**
     * Write the low and high ranges of this Interval object to a given byte array.
     * Writes them like LLLLHHHH at the offset in the array.
     * Each is a 4-byte int in big endian order.
     * 
     * @param dest   A destination byte array this toBytes() method will write data in
     * @param offset The distance into the destionation byte array where toBytes() will start writing
     */
    public void toBytes(byte[] dest, int offset) {

    	// Write the low range as a 4-byte int, followed by the high range as a 4-byte int
        ByteOrder.int2beb(low, dest, offset);
        ByteOrder.int2beb(high, dest, offset + 4); // Add 4 to move beyond the low range we wrote first
    }
}
