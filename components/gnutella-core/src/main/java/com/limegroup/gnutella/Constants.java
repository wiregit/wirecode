/*
 * SpeedConstants.java
 *
 * Created on December 15, 2000, 3:18 PM
 */

package com.limegroup.gnutella;

/**
* A class to keep together the constants (and corresponding conversion
* methods) that may be used by multiple classes
* @author  Anurag Singla
*/
public class Constants
{
    
    //SPEED CONSTANTS

    // STRINGS FOR DIFFERENT CONNECTION SPEEDS
    public static final String MODEM_SPEED = "Modem";
    public static final String CABLE_SPEED = "Cable/DSL";
    public static final String T1_SPEED    = "T1";
    public static final String T3_SPEED    = "T3 or Higher";
    //public static final String OTHER_SPEED = "Other";

    // INTS ASSOCIATED WITH CONNECTION SPEED NAMES.  See 
    // SettingsManager.setKeepAlive and setMaxConn before changing.
    public static final int MODEM_SPEED_INT = 56;
    public static final int CABLE_SPEED_INT = 999;
    public static final int T1_SPEED_INT    = 1600;
    public static final int T3_SPEED_INT    = 3000;
    //public static final int OTHER_SPEED_INT = 28;
    
    //constants defining the names to be used to identify various kind
    //of limewire gnutella servents that exist in the network
    public static final String CLIENT = "CLIENT";
    public static final String XML_CLIENT = "XML_CLIENT";
    public static final String SERVER = "SERVER";
    public static final String ENTRY_SEPARATOR = ",";
        
    /** Converts the following bandwidth value, in kbytes/second, to 
     *  a human readable string. */
    public static String speed2name(long rate)
    {
        if (rate<=MODEM_SPEED_INT)
            return MODEM_SPEED;
        else if (rate<=CABLE_SPEED_INT)
            return CABLE_SPEED;
        else if (rate<=T1_SPEED_INT)
            return T1_SPEED;
        else
            return T3_SPEED;
    }

    /**
     * Returns true, if the passed servant type is avalid one, false
     * otherwise
     */
    public static boolean isValidServantType(String servantType)
    {
        //checking condition
        if(servantType == null)
            return false;
        
        //see if the servantType is valid
        if(servantType.equals(CLIENT) || servantType.equals(XML_CLIENT)
            || servantType.equals(SERVER))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
}
