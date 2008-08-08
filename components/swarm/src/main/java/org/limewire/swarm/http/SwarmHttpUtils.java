package org.limewire.swarm.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.nio.IOControl;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.Range;
import org.limewire.io.IOUtils;

public class SwarmHttpUtils {

    private SwarmHttpUtils() {
    }

    /**
     * Closes the connection associated with this HttpContext.
     */
    public static void closeConnectionFromContext(HttpContext context) {
        IOControl ioctrl = (IOControl) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
        try {
            ioctrl.shutdown();
        } catch (IOException ignored) {
        }
    }

    /**
     * Returns a {@link Range} object representing the data within the range
     * request. The data should have 'bytes=X-Y'.
     */
    public static Range rangeForRequest(String value) {
        if (!value.startsWith("bytes") || value.length() <= 6) {
            return null;
        }

        int dash = value.indexOf('-');
        if (dash == -1 || dash == value.length() - 1) {
            return null;
        }

        try {
            long low = Long.parseLong(value.substring(6, dash).trim());
            long high = Long.parseLong(value.substring(dash + 1).trim());
            if (low > high) {
                return null;
            } else {
                return Range.createRange(low, high);
            }
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            return null;
        }
    }

    public static Range rangeForContentRange(String headerValue, long contentLength)
            throws IOException {
        try {
            int start = headerValue.indexOf("bytes") + 6; // skip "bytes " or
            // "bytes="
            int slash = headerValue.indexOf('/');

            // if looks like: "bytes */*" or "bytes */10" -- NOT part of the
            // spec
            if (headerValue.substring(start, slash).equals("*")) {
                return Range.createRange(0, contentLength - 1);
            }

            int dash = headerValue.lastIndexOf("-");
            long numBeforeDash = numberFor(headerValue.substring(start, dash));
            long numBeforeSlash = numberFor(headerValue.substring(dash + 1, slash));

            if (numBeforeSlash < numBeforeDash)
                throw new IOException("invalid range, high (" + numBeforeSlash
                        + ") less than low (" + numBeforeDash + ")");

            return Range.createRange(numBeforeDash, numBeforeSlash);

            // TODO: Is it necessary to validate the number after the slash
            // matches the fileCoordinator's size (or is '*')?
        } catch (IndexOutOfBoundsException e) {
            throw IOUtils.getIOException("Invalid Header: " + headerValue, e);
        }

    }

    public static long numberFor(String number) throws IOException {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException nfe) {
            throw IOUtils.getIOException("Invalid number: " + number, nfe);
        }
    }
    
    public static Range parseContentRange(HttpResponse response) throws IOException {
        Range actualRange;
        
        Header contentRange = response.getFirstHeader("Content-Range");
        Header contentLengthHeader = response.getFirstHeader("Content-Length");
        

        if (contentLengthHeader != null) {
            long contentLength = SwarmHttpUtils.numberFor(contentLengthHeader.getValue());
            if (contentLength < 0) {
                throw new IOException("Invalid content length: " + contentLength);
            }
            
            if (contentRange != null) {
                // If a range exists, that's what we want.
                actualRange = SwarmHttpUtils.rangeForContentRange(contentRange.getValue(),
                        contentLength);
            } else {
                // If no range exists, assume 0 -> contentLength
                actualRange = Range.createRange(0, contentLength - 1);
            }
        } else if (contentRange == null) {
            // Fail miserably.
            throw new IOException("No Content Range Found.");
        } else {
            // Fail miserably.
            throw new IOException("No content length, though content range existed.");
        }
        return actualRange;
    }
}
