/**
 * Decodes a string from x-www-urlencoded format
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
package com.limegroup.gnutella.util;

import java.io.*;
import java.security.AccessController;

import sun.security.action.GetPropertyAction;

public class URLDecoder {

    // The platform default encoding
    static String dfltEncName = 
        (String)AccessController.doPrivileged(
            new GetPropertyAction("file.encoding"));

    /**
     * Decodes a <code>x-www-form-urlencoded</code> string.
     * The platform's default encoding is used to determine what characters 
     * are represented by any consecutive sequences of the form 
     * "<code>%<i>xy</i></code>".
     * @param s the <code>String</code> to decode
     * @deprecated The resulting string may vary depending on the platform's
     *          default encoding. Instead, use the decode(String,String) method
     *          to specify the encoding.
     * @return the newly decoded <code>String</code>
     */
    public static String decode(String s) {
        try {
            return decode(s, dfltEncName);
        } catch (UnsupportedEncodingException e) {
            // The system should always have the platform default
            return null;
        }
    }

    /**
     * Decodes a <code>application/x-www-form-urlencoded</code> string using a 
     * specific encoding scheme.
     * The supplied encoding is used to determine
     * what characters are represented by any consecutive sequences of the
     * form "<code>%<i>xy</i></code>".
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilites.</em>
     *
     * @param s the <code>String</code> to decode
     * @param enc   The name of a supported 
     *    <a href="../lang/package-summary.html#charenc">character
     *    encoding</a>. 
     * @return the newly decoded <code>String</code>
     * @exception  UnsupportedEncodingException
     *             If the named encoding is not supported
     * @see URLEncoder#encode(java.lang.String, java.lang.String)
     * @since 1.4
     */
    public static String decode(String s, String enc) 
        throws UnsupportedEncodingException {
        
        boolean needToChange = false;
        StringBuffer sb = new StringBuffer();
        int numChars = s.length();
        int i = 0;
    
        if (enc.length() == 0) {
            throw new UnsupportedEncodingException ("URLDecoder: empty " +
                "string enc parameter");
        }
    
        while (i < numChars) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    i++;
                    needToChange = true;
                    break;
                case '%':
                    /*
                     * Starting with this instance of %, process all
                     * consecutive substrings of the form %xy. Each
                     * substring %xy will yield a byte. Convert all
                     * consecutive  bytes obtained this way to whatever
                     * character(s) they represent in the provided
                     * encoding.
                     */
            
                    try {
            
                        // (numChars-i)/3 is an upper bound for the number
                        // of remaining bytes
                        byte[] bytes = new byte[(numChars-i)/3];
                        int pos = 0;
                        
                        while ( ((i+2) < numChars) && (c=='%')) {
                            bytes[pos++] = 
                                (byte)Integer.parseInt(s.substring(i+1,i+3),16);
                            i+= 3;
                            if (i < numChars)
                                c = s.charAt(i);
                            }
            
                            // A trailing, incomplete byte encoding such as
                            // "%x" will cause an exception to be thrown
            
                            if ((i < numChars) && (c=='%'))
                                throw new IllegalArgumentException(
                                    "URLDecoder: Incomplete trailing escape " +
                                    "(%) pattern");
                            
                            sb.append(new String(bytes, 0, pos, enc));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(
                                    "URLDecoder: Illegal hex characters in " +
                                    "escape (%) pattern - " 
                            + e.getMessage());
                        }
                        needToChange = true;
                        break;
                    default: 
                        sb.append(c); 
                        i++;
                        break; 
                }
            }

        return (needToChange? sb.toString() : s);
    }
}


