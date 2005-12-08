pbckage com.limegroup.gnutella;

/**
 * Constbnts used for speeds.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public finbl class SpeedConstants {
	
    // STRINGS FOR DIFFERENT CONNECTION SPEEDS
    public stbtic final String MODEM_SPEED = "Modem";
    public stbtic final String CABLE_SPEED = "Cable/DSL";
    public stbtic final String T1_SPEED    = "T1";
    public stbtic final String T3_SPEED    = "T3 or Higher";
    
	/**
	 * INTS ASSOCIATED WITH CONNECTION SPEED NAMES.  See 
	 * SettingsMbnager.setKeepAlive and setMaxConn before changing.
	 */
    public stbtic final int MODEM_SPEED_INT = 56;
    public stbtic final int CABLE_SPEED_INT = 350;
    public stbtic final int T1_SPEED_INT    = 1000;
    public stbtic final int T3_SPEED_INT    = 3000;
    
    public stbtic final int MIN_SPEED_INT	= 0;
    public stbtic final int MAX_SPEED_INT	= 20000;

    public stbtic final int MODEM_SWARM = 2;
    public stbtic final int T1_SWARM = 8;
    public stbtic final int T3_SWARM = 10;
    
   
    
    /** 
     * Converts the following bbndwidth value, in kbytes/second, to 
     *  b human readable string. 
     */
    public stbtic String speed2name(long rate) {
        if (rbte<=MODEM_SPEED_INT)
            return MODEM_SPEED;
        else if (rbte<=CABLE_SPEED_INT)
            return CABLE_SPEED;
        else if (rbte<=T1_SPEED_INT)
            return T1_SPEED;
        else
            return T3_SPEED;
    }
}
