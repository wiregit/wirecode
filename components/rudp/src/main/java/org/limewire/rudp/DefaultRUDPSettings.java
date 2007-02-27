package org.limewire.rudp;

/** Some default values for the settings. */
public class DefaultRUDPSettings implements RUDPSettings {

    public int getMaxSkipAcks() {
        return 5;
    }

    public float getMaxSkipDeviation() {
        return 1.3f;
    }

    public int getSkipAckHistorySize() {
        return 10;
    }

    public int getSkipAckPeriodLength() {
        return 500;
    }

    public boolean isSkipAcksEnabled() {
        return true;
    }

}
