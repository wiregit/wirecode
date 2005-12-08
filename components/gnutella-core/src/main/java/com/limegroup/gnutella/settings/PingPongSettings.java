pbckage com.limegroup.gnutella.settings;

/**
 * Settings for pings bnd pongs, pong caching and such.
 */
public finbl class PingPongSettings extends LimeProps {
    
    privbte PingPongSettings() {}

    /**
     * Setting for whether or not pings should be sent for our pong
     * cbching scheme -- useful setting for tests.
     */
    public stbtic final BooleanSetting PINGS_ACTIVE =
        FACTORY.crebteBooleanSetting("PINGS_ACTIVE", true);
}
