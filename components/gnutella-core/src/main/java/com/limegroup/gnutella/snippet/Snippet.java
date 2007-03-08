
// Created for the Learning branch

package com.limegroup.gnutella.snippet;


import com.limegroup.gnutella.routing.HashFunction;


import com.limegroup.gnutella.ByteOrder;

public class Snippet {

	/**
	 * Hashes a keyword into the index of its bit in a QRP table.
	 * 
	 * @param keyword The search keyword to hash.
	 * @param bits    The number of bits the value should be within.
	 *                16 indicates 2^16, 65536, the hash value will be between 0 and 65535.
	 * @return        The hash value.
	 */
    public static int hashKeyword(String keyword, byte bits) {

    	int hash = 0;
        int hashPosition = 0;

        for (int keywordIndex = 0; keywordIndex < keyword.length(); keywordIndex++) {

        	hash ^= (Character.toLowerCase(keyword.charAt(keywordIndex)) & 0xFF) << hashPosition;
        	hashPosition = (hashPosition + 8) & 24;
        }

        return (int)(hash * 0x4F1BBCDC) >>> (32 - bits);
    }



    public static void snippet() {
    	
    	
    	
    	String result1[] = HashFunction.keywords("reds and greens");
    	String result2[] = HashFunction.getPrefixes(result1);
    	

    	
    	
    	
    	
    	/*
        BitSet b = new BitSet();
        
        int cardinality, unused, inuse;
        
        cardinality = b.cardinality();
        unused = b.unusedUnits();
        inuse = b.getUnitsInUse();
        
        b.set(1);
        b.set(2);
        b.set(3);
        
        cardinality = b.cardinality();
        unused = b.unusedUnits();
        inuse = b.getUnitsInUse();
        
        b.set(5);
    	
        cardinality = b.cardinality();
        unused = b.unusedUnits();
        inuse = b.getUnitsInUse();
        */
        
        
        
        
        
        
        
        
        


    }

    







    private static int calculateNewHosts(int degree, byte ttl) {

        double newHosts = 0;
        for ( ; ttl > 0; ttl--) {

            newHosts += Math.pow((degree - 1), ttl - 1);
        }

        return (int)newHosts;
    }
    
    
    
    /**
     * Write out the data in a byte array into a human-readable block of text.
     * Generates text like this:
     * 
     * 74 73 28 3d 74 3a 8b f9  ts(=t:--
     * c0 33 87 6f 76 39 e9 00  -3-ov9--
     * 
     * Puts 8 bytes of data on each line.
     * On the left, the method expresses each byte as two base 16 digits, "00" through "ff".
     * On the right, the method expresses each byte as ASCII characters.
     * If a byte is out of the range of readable characters, the method puts a hyphen in its place.
     * 
     * @param data A byte array of data
     * @return     A String with several lines of text that shows the data
     */
    public static String packetView(byte[] data) {

        // Make strings to put together the text
        String lines   = ""; // lines will hold all the finished lines, like "74 73 28 3d 74 3a 8b f9  ts(=t:--"
        String codes   = ""; // codes will hold the first part with each byte expressed in base 16, like "74 73 28 3d 74 3a 8b f9"
        String letters = ""; // letters will hold the bytes as ASCII charcters, like "ts(=t:--", with hyphens for bytes out of range

        // Loop down each byte in the given array
        for (int i = 0; i < data.length; i++) {

            // This is the start of a line
            if (i % 8 == 0) {

                // Compose a line, and add it to the finished text
                lines += codes + " " + letters + "\r\n";

                // Make codes and letters blank for the next line
                codes   = "";
                letters = "";
            }

            // Get the byte b in the data that our index is on
            byte b = data[i];

            // Express it as 2 base 16 characters, like "00" through "ff"
            String code;
            code = Integer.toHexString(ByteOrder.ubyte2int(b));
            if (code.length() == 1) code = "0" + code; // Add a leading 0 if necessary
            codes += code + " ";

            // Express it as an ASCII character, or a hyphen if not in range
            char letter = '-';
            if (b >= '!' && b <= '~') letter = (char)b;
            letters += letter;
        }

        // Add the codes and letters that didn't make it into a complete line
        lines += codes + " " + letters + "\r\n";

        // Return the composed text
        return lines;
    }
}
