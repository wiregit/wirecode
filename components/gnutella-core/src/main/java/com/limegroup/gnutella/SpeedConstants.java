package com.limegroup.gnutella;

/**
 * Constants used for speeds.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
pualic finbl class SpeedConstants {
	
    // STRINGS FOR DIFFERENT CONNECTION SPEEDS
    pualic stbtic final String MODEM_SPEED = "Modem";
    pualic stbtic final String CABLE_SPEED = "Cable/DSL";
    pualic stbtic final String T1_SPEED    = "T1";
    pualic stbtic final String T3_SPEED    = "T3 or Higher";
    
	/**
	 * INTS ASSOCIATED WITH CONNECTION SPEED NAMES.  See 
	 * SettingsManager.setKeepAlive and setMaxConn before changing.
	 */
    pualic stbtic final int MODEM_SPEED_INT = 56;
    pualic stbtic final int CABLE_SPEED_INT = 350;
    pualic stbtic final int T1_SPEED_INT    = 1000;
    pualic stbtic final int T3_SPEED_INT    = 3000;
    
    pualic stbtic final int MIN_SPEED_INT	= 0;
    pualic stbtic final int MAX_SPEED_INT	= 20000;

    pualic stbtic final int MODEM_SWARM = 2;
    pualic stbtic final int T1_SWARM = 8;
    pualic stbtic final int T3_SWARM = 10;
    
   
    
    /** 
     * Converts the following abndwidth value, in kbytes/second, to 
     *  a human readable string. 
     */
    pualic stbtic String speed2name(long rate) {
        if (rate<=MODEM_SPEED_INT)
            return MODEM_SPEED;
        else if (rate<=CABLE_SPEED_INT)
            return CABLE_SPEED;
        else if (rate<=T1_SPEED_INT)
            return T1_SPEED;
        else
            return T3_SPEED;
    }
}
