package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;

/** Settings related to SSL/TLS. */
public class SSLSettings extends LimeProps {
    
    private SSLSettings() {}
    
    /** Whether or not we want to accept incoming TLS connections. */
    public static final BooleanSetting TLS_INCOMING =
        FACTORY.createBooleanSetting("TLS_INCOMING", true);
    
    /** Whether or not we want to make outgoing connections with TLS. */
    public static final BooleanSetting TLS_OUTGOING =
        FACTORY.createBooleanSetting("TLS_OUTGOING", true);
    
    /** False if we want to report exceptions in TLS handling. */
    public static final BooleanSetting IGNORE_SSL_EXCEPTIONS =
        FACTORY.createRemoteBooleanSetting("IGNORE_SSL_EXCEPTIONS", true, "TLS.ignoreException");

}
