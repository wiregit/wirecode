
// Commented for the Learning branch

package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * A BEDictionary object reads a bencoded dictionary that starts "d" and ends "e" from a given channel and parses it into Java objects in a HashMap.
 * A bencoded dictionary looks like this:
 * 
 * d
 *  5:color     5:green
 *  4:year      i2006e
 *  9:platforms l 3:mac 7:windows 5:linux e
 *  6:flavor    4:lime
 * e
 * 
 * The dictionary entries are pairs of keys and values.
 * The keys are all bencoded strings.
 * The values can be any bencoded object.
 * Above, the keys "color" and "flavor" map to string values, while "year" maps to a number.
 * The key "platforms" maps to a list.
 * A key can even map to another dictionary.
 * 
 * BEDictionary produces a Java HashMap of keys and values.
 * The keys are Java String objects that hold the name of each key.
 * The values are Java objects specific to the type of object stored under the key in the dictionary.
 * 
 * BEDictionary extends BEList.
 * BEDictionary doesn't have a handleRead() method.
 * When you call handleRead() on a BEDictionary, the call goes to BEList.handleRead().
 * When that handleRead() method calls getNewElement(), control returns here and handleRead() gets a new BEEntry object.
 */
class BEDictionary extends BEList {

	/**
	 * Make a new BEDictionary object that can read and parse a bencoded dictionary.
	 * 
	 * @param chan The channel this new object can read bencoded data from
	 */
    BEDictionary(ReadableByteChannel chan) {

    	// Save the channel in this object
        super(chan);
    }

    /**
     * Determine what kind of Token object this is, and what kind of Java object it will parse.
     * 
     * @return Token.DICTIONARY, the code number for a BEDictionary object that will produce a Java HashMap
     */
    public int getType() {

    	// Return the Token.DICTIONARY code
        return DICTIONARY;
    }

    /**
     * Make the object this BEDictionary will fill with BEEntry objects that hold keys and values, and return.
     * 
     * @return A new empty HashMap
     */
    protected Object createCollection() {

    	// Make and return a new empty HashMap for this BEDictionary to parse its values into
        return new HashMap();
    }

    /**
     * Add a key and value we parsed to the result HashMap this BEDictionary is preparing.
     * 
     * @param o A BEEntry object with a string key and object value
     */
    protected void add(Object o) {

    	// Add the key and value of the given BEEntry to the result HashMap
        Map m = (Map)result;    // result is a HashMap object that createCollection() made
        BEEntry e = (BEEntry)o; // The given Object is actually a BEEntry
        m.put(e.key, e.value);  // Add the key and value from the BEEntry to the HashMap
    }

    /**
     * Make a new BEEntry object that will read and parse a key and value pair of bencoded data.
     * Control comes here when BEList.handleRead() calls getNewElement() and this object is actually a BEDictionary.
     * 
     * @return A new BEEntry object that will read and parse a key and value pair of bencoded data
     */
    protected Token getNewElement() {

    	// Make a new BEEntry object that will read and parse a key and value pair of bencoded data
        return new BEEntry(chan);
    }

    /**
     * A BEEntry object can read and parse a single entry in a bencoded dictionary.
     * For instance, a dictionary might look like this:
     * 
     * d
     *  5:color  5:green
     *  6:flavor 4:lime
     * e
     * 
     * Make a BEEntry object to parse one entry, like "5:color5:green".
     * When it's done, the String BEEntry.key will be "color", and the Object BEEntry.value will be the String "green".
     */
    private static class BEEntry extends Token {

        /** The BEString object that will parse the key name, like "5:color". */
        private BEString keyToken;

        /** The String key, like "color". */
        private String key;

        /** The object that extends Token that will parse the value object, like a BEString to parse "5:green". */
        private Token valueToken;

        /** The value object, like the String "green". */
        private Object value;

        /** True if this is the last entry in the dictionary. */
        private boolean lastEntry;

        /**
         * Make a new BEEntry object that will parse a single key and value in the middle of a bencoded dictionary.
         * 
         * @param chan The channel this object can read bencoded data from
         */
        BEEntry (ReadableByteChannel chan) {

        	// Save the given channel in this object
        	super(chan);

        	// When this BEEntry object is done reading one key and value, the Java object it made is itself
            result = this;
        }

        /**
         * The "NIODispatch" thread will call this handleRead() method when this BEEntry object can read more bencoded data from its channel.
         * 
         * A key and value pair in a bencoded dictionary looks like "5:color5:green".
         * keyToken is a BEString object that will read "5:color" into the String key "color".
         * valueToken is an object that extends token that will read "5:green" into the Object value "green".
         * This handleRead() method tries to read the key and value.
         * You can tell how far it got by whether or not key and value are null when it returns.
         */
        public void handleRead() throws IOException {

        	// If we haven't read the key, like "5:color", yet
        	if (keyToken == null && // We don't have a BEString object to parse the text key yet, and
        		key == null) {      // We don't have the parsed String key, like "color" yet either

        		// Read the next character from the channel
                Token t = getNextToken(chan);

                // getNextToken read a character from the channel, and made an object that extends Token that can read and parse it
                if (t != null) {

                	// The character was a numeral "0" through "9", a string key like "5:color" is in the channel next
                    if (t instanceof BEString) {

                    	// Save the BEString object as this BEEntry's keyToken, and keep going in this method
                        keyToken = (BEString)t;

                    // The character was "e", marking the end of the whole dictionary
                    } else if (t == Token.TERMINATOR) {

                    	// Set the last entry flag, and leave now
                        lastEntry = true;
                        return;

                    // The character was something else
                    } else {

                    	// Bencoded dictionary entries have to start with a bencoded string key, or "e" for the end
                    	throw new IOException("invalid entry - key not a string");
                    }

                // The channel couldn't even give us 1 character
                } else {

                	// Try again next time
                	return;
                }
            }

        	/*
        	 * If control reaches here, we have a BEString object named keyToken parsing the key.
        	 */

        	// We have a BEString named keyToken parsing the key like "5:color", but it hasn't finished yet
            if (key == null) {

            	// Tell it to read more bencoded data from its channel
                keyToken.handleRead();

                // It's done parsing the String "color"
                if (keyToken.getResult() != null) {

                	// Save the String key
                    key = new String((byte[])keyToken.getResult(), Token.ASCII);

                    // We don't need the BEString object that parsed it for us any longer
                    keyToken = null;

                // It hasn't read the whole string yet
                } else {

                	// Try to read more next time
                	return;
                }
            }

            /*
             * If control reaches here, we have read the String key in the dictionary entry this BEEntry object is parsing.
             */

            // We haven't read the value yet
            if (valueToken == null && value == null) {

            	// Read the next character of bencoded data, and get an object that extends Token that will read and parse it into a Java object
                Token t = getNextToken(chan);
                if (t != null) valueToken = t; // Save it
                else return; // The channel couldn't give us one character, try to figure out the value's type next time the program calls handleRead()
            }

            // We've created the valueToken that will parse the value, but it hasn't parsed the whole value yet
            if (value == null) {

            	// Tell valueToken to read more bencoded data, and get the object it will make
                valueToken.handleRead();
                value = valueToken.getResult();

                // It read "e" for end, which doesn't make sense, because there should be a value for each key entry
                if (value == Token.TERMINATOR) throw new IOException("missing value");

                // Clear our reference to the Java object valueToken read
                if (value != null) valueToken = null;

            // We've parsed and read the whole value
            } else {

            	// Code shouldn't call handleRead() after we're done parsing the value
            	throw new IllegalStateException("token is done - don't read to it " + key + " " + value);
            }
        }

        /**
         * Determine if this BEEntry object is finished reading a key and value from a bencoded dictionary.
         * 
         * @return True if key and value are objects we parsed and made.
         *         False if one or both of them are still null, and you need to call handleRead() some more.
         */
        protected boolean isDone() {

        	// Return true if handleRead() has made the key and value objects
            return key != null && value != null;
        }

        /**
         * Get this BEEntry object, which holds the key and value it read and parsed.
         * 
         * @return A reference to this BEEntry object
         */
        public Object getResult() {

        	// If this BEEntry object read the terminating "e", return the TERMINATOR object
            if (lastEntry) return Token.TERMINATOR;

            // Call Token.getResult(), which returns result, which the BEEntry constructor set to this
            return super.getResult();
        }
    }
}
