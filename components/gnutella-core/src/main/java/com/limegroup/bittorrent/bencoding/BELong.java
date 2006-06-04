
// Commented for the Learning branch

package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * A BELong object reads bencoded data like "i87e" and parses it into a number.
 * You can also use a BELong to read and parse the length number at the start of a string, like "52:".
 */
class BELong extends Token {

	/** "-", the ASCII byte of a minus sign. */
    private static final byte MINUS;

    // Java runs this code before control enters a method in this class
    static {

    	// Conver the "-" String into a byte array using default ASCII encoding, and read the first byte
    	byte minus = 0;
        try { minus = "-".getBytes(ASCII)[0]; } catch (UnsupportedEncodingException impossible) {}
        MINUS = minus;
    }

    /** A StringBuffer that holds the digits of the number, like "124" for the bencoded data "i124e". */
    private StringBuffer sb = new StringBuffer();

    /** A byte array that holds a single byte. */
    private byte[] currentByte = new byte[1];

    /**
     * A ByteBuffer that holds a single byte.
     * handleRead() uses buf to read bencoded data from our channel one character at a time.
     */
    private ByteBuffer buf = ByteBuffer.wrap(currentByte);

    /** -1 for negative values, 0 for 0, 1 for positive values. */
    private int multiplier = 1;

    /** When we've read the terminator like "e" or ":" from the channel, handleRead() sets done to true. */
    private boolean done;

    /**
     * The charcter we're looking for that ends the number.
     * If the bencoded data is like "i87e", terminator will be "e".
     * If the bencoded data is like "5:hello", terminator will be ":".
     */
    private final byte terminator;

    /**
     * Make a new BELong object that will read bencoded data like "i87e" from the given channel and turn it into a number.
     * 
     * @param chan The ReadableByteChannel this new BELong object will read bencoded data from.
     *             The program has already read the leading "i" from this channel.
     *             The new BELong object will read the number and the trailing "e", but not take anything else out of the channel.
     */
    BELong(ReadableByteChannel chan) {

    	// Call the next constructor
    	this(
    	    chan,     // The channel we can read bencoded data from
    	    E,        // "e", when we read a "e", that's the end of the number
    	    (byte)0); // No, we haven't read a digit like "0" through "9" from the channel yet
    }

    /**
     * Make a new BELong object that will read a bencoded number from the given channel like "i23e" or "53:".
     * In the "i23e" case, we've already read "i" from the channel.
     * In the "53:" case, we may have already read the first digit "5", if so, it's passed here as firstByte.
     * 
     * @param chan       A ReadableByteChannel the new BELong can read from to get more data.
     * @param terminator The character we'll look for to mark the end of the numerals, like "e" or ":".
     * @param firstByte  If you already ready the first digit from the channel, pass it here.
     *                   If not, pass 0 for firstByte.
     */
    BELong(ReadableByteChannel chan, byte terminator, byte firstByte) {

    	// Save the channel this new object will read data from
        super(chan);

        // Save the character we'll watch for to end the number
        this.terminator = terminator;

        // If the caller already pulled the first byte of this number out of the channel
        if (firstByte != 0) {

        	// Make sure the character the caller read was a digit, "0" through "9"
            if (firstByte < ZERO || firstByte > NINE) throw new IllegalArgumentException("invalid first byte");

            // Add the number to the sb string of numerals we're building
            sb.append(firstByte - ZERO); // If firstByte is "5", subtract "0" to get the number 5, then append() turns that number back into text
        }
    }

    /**
     * The "NIODispatch" thread calls handleRead() when the channel this BELong object gets its bencoded data from has some more.
     * 
     * Bencoded numbers look like "i87e" for 87, "i0e" for 0, and "i-32e" for negative 32.
     * BELong can also parse the number that tells the length of a bencoded string, like "52:".
     * 
     * Loops, reading individual characters of bencoded data from the channel this BELong was given when it was made.
     * Looks for a leading "-", makes sure there are no leading 0s, and stops on the terminating character "e" or ":".
     * Puts the numerals into a StringBuffer named sb.
     * When it reaches the terminating "e" or ":", converts the numerals into a number and saves it as a Long object named result.
     * At this point, isDone() returns true, and getResult() returns the Long object.
     */
    public void handleRead() throws IOException {

    	// If we've read the complete number like "i87e", the program shouldn't call handleRead() on this BELong object again
        if (done) throw new IllegalStateException("this token is done.  Don't read it!");

        // Loop, reading individual characters from the channel
        while (true) {

            try {

            	// Get the next character of bencoded data from the channel
                int read = chan.read(buf);
                if (read == -1) throw new IOException("channel closed before end of integer token");
                else if (read == 0) return; // The channel doesn't have any more data for us right now, this method will continue the next time it's called

            } finally {

            	// Mark the ByteBuffer empty for the next time, we can still get to the byte we read with currentByte[0]
            	buf.clear();
            }

            // The character isn't a numeral "0" through "9"
            if (currentByte[0] < ZERO || currentByte[0] > NINE) {

            	// It's "-", a minus sign at the front of a bencoded number like "i-25e"
                if (currentByte[0] == MINUS && sb.length() == 0 && multiplier != -1) {

                	// Set the multiplier to -1 so we'll make the number we parse negative
                	multiplier = -1;

                // We read the terminating character like "e" or ":", and we have found some numerals before this
                } else if (currentByte[0] == terminator && sb.length() != 0) {

                    try {

                    	// Compute the result number
                        result = new Long(Long.parseLong(sb.toString()) * multiplier);

                    // There was an error reading the text as a number
                    } catch (NumberFormatException impossible) {
                        throw new IOException(impossible.getMessage());
                    }

                    // We don't need to the StringBuffer anymore
                    sb = null;

                    // Have isDone() return true after this so the caller can get the Long object with getResult()
                    done = true;
                    return;

                // It's not "0" through "9", or "-", or "e" or ":"
                } else {

                	// The bencoded data has a mistake, throw an exception
                	throw new IOException("invalid integer");
                }

            // The character we read is "0"
            } else if (currentByte[0] == ZERO) {

            	// We haven't read any numerals yet
                switch (sb.length()) {
                case 0:

                	// Don't allow the number "i-0e"
                    if (multiplier == -1) throw new IOException("negative 0");

                    // The number is "i0e", which is 0
                    multiplier = 0;
                    break;

                // We've read some numerals before running into this 0
                case 1:

                	// Don't allow leading 0s, like "i050e" for 50
                    if (multiplier == 0) throw new IOException("leading 0s");
                }

                // Add the "0" to sb, which hold the numerals we've read
                sb.append(0);

            // The character we read is "1" through "9"
            } else {

            	// Make sure we haven't found a leading 0 before this
                if (multiplier == 0) throw new IOException("leading 0s - wrong");

                // Add the numeral to the string of them we've read
                sb.append(currentByte[0] - ZERO);
            }
        }
    }

    /**
     * Find out if this BELong object has read the whole bencoded number like "i87e".
     * If it has, you can call getResult() to get the number in a Long object.
     * 
     * @return True if this BELong has read and parsed the whole bencoded number.
     *         False if you still need to call handleRead() to get it to read and parse more bencoded data from its channel.
     */
    protected boolean isDone() {

    	// Return the done flag that handleRead() will set to true when it's done
        return done;
    }

    /**
     * Find out what type of Token object this is, and what kind of Java object it will parse and make.
     * 
     * @return Token.LONG, this is a BELong that will parse a Number object
     */
    public int getType() {

    	// Return the LONG code number for the number type
    	return LONG;
    }
}
