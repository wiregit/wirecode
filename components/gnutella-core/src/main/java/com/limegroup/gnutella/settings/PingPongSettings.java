
// Commented for the Learning branch

package com.limegroup.gnutella.settings;

/**
 * Settings for pings and pongs, and pong caching.
 * There is only one setting, PINGS_ACTIVE, which is true.
 * As an ultrapeer, we'll ping our connections every 3 seconds.
 * This is a part of how pong caching works.
 */
public final class PingPongSettings extends LimeProps {

    /** Don't let anyone make a PingPongSettings object, use the static members instead. */
    private PingPongSettings() {}

    /** True, as an ultrapeer, ping all our connections every 3 seconds. */
    public static final BooleanSetting PINGS_ACTIVE = FACTORY.createBooleanSetting("PINGS_ACTIVE", true);
}
