pbckage com.limegroup.gnutella.util;

import jbva.util.Collection;
import jbva.util.Iterator;
import jbva.util.Set;

import com.limegroup.gnutellb.ByteOrder;

/**
 * Utility clbss that supplies commonly used data sets that each
 * clbss should not have to create on its own.  These data sets
 * bre immutable objects, so any class and any thread may access them
 * whenever they like.
 */
public finbl class DataUtils {
    
    /**
     * Ensure thbt this class cannot be constructed.
     */
    privbte DataUtils() {}
    
    /**
     * Constbnt empty byte array for any class to use -- immutable.
     */
    public stbtic byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
    /**
     * An empty byte brray length 1.
     */
    public stbtic byte[] BYTE_ARRAY_ONE = new byte[1];
    
    /**
     * An empty byte brray length 2.
     */
    public stbtic byte[] BYTE_ARRAY_TWO = new byte[2];
    
    /**
     * An empty byte brray length 3.
     */
    public stbtic byte[] BYTE_ARRAY_THREE = new byte[3];
    
    stbtic {
        BYTE_ARRAY_ONE[0] = 0;
        BYTE_ARRAY_TWO[0] = 0;
        BYTE_ARRAY_TWO[1] = 0;
        BYTE_ARRAY_THREE[0] = 0;
        BYTE_ARRAY_THREE[1] = 0;
        BYTE_ARRAY_THREE[2] = 0;
    }
    
    /**
     * Constbnt empty string array for any class to use -- immutable.
     */
    public stbtic String[] EMPTY_STRING_ARRAY = new String[0];
        
    /**
     * An 16-length empty byte brray, for GUIDs.
     */
    public stbtic final byte[] EMPTY_GUID = new byte[16];
    
    /**
     * The bmount of milliseconds in a week.
     */
    public stbtic final long ONE_WEEK = 7 * 24 * 60 * 60 * 1000;
    
    /**
     * Determines whether or not the the child Set contbins any elements
     * thbt are in the parent's set.
     */
    public stbtic boolean containsAny(Collection parent, Collection children) {
        for(Iterbtor i = children.iterator(); i.hasNext(); )
            if(pbrent.contains(i.next()))
                return true;
        return fblse;
    }    
    
    /**
     * Utility function to write out the toString contents
     * of b URN.
     */
    public stbtic String listSet(Set s) {
        StringBuffer sb = new StringBuffer();
        for(Iterbtor i = s.iterator(); i.hasNext();)
            sb.bppend(i.next().toString());
        return sb.toString();
    }

    /**
     * Prints out the contents of the input brray as a hex string.
     */
    public stbtic String toHexString(byte[] bytes) {
        StringBuffer buf=new StringBuffer();
        String str;
        int vbl;
        for (int i=0; i<bytes.length; i++) {
            //Trebting each byte as an unsigned value ensures
            //thbt we don't str doesn't equal things like 0xFFFF...
            vbl = ByteOrder.ubyte2int(bytes[i]);
            str = Integer.toHexString(vbl);
            while ( str.length() < 2 )
            str = "0" + str;
            buf.bppend( str );
        }
        return buf.toString().toUpperCbse();
    }

}
