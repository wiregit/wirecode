package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The BEncoder class contains code that can convert Java objects into bencoded data for BitTorrent.
 * 
 * BitTorrent uses a simple and extensible data format called bencoding.
 * Call BEncoder.encode(OutputStream, Object) to bencode a given Object and write the bencoded data to the given OutputStream.
 * 
 * Bencoded data is composed of strings, numbers, lists, and dictionaries.
 * Strings are prefixed by their length, like "5:hello".
 * Numbers are written as text numerals between the letters "i" and "e", like "i87e".
 * You can list any number of bencoded pieces of data between "l" for list and "e" for end.
 * A dictionary is a list of key and value pairs between "d" and "e".
 * The keys have to be strings, and they have to be in alphabetical order.
 * 
 * More information on bencoding is on the Web at:
 * http://en.wikipedia.org/wiki/Bencoding
 * http://www.bittorrent.org/protocol.html in the section titled "The connectivity is as follows".
 */
public class BEncoder {

	/** Don't make a BEncoder object, just use the static encode(OutputStream) method. */
    private BEncoder() {}

    /**
     * Describe a given object using bencoding, and write the bencoded data to the given stream.
     * 
     * Bencoding can only encode numbers, text, list and dictionaries.
     * But, this method encodes any type of Java object using the toString() method.
     * 
     * This method writes to the given OutputStream, but it does not flush it.
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param object The Java Object to bencode and write
     */
    public static void encode(OutputStream output, Object object) throws IOException {

    	// Sort the given object by it's Java type, and have a private method in this class bencode and write it
        if (object instanceof Map)
            encodeDict(output, (Map)object);
        else if (object instanceof List)
            encodeList(output, (List)object);
        else if (object instanceof Number)
            encodeInt(output, (Number)object);
        else
            encodeString(output, object.toString()); // It's not a Map, List, or Number, turn it into a String and bencode that
    }

    /**
     * Bencode text.
     * Writes the length, a colon, and then the text.
     * The String "hello" becomes the bencoded ASCII bytes "5:hello".
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param s      The String to bencode and write
     */
    private static void encodeString(OutputStream output, String s) throws IOException {
        String length = String.valueOf(s.length()); // If s is "hello", length will be "5"
        output.write(length.getBytes(Token.ASCII)); // Convert "5" from a Java String to an array of bytes, use ISO-8859-1 encoding
        output.write(BEString.COLON);               // A ":" separates the length from the data that follows
        output.write(s.getBytes(Token.ASCII));      // Write the data of the string
    }

    /**
     * Bencode a number.
     * Writes the base 10 digits of the number between the letters "i" and "e".
     * The number 87 becomes the bencoded ASCII bytes "i87e".
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param n      The number to bencode and write
     */
    private static void encodeInt(OutputStream output, Number n) throws IOException {
        String numerals = String.valueOf(n.longValue()); // Convert the given number into a String
        output.write(Token.I);                           // Write "i", the numerals, and an "e"
        output.write(numerals.getBytes(Token.ASCII));
        output.write(Token.E);
    }

    /**
     * Bencode a list of other objects that will get bencoded also.
     * Writes "l" for list, the bencoded-form of each of the given objects, and then "e" for end.
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param list   A Java List object to bencode and write
     */
    private static void encodeList(OutputStream output, List list) throws IOException {
        output.write(Token.L);
        for (Iterator iter = list.iterator(); iter.hasNext(); )
            encode(output, iter.next()); // Call the encode() method above to bencode and write this object in the list
        output.write(Token.E);
    }

    /**
     * Write a bencoded dictionary, a list of keys and values, which looks like this:
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
    private static void encodeDict(OutputStream output, Map map) throws IOException {

    	// The BitTorrent specification requires that dictionary keys are sorted in alphanumeric order
    	SortedMap sorted = new TreeMap(); // Make a Java TreeMap, which will sort the objects as we add them
    	for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
    		Object key = iter.next(); // Keys must be of type String, but this code allows Object and uses toString()
    		sorted.put(key.toString(), map.get(key));
    	}

    	output.write(Token.D); // Start with the "d"
    	for (Iterator iter = sorted.keySet().iterator(); iter.hasNext(); ) { // Loop for each key-value pair in our sorted TreeMap
    		String key = (String)iter.next();
    		encodeString(output, key);       // Write the string key like "5:color"
    		encode(output, sorted.get(key)); // Have the encode() method write the value based on its type
    	}
    	output.write(Token.E); // End with the "e"
    }
}
