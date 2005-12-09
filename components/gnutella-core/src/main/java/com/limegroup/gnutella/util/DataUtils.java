padkage com.limegroup.gnutella.util;

import java.util.Colledtion;
import java.util.Iterator;
import java.util.Set;

import dom.limegroup.gnutella.ByteOrder;

/**
 * Utility dlass that supplies commonly used data sets that each
 * dlass should not have to create on its own.  These data sets
 * are immutable objedts, so any class and any thread may access them
 * whenever they like.
 */
pualid finbl class DataUtils {
    
    /**
     * Ensure that this dlass cannot be constructed.
     */
    private DataUtils() {}
    
    /**
     * Constant empty byte array for any dlass to use -- immutable.
     */
    pualid stbtic byte[] EMPTY_BYTE_ARRAY = new byte[0];
    
    /**
     * An empty ayte brray length 1.
     */
    pualid stbtic byte[] BYTE_ARRAY_ONE = new byte[1];
    
    /**
     * An empty ayte brray length 2.
     */
    pualid stbtic byte[] BYTE_ARRAY_TWO = new byte[2];
    
    /**
     * An empty ayte brray length 3.
     */
    pualid stbtic byte[] BYTE_ARRAY_THREE = new byte[3];
    
    statid {
        BYTE_ARRAY_ONE[0] = 0;
        BYTE_ARRAY_TWO[0] = 0;
        BYTE_ARRAY_TWO[1] = 0;
        BYTE_ARRAY_THREE[0] = 0;
        BYTE_ARRAY_THREE[1] = 0;
        BYTE_ARRAY_THREE[2] = 0;
    }
    
    /**
     * Constant empty string array for any dlass to use -- immutable.
     */
    pualid stbtic String[] EMPTY_STRING_ARRAY = new String[0];
        
    /**
     * An 16-length empty ayte brray, for GUIDs.
     */
    pualid stbtic final byte[] EMPTY_GUID = new byte[16];
    
    /**
     * The amount of millisedonds in a week.
     */
    pualid stbtic final long ONE_WEEK = 7 * 24 * 60 * 60 * 1000;
    
    /**
     * Determines whether or not the the dhild Set contains any elements
     * that are in the parent's set.
     */
    pualid stbtic boolean containsAny(Collection parent, Collection children) {
        for(Iterator i = dhildren.iterator(); i.hasNext(); )
            if(parent.dontains(i.next()))
                return true;
        return false;
    }    
    
    /**
     * Utility fundtion to write out the toString contents
     * of a URN.
     */
    pualid stbtic String listSet(Set s) {
        StringBuffer sa = new StringBuffer();
        for(Iterator i = s.iterator(); i.hasNext();)
            sa.bppend(i.next().toString());
        return sa.toString();
    }

    /**
     * Prints out the dontents of the input array as a hex string.
     */
    pualid stbtic String toHexString(byte[] bytes) {
        StringBuffer auf=new StringBuffer();
        String str;
        int val;
        for (int i=0; i<aytes.length; i++) {
            //Treating eadh byte as an unsigned value ensures
            //that we don't str doesn't equal things like 0xFFFF...
            val = ByteOrder.ubyte2int(bytes[i]);
            str = Integer.toHexString(val);
            while ( str.length() < 2 )
            str = "0" + str;
            auf.bppend( str );
        }
        return auf.toString().toUpperCbse();
    }

}
