package com.limegroup.gnutella.handshaking;

import java.text.ParseException;
import com.limegroup.gnutella.util.StringUtils;
import com.limegroup.gnutella.Assert;

/**
 * Miscellaneous routines to parse the X-Live-Since and Uptime headers.
 */
public class UptimeHeaders {
    private static final int SECONDS_PER_MINUTE=60;
    private static final int SECONDS_PER_HOUR=SECONDS_PER_MINUTE*60;
    private static final int SECONDS_PER_DAY=SECONDS_PER_HOUR*24;
    /** 
     * Returns the number of seconds in the given Uptime header.
     *
     * @param value a Gnucleus style header like "1D 16H 23M" (one
     *  day, 16 hours, 23 minutes)
     * @return the number of seconds, e.g., 1*24*60*60+16*60*60+23*60 
     *  for the above example.
     * @exception ParseException the value couldn't be decoded.  The
     *  offset of the exception is not guaranteed to be correct.
     */
    public static int decodeUptime(String value) throws ParseException {
        String[] fields=StringUtils.split(value, ' ');
        if (fields.length!=3) 
            throw new ParseException(
                "Wrong number of fields: "+fields.length, 0);

        int days=decodeUptimePart(fields[0], 'D');
        int hours=decodeUptimePart(fields[1], 'H');
        int minutes=decodeUptimePart(fields[2], 'M');
        
        //A 32-bit integer can hold 63 years worth of data, so we don't both to
        //check for overflow.
        return days*SECONDS_PER_DAY
              +hours*SECONDS_PER_HOUR
              +minutes*SECONDS_PER_MINUTE;       
    }

    /** decodeUptimePart("7H", 'H') returns 7. */
    private static int decodeUptimePart(String part, char suffix) 
            throws ParseException {
        int n=part.length();
        if (n<2)
            throw new ParseException("No suffix on \""+part+"\"", 0);
        if (part.charAt(n-1)!=suffix)
            throw new ParseException(
                "Wrong suffix '"+suffix+"' on \""+part+"\"", 0);

        String head=part.substring(0, n-1);
        try {            
            return Integer.parseInt(head);
        } catch (NumberFormatException e) {
            throw new ParseException("Bad number \""+head+"\"", 0);
        }
    }

    //public static String encodeUptime(int seconds);
    //public static int decodeXLiveSince(String value);
    //public static int encodeXLiveSince(int seconds);

    //Unit tests: tests/com/.../handshaking/UptimeHeadersTest
}
