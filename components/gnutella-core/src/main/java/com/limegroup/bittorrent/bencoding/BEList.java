
// Commented for the Learning branch

package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * A BEList object reads a bencoded list that starts "l" and ends "e" from a given channel and parses it into Java objects in an ArrayList.
 * A bencoded list looks like this:
 * 
 * l
 *  5:hello
 *  i87e
 *  23:here is a third element
 * e
 * 
 * Between "l" and "e" are any number of other bencoded elements.
 * The list items can be strings, numbers, or even other lists.
 * 
 * Here's how to use BEList to parse a bencoded list into Java objects.
 * You've already read "l" from the channel, telling you it's a list that will come next.
 * Make a new BEList object, giving it the channel it can read the rest of the bencoded data from.
 * It will make a new empty ArrayList, this is the object that it will return when it's done.
 * Call handleRead() on it every time there's more bencoded data for it to read in the channel.
 * 
 * handleRead() makes a Token object called currentElement, it reads and parses the first list item.
 * In the example above, currentElement will be a BEString that reads "5:hello" and turns it into a String.
 * When currentElement is done, handleRead() gets the object it made and adds it to the result ArrayList.
 * After all the elements, handleRead() will read the "e".
 * When it happens, it sets done to true so isDone() starts returning true.
 * Then, you can call getResult() to access the ArrayList with all the parsed objects.
 */
class BEList extends Token {

    /** When this BEList object has read and parsed the whole list, it will set done to true. */
    protected boolean done;

    /** The current list element we're reading and parsing. */
    protected Token currentElement;

    /**
     * Make a new BEList object that can read a bencoded list.
     * 
     * @param chan The channel this new BEList can read bencoded data from
     */
    BEList(ReadableByteChannel chan) {

    	// Save the channel in this object
        super(chan);

        // Make a new empty ArrayList, this is the object we'll fill and getResult() will return when we're done
        result = createCollection();
    }

    /**
     * Make a new empty ArrayList.
     * Only the BEList constructor above calls this method.
     * 
     * @return An ArrayList
     */
    protected Object createCollection() {

    	// Make a new ArrayList and return it
        return new ArrayList();
    }

    /**
     * Add a given object we just parsed to the result ArrayList.
     * Only handleRead() below calls this.
     * 
     * @param o An object we just parsed
     */
    protected void add(Object o) {

    	// Add the given object to our ArrayList
        ((List)result).add(o);
    }

    /**
     * Read the next bencoded object from the channel, returning a type-specific object like a BEString that can read, parse, and make the object.
     * Only handleRead() below calls this.
     * 
     * @return An object that extends Token, like a BEString, that will parse the next bencoded piece of data in the list
     */
    protected Token getNewElement() throws IOException {

    	// Give the channel to getNextToken(), which will read 1 character from it and return a type-specific parsing object
        return getNextToken(chan);
    }

    /**
     * The "NIODispatch" thread will call handleRead() when there's more bencoded in data for us to read and parse.
     * 
     * A bencoded list looks like this:
     * 
     *   l 3:red 5:green 4:blue e
     * 
     * We've already read the "l" for list from the channel, we did that to determine it's a list next and we need this BEList object to decode it.
     * handleRead() calls getNewElement() to make a new object that extends Token that will parse the first element, "3:red".
     * When that BEString is done, code here adds it to our result ArrayList.
     * When we hit the "e", getNewElement() returns the Token.TERMINATOR object, and this method sets done to true.
     * 
     * BEDictionary extends BEList, and doesn't have a handleRead() method.
     * This means that when you call handleRead() on a BEDictionary, control will come here.
     */
    public void handleRead() throws IOException {

    	// Code shouldn't call handleRead() after this BEList object has read the "e" and parsed the result
        if (isDone()) throw new IllegalStateException("token is done, don't read to it");

        // Loop until we read the "e" that ends the list, or we have a current parser returning because it needs more data
        while (true) {

        	// If we don't have an object that extends Token reading the next bencoded object, make one
            if (currentElement == null) currentElement = getNewElement(); // If this is a BEDictionary object, calls BEDictionary.getNewElement(), which returns a BEEntry object

            // The channel couldn't even produce 1 character to tell us what kind of bencoded object is next
            if (currentElement == null) return; // Read more later

            // getNextToken() read "e", we've finished the list
            if (currentElement.getResult() == Token.TERMINATOR) {

            	// We're done with the entire list
                done = true;
                return;
            }

            // Have the Token object we made read more bencoded data to get closer to finishing the list item it's parsing
            currentElement.handleRead();
            Object result = currentElement.getResult(); // getResult() returns null if it's not done yet
            if (result == null) return; // Read more later

            // currentElement parsed "e" (do)
            if (result == Token.TERMINATOR) {

            	// We're done with the entire list
                done = true;
                return;
            }

            // Add the Java object we just parsed to our ArrayList of them
            add(result);
            currentElement = null; // Set currentElement to null so we'll make a new one next time
        }
    }

    /**
     * Find out if this BEList object is finished reading its bencoded list from its channel, and has parsed it into a ArrayList object of other objects.
     * 
     * @return True, this object has read the entire bencoded list from the channel.
     *         False, call the handleRead() method more to get this object to read more bencoded data from its channel to reach the "e".
     */
    protected boolean isDone() {

    	// Return true if handleRead() made a Token object that parsed the "e" that ends the list
    	return done;
    }

    /**
     * Determine what kind of Token object this is, and what kind of Java object it will parse.
     * 
     * @return Token.LIST, the code number for a BEList object that will produce a Java ArrayList
     */
    public int getType() {

    	// Return the Token.LIST code
        return LIST;
    }
}
