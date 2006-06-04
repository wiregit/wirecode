
// Commented for the Learning branch

package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The BEncoder class has code that can convert Java objects into bencoded data for BitTorrent.
 * Call BEncoder.encode(OutputStream, Object) to bencode a given Object and write the bencoded data to the given OutputStream.
 * 
 * BitTorrent uses a simple and extensible data format called bencoding.
 * More information on bencoding is on the Web at:
 * http://en.wikipedia.org/wiki/Bencoding
 * http://www.bittorrent.org/protocol.html in the section titled "The connectivity is as follows".
 * 
 * Bencoded data is composed of strings, numbers, lists, and dictionaries.
 * Strings are prefixed by their length, like "5:hello".
 * Numbers are written as text numerals between the letters "i" and "e", like "i87e".
 * You can list any number of bencoded pieces of data between "l" for list and "e" for end.
 * A dictionary is a list of key and value pairs between "d" and "e".
 * The keys have to be strings, and they have to be in alphabetical order.
 */
public class BEncoder {

	/**
	 * The BEncoder() constructor is marked private to prevent anyone from making a BEncoder object.
	 * Just use the public static methods like encodeString() instead.
	 */
    private BEncoder() {}

    /**
     * Bencode the given byte array to the given OutputStream.
     * 
     * Writes the length, a colon, and then the text.
     * For example, the string "hello" becomes the bencoded bytes "5:hello".
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param b      The byte array to bencode and write
     */
    public static void encodeByteArray(OutputStream output, byte[] b) throws IOException {

    	// Write the length, a colon, and the byte array, like "5:hello"
        String length = String.valueOf(b.length);   // Find out how long the data is, and convert that number into a String
        output.write(length.getBytes(Token.ASCII)); // Write the text to the given OutputStream
        output.write(BEString.COLON);               // Write the ":"
        output.write(b);                            // Write the data
    }

    /**
     * Bencode the given number to the given OutputStream.
     * 
     * Writes the base-10 numerals of the number between the letters "i" and "e".
     * For example, the if n is 87 encodeInt() will write "i87e" to the OutputStream.
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param n      The number to bencode and write
     */
    public static void encodeInt(OutputStream output, Number n) throws IOException {

    	// Write the number between an "i" and "e", like "i87e"
        String numerals = String.valueOf(n.longValue()); // Convert the number into a String
        output.write(Token.I);                           // Write the "i"
        output.write(numerals.getBytes(Token.ASCII));    // Write the numerals
        output.write(Token.E);                           // Write the "e"
    }

    /**
     * Bencodes the given List to the given OutputStream.
     * Pass any Java object that extends java.util.List for the list parameter.
     * 
     * Writes "l" for list, the bencoded-form of each of the given objects, and then "e" for end.
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param list   A Java List object to bencode and write
     */
    public static void encodeList(OutputStream output, List list) throws IOException {

    	// Write "l", the bencoded form of each element in the List, and "e"
        output.write(Token.L);
        for (Iterator iter = list.iterator(); iter.hasNext(); ) encode(output, iter.next()); // Loop for each object, calling encode() on each one
        output.write(Token.E);
    }

    /**
     * Bencodes the given Map to the given OutputStream.
     * Pass any Java object that extends java.util.Map for the map parameter.
     * 
     * Writes a bencoded dictionary, which is a list of keys and values which looks like this:
     * 
     * d
     * 5:color  5:green
     * 6:flavor 4:lime
     * 5:shape  5:round
     * e
     * 
     * The bencoded data starts "d" for dictionary and ends "e" for end.
     * In the middle are pairs of bencoded values.
     * The keys have to be strings, while the values can be strings, numbers, lists, or more dictionaries.
     * The keys have to be in alphabetical order.
     * 
     * @param o   An OutputStream for this method to write bencoded data to
     * @param map The Java Map object to bencode and write
     */
    public static void encodeDict(OutputStream output, Map map) throws IOException {

    	// The BitTorrent specification requires that dictionary keys are sorted in alphanumeric order
    	SortedMap sorted = new TreeMap(); // A Java TreeMap will automatically sort the items we add to it
    	for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) { // Loop for each key and value pair in the given Map
    		Object key = iter.next();
    		sorted.put(key.toString(), map.get(key)); // Copy it into the TreeMap, which will put it in sorted order
    	}

    	// Write "d", each bencoded key and value, and then "e"
    	output.write(Token.D);
    	for (Iterator iter = sorted.keySet().iterator(); iter.hasNext(); ) {
    		String key = (String)iter.next();

    		// Write the key, a bencoded string like "4:key1"
    		encodeByteArray(output, key.getBytes(Token.ASCII));

    		// Write the value, which can be any kind of bencoded data
    		encode(output, sorted.get(key));
    	}
    	output.write(Token.E);
    }

    /**
     * Describes a given object using bencoding, and writes the bencoded data to the given stream.
     * 
     * @param  output                   An OutputStream for this method to write bencoded data to.
     * @param  object                   The Java Object to bencode and write.
     *                                  To write a bencoded dictionary, pass a Map object.
     *                                  To write a bencoded list, pass a List object.
     *                                  To write a bencoded number, pass a Number object.
     *                                  To write a bencoded string, pass a String or just a byte array.
     * @throws IOException              If there was a problem reading from the OutputStream.
     *         IllegalArgumentException If you pass an object that isn't a Map, List, Number, String, or byte array.
     */
    private static void encode(OutputStream output, Object object) throws IOException {

    	// Sort the object by its type, having the matching method bencode it and write it
    	if      (object instanceof Map)    encodeDict(output, (Map)object);
    	else if (object instanceof List)   encodeList(output, (List)object);
    	else if (object instanceof Number) encodeInt(output, (Number)object);
    	else if (object instanceof String) encodeByteArray(output, ((String)object).getBytes(Token.ASCII));
    	else if (object instanceof byte[]) encodeByteArray(output, (byte[])object);
    	else throw new IllegalArgumentException();
    }
}
