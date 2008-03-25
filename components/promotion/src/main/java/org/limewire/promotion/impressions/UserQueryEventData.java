package org.limewire.promotion.impressions;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.limewire.util.ByteUtil;

/**
 * This is the data sent to the server. It includes a String query, and binary
 * data for the time and impressions.
 */
public class UserQueryEventData {

    private final UserQueryEvent event;

    private final long millisSinceToday;

    public UserQueryEventData(UserQueryEvent event, long millisSinceToday) {
        this.event = event;
        this.millisSinceToday = millisSinceToday;
    }

    public UserQueryEventData(UserQueryEvent event) {
        this(event, millisecondsSinceTheStartOfToday());
    }

    public String getQuery() {
        return event.getOriginalQuery();
    }

    // TODO: Improve to not include all the array creation
    public byte[] getData() {
        List<Impression> impressions = event.getImpressions();
        /*
         * Format:
         *  
         *   number of impressions               : 1 byte
         *   milliseconds since start of the day : 4 bytes 
         *   original query time                 : 8 bytes  
         *   List of n=length of
         *     n=binder name length     : 1 byte 
         *     binder name              : n bytes 
         *     promo ID                 : 8 bytes
         *     impression time          : 8 bytes
         */
        int length = 1 + 4 + 8;
        for (Impression imp : impressions) {
            length += 1;
            length += imp.getBinderUniqueName().length();
            length += 8;
            length += 8;
        }
        byte[] bytes = new byte[length];
        PostIncrement inc = new PostIncrement(0);
        bytes[inc.inc()] = (byte) (0xff & impressions.size());
        System.arraycopy(ByteUtil.convertToBytes(millisSinceToday, 4), 0, bytes, inc.inc(4), 4);
        System.arraycopy(ByteUtil.convertToBytes(event.getOriginalQueryTime().getTime(), 8), 0,
                bytes, inc.inc(8), 8);
        for (int i = 0; i < impressions.size(); i++) {
            Impression imp = impressions.get(i);
            String binderName = imp.getBinderUniqueName();
            bytes[inc.inc()] = (byte) (0xff & binderName.length());
            System.arraycopy(binderName.getBytes(), 0, bytes, inc.inc(binderName.length()),
                    binderName.length());
            System.arraycopy(ByteUtil.convertToBytes(imp.getPromo().getUniqueID(), 8), 0, bytes,
                    inc.inc(8), 8);
            System.arraycopy(ByteUtil.convertToBytes(imp.getTimeShown().getTime(), 8), 0, bytes,
                    inc.inc(8), 8);
        }
        return bytes;
    }

    /**
     * Returns the number of milliseconds since the start of the today. This is
     * exposed, so we can get it during testing.
     * 
     * @return the number of milliseconds since the start of the today.
     */
    long getMillisSinceToday() {
        return millisSinceToday;
    }

    /**
     * Returns the milliseconds since the start of today.
     * 
     * @return
     */
    static int millisecondsSinceTheStartOfToday() {
        return (int) (System.currentTimeMillis() - getFirstMillisecond());
    }

    static long getFirstMillisecond() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        Calendar cal = new GregorianCalendar(year, 0, 1, 0, 0, 0);
        return cal.getTime().getTime();
    }

}
