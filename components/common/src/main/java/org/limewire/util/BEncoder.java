package org.limewire.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Convert Java objects into bencoded data.
 * 
 * Call BEncoder.encode(OutputStream, Object) to bencode a given Object and write the bencoded data to the given OutputStream.
 * 
 * Bencoded data is composed of strings, numbers, lists, and dictionaries.
 * Strings are prefixed by their length, like "5:hello".
 * Numbers are written as text numerals between the letters "i" and "e", like "i87e".
 * You can list any number of bencoded pieces of data between "l" for list and "e" for end.
 * A dictionary is a list of key and value pairs between "d" and "e".
 * The keys have to be strings, and they have to be in alphabetical order.
 * 
 * BitTorrent uses a simple and extensible data format called bencoding.
 * More information on bencoding is on the Web at:
 * http://en.wikipedia.org/wiki/Bencoding
 * http://www.bittorrent.org/protocol.html in the section titled "The connectivity is as follows".
 */
public class BEncoder {

    /** Identifies a bencoded number. */
    public static final byte I;
    /** Identifies a bencoded dictionary. */
    public static final byte D;
    /** Identifies a bencoded list. */
    public static final byte L;
    /** Marks the end of something in bencoding. */
    public static final byte E;
    /** Separates the length from the string in the data of a bencoded string. */
    public final static byte COLON;
    
    private static final String ASCII = "ISO-8859-1";
    
    static {
        byte i = 0;
        byte d = 0;
        byte l = 0;
        byte e = 0;
        byte colon = 0;

        try {

            i = "i".getBytes(ASCII)[0];
            d = "d".getBytes(ASCII)[0];
            l = "l".getBytes(ASCII)[0];
            e = "e".getBytes(ASCII)[0];
            colon = ":".getBytes(ASCII)[0];

        } catch (UnsupportedEncodingException impossible) {

            // TODO: connect to the error service
        }

        COLON = colon;
        I = i;
        D = d;
        L = l;
        E = e;
    }
    
	// Prevents anyone from making a BEncoder object
    private BEncoder() {}

    /**
     * Bencodes the given byte array to the given OutputStream.
     * 
     * Writes the length, a colon, and then the text.
     * For example, the byte array ['h', 'e', 'l', 'l', 'o'] becomes the bencoded bytes "5:hello".
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param b      The byte array to bencode and write
     */
    public static void encodeByteArray(OutputStream output, byte[] b) throws IOException {
        String length = String.valueOf(b.length);
        output.write(length.getBytes(ASCII));
        output.write(COLON);
        output.write(b);
    }

    /**
     * Bencodes the given Number to the given OutputStream.
     * 
     * Writes the base 10 digits of the number between the letters "i" and "e".
     * For example, the number 87 becomes the bencoded ASCII bytes "i87e".
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param n      The number to bencode and write
     */
    public static void encodeInt(OutputStream output, Number n) throws IOException {
        String numerals = String.valueOf(n.longValue());
        output.write(I);
        output.write(numerals.getBytes(ASCII));
        output.write(E);
    }

    /**
     * Bencodes the given List to the given OutputStream.  Any String objects
     * are encoded with ASCII.
     * 
     * Writes "l" for list, the bencoded-form of each of the given objects, and then "e" for end.
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param list   A Java List object to bencode and write
     */
    public static void encodeList(OutputStream output, List<?> list) throws IOException {
        encodeList(output, list, ASCII);
    }
    
    /**
     * Bencodes the given List to the given OutputStream.
     * 
     * Writes "l" for list, the bencoded-form of each of the given objects, and then "e" for end.
     * 
     * @param output An OutputStream for this method to write bencoded data to
     * @param list   A Java List object to bencode and write
     * @param encoding the encoding to apply to string objects.
     */
    public static void encodeList(OutputStream output, List<?> list, String encoding) throws IOException {
        output.write(L);
        for(Object next : list) 
            encode(output, next, encoding);
        output.write(E);
    }

    /**
     * Bencodes the given Map to the given OutputStream.  Any String objects
     * are encoded using ASCII.
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
    public static void encodeDict(OutputStream output, Map<?, ?> map) throws IOException {
        encodeDict(output, map, ASCII);
    }
    
    /**
     * Bencodes the given Map to the given OutputStream.
     * @param encoding the encoding to apply to String objects.
     */
    public static void encodeDict(OutputStream output, Map<?, ?> map, String encoding) throws IOException {

        // The BitTorrent specification requires that dictionary keys are sorted in alphanumeric order
        SortedMap<String, Object> sorted = new TreeMap<String, Object>();
        for(Map.Entry<?, ?> entry : map.entrySet())
            sorted.put(entry.getKey().toString(), entry.getValue());

        output.write(D);
        for(Map.Entry<String, Object> entry : sorted.entrySet()) {
            encodeByteArray(output, entry.getKey().getBytes(encoding));
            encode(output, entry.getValue(), encoding);
        }
        output.write(E);
    }

    /**
     * Describes a given object using bencoding, and writes the bencoded data to the given stream.
     * 
     * To write a bencoded dictionary, pass a Map object.
     * To write a bencoded list, pass a List object.
     * To write a bencoded number, pass a Number object.
     * To write a bencoded string, pass a String or just a byte array.
     * 
     * @param  output                   An OutputStream for this method to write bencoded data to.
     * @param  object                   The Java Object to bencode and write.
     * @throws IOException              If there was a problem reading from the OutputStream.
     *         IllegalArgumentException If you pass an object that isn't a Map, List, Number, String, or byte array.
     */
    private static void encode(OutputStream output, Object object, String encoding) throws IOException {
    	if (object instanceof Map)
    		encodeDict(output, (Map)object);
    	else if (object instanceof List)
    		encodeList(output, (List)object);
    	else if (object instanceof Number)
    		encodeInt(output, (Number)object);
    	else if (object instanceof String)
    		encodeByteArray(output, ((String)object).getBytes(encoding));
    	else if (object instanceof byte[])
    		encodeByteArray(output, (byte[])object);
    	else
    		throw new IllegalArgumentException();
    }
}
