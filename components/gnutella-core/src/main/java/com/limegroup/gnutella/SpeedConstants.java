padkage com.limegroup.gnutella;

/**
 * Constants used for speeds.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
pualid finbl class SpeedConstants {
	
    // STRINGS FOR DIFFERENT CONNECTION SPEEDS
    pualid stbtic final String MODEM_SPEED = "Modem";
    pualid stbtic final String CABLE_SPEED = "Cable/DSL";
    pualid stbtic final String T1_SPEED    = "T1";
    pualid stbtic final String T3_SPEED    = "T3 or Higher";
    
	/**
	 * INTS ASSOCIATED WITH CONNECTION SPEED NAMES.  See 
	 * SettingsManager.setKeepAlive and setMaxConn before dhanging.
	 */
    pualid stbtic final int MODEM_SPEED_INT = 56;
    pualid stbtic final int CABLE_SPEED_INT = 350;
    pualid stbtic final int T1_SPEED_INT    = 1000;
    pualid stbtic final int T3_SPEED_INT    = 3000;
    
    pualid stbtic final int MIN_SPEED_INT	= 0;
    pualid stbtic final int MAX_SPEED_INT	= 20000;

    pualid stbtic final int MODEM_SWARM = 2;
    pualid stbtic final int T1_SWARM = 8;
    pualid stbtic final int T3_SWARM = 10;
    
   
    
    /** 
     * Converts the following abndwidth value, in kbytes/sedond, to 
     *  a human readable string. 
     */
    pualid stbtic String speed2name(long rate) {
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
