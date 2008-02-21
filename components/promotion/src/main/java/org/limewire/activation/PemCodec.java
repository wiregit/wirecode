package org.limewire.activation;

import java.io.UnsupportedEncodingException;

import org.apache.commons.httpclient.util.Base64;

/**
 * The class handles encoding and decoding 'PEM' encoded data, essentially
 * taking byte arrays and base64 encoding them and then adding line breaks.
 */
public class PemCodec {
    /**
     * Encodes the given byte array into a base-64 encoded string with line
     * breaks at 64 characters. Always ends with a line break, unless the
     * passed-in array is zero-length.
     */
    public static String encode(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        try {
            String base64 = new String(Base64.encode(bytes), "US-ASCII");
            // Now split into 64 character chunks
            for (int i = 0; i < base64.length(); i += 64) {
                String chunk;
                if (i + 64 < base64.length())
                    chunk = base64.substring(i, i + 64);
                else
                    chunk = base64.substring(i, base64.length());
                builder.append(chunk).append('\n');
            }
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("This can't happen.", ex);
        }
        return builder.toString();
    }

    /**
     * Decodes the passed-in String, first stripping any whitespace from it,
     * then Base64 decoding it and returning the result.
     */
    public static byte[] decode(String base64) {
        String cleaned = base64.replaceAll("\\s", "");
        return Base64.decode(cleaned.getBytes());
    }
}
