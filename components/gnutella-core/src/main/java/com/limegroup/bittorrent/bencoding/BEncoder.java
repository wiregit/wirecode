package com.limegroup.bittorrent.bencoding;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class BEncoder {
    
    private BEncoder() {}
    
    /**
     * Encodes an object to a stream using BEncoding.  Even though bencoding
     * can encode only digits, strings, lists and dictionaries, we can encode
     * any type of java object thanks to the toString() method :)
     * 
     * Note: This will not flush the stream.
     */
    public static void encode(OutputStream os, Object data) throws IOException {
        if (data instanceof Map)
            encodeDict(os, (Map) data);
        else if (data instanceof List)
            encodeList(os, (List) data);
        else if (data instanceof Number)
            encodeInt(os, (Number) data);
        else
            encodeString(os, data.toString());
    }
    
    private static void encodeDict(OutputStream os, Map map) throws IOException {
        
        // according to spec, dicts must be sorted alphanumerically
        SortedMap tree = new TreeMap();
        
        for (Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            // keys must be of type string, but we can cheat
            Object key = iter.next();
            tree.put(key.toString(), map.get(key));
        }
        
        os.write(Token.D);
        
        for (Iterator iter = tree.keySet().iterator(); iter.hasNext();) {
            String key = (String) iter.next();
            encodeString(os, key);
            encode(os, tree.get(key));
        }
        os.write(Token.E);
    }
    
    private static void encodeString(OutputStream os, String value)
    throws IOException {
        String length = String.valueOf(value.length());
        os.write(length.getBytes(Token.ASCII));
        os.write(BEString.COLON);
        os.write(value.getBytes(Token.ASCII));
    }

    private static void encodeInt(OutputStream os, Number num)
    throws IOException {
        String number = String.valueOf(num.longValue());
        os.write(Token.I);
        os.write(number.getBytes(Token.ASCII));
        os.write(Token.E);
    }

    private static void encodeList(OutputStream os, List list)
    throws IOException {
        os.write(Token.L);
        for (Iterator iter = list.iterator(); iter.hasNext();) 
            encode(os, iter.next());
        os.write(Token.E);
    }
}
