
// Commented for the Learning branch

package com.limegroup.gnutella;

/**
 * Constants used for speeds, in Kbps.
 * These speeds are in Kbps, 8 Kbps is 1 KB/s.
 */
public final class SpeedConstants {

    // Not used
    public static final String MODEM_SPEED = "Modem";
    public static final String CABLE_SPEED = "Cable/DSL";
    public static final String T1_SPEED    = "T1";
    public static final String T3_SPEED    = "T3 or Higher";

    /**
     * 56 Kbps, the speed of a modem.
     */
    public static final int MODEM_SPEED_INT = 56;

    /**
     * 350 Kbps, the upload speed of a cable Internet connection.
     * In North America in late 2005, cable connections download at 4-8 Mbps and upload at 384-768 Kbps.
     */
    public static final int CABLE_SPEED_INT = 350;

    /**
     * 1000 Kbps, the speed of a T1 line.
     * Officially, a T1 line has a speed of 1.4 Mbps.
     */
    public static final int T1_SPEED_INT = 1000;

    /**
     * 3000 KB/s, the speed of a T3 line.
     */
    public static final int T3_SPEED_INT = 3000;

    // Used by the settings GUI
    public static final int MIN_SPEED_INT = 0;
    public static final int MAX_SPEED_INT = 20000;

    /** 2, our swarm capacity if we have a modem. (do) */
    public static final int MODEM_SWARM = 2;
    /** 8, our swarm capacity if we have a speed like a T1 line. */
    public static final int T1_SWARM = 8;
    /** 10, our swarm capacity if we have a connection speed like a T3 line. */
    public static final int T3_SWARM = 10;

    /** Not used. */
    public static String speed2name(long rate) {
        if      (rate <= MODEM_SPEED_INT) return MODEM_SPEED;
        else if (rate <= CABLE_SPEED_INT) return CABLE_SPEED;
        else if (rate <= T1_SPEED_INT)    return T1_SPEED;
        else                              return T3_SPEED;
    }
}
