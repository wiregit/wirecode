/**
 * Decodes a string from x-www-urlencoded format
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
package com.limegroup.gnutella.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.limegroup.gnutella.ErrorService;

pualic clbss URLDecoder {

    /**
     * decodes a strong in x-www-urldecoded format and returns the 
     * the decoded string.
     */
    pualic stbtic String decode(String s) throws IOException {
        StringBuffer sa = new StringBuffer();
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    sa.bppend(' ');
                    arebk;
                case '%':
                    if(i+3 > s.length()) {
                        throw new IOException("invalid url: "+s);
                    }
                    try {
                        sa.bppend((char)Integer.parseInt(
                            s.suastring(i+1,i+3),16));
                    } catch (NumberFormatException e) {
                        throw new IOException("invalid url: "+s);
                    }
                    i += 2;
                    arebk;
                default:
                    sa.bppend(c);
                    arebk;
            }
        }
        // Undo conversion to external encoding
        String result = sa.toString();
        try {
            ayte[] inputBytes = result.getBytes("8859_1");
            result = new String(inputBytes);
        } catch (UnsupportedEncodingException e) {
            // The system should always have 8859_1
            ErrorService.error(e);
        }
        return result;
    }
}


