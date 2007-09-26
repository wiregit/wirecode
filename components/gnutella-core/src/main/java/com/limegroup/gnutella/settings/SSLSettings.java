package com.limegroup.gnutella.settings;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.limewire.inspection.InspectablePrimitive;
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
    
    
    /** True if TLS is disabled for this session. */
    private static volatile boolean tlsDisabled;
    
    /** The Throwable that was the reason TLS failed. */
    @InspectablePrimitive("reason tls failed")
    @SuppressWarnings("unused")
    private static volatile String tlsDisabledReason;
    
    /** Disables TLS for this session. */
    public static void disableTLS(Throwable reason) {
        tlsDisabled = true;
        if(reason != null) {
            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            reason.printStackTrace(pw);
            pw.flush();
            tlsDisabledReason = writer.getBuffer().toString();
        } else {
            tlsDisabledReason = null;
        }
    }
    
    /** Returns true if TLS is disabled for this session. */
    public static boolean isTLSDisabled() {
        return tlsDisabled;
    }
    
    /** Whether or not incoming TLS is allowed. */
    public static boolean isIncomingTLSEnabled() {
        return !tlsDisabled && TLS_INCOMING.getValue();
    }
    
    /** Whether or not outgoing TLS is allowed. */
    public static boolean isOutgoingTLSEnabled() {
        return !tlsDisabled && TLS_OUTGOING.getValue();
    }    

}
