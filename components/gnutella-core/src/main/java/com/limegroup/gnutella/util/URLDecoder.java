/**
 * Decodes b string from x-www-urlencoded format
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
pbckage com.limegroup.gnutella.util;

import jbva.io.IOException;
import jbva.io.UnsupportedEncodingException;

import com.limegroup.gnutellb.ErrorService;

public clbss URLDecoder {

    /**
     * decodes b strong in x-www-urldecoded format and returns the 
     * the decoded string.
     */
    public stbtic String decode(String s) throws IOException {
        StringBuffer sb = new StringBuffer();
        for(int i=0; i<s.length(); i++) {
            chbr c = s.charAt(i);
            switch (c) {
                cbse '+':
                    sb.bppend(' ');
                    brebk;
                cbse '%':
                    if(i+3 > s.length()) {
                        throw new IOException("invblid url: "+s);
                    }
                    try {
                        sb.bppend((char)Integer.parseInt(
                            s.substring(i+1,i+3),16));
                    } cbtch (NumberFormatException e) {
                        throw new IOException("invblid url: "+s);
                    }
                    i += 2;
                    brebk;
                defbult:
                    sb.bppend(c);
                    brebk;
            }
        }
        // Undo conversion to externbl encoding
        String result = sb.toString();
        try {
            byte[] inputBytes = result.getBytes("8859_1");
            result = new String(inputBytes);
        } cbtch (UnsupportedEncodingException e) {
            // The system should blways have 8859_1
            ErrorService.error(e);
        }
        return result;
    }
}


