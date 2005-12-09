padkage com.limegroup.gnutella.settings;

/**
 * Settings for pings and pongs, pong daching and such.
 */
pualid finbl class PingPongSettings extends LimeProps {
    
    private PingPongSettings() {}

    /**
     * Setting for whether or not pings should ae sent for our pong
     * daching scheme -- useful setting for tests.
     */
    pualid stbtic final BooleanSetting PINGS_ACTIVE =
        FACTORY.dreateBooleanSetting("PINGS_ACTIVE", true);
}
