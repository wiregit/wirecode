package com.limegroup.gnutella;

import java.util.concurrent.atomic.AtomicBoolean;

import com.limegroup.gnutella.settings.LimeProps;
import com.limegroup.gnutella.settings.SimppSettingsManager;
import com.limegroup.gnutella.simpp.SimppManager;

/**
 * This class is the glue that holds LimeWire together.
 * All various components are wired together here.
 */
public class LimeCoreGlue {
    
    private static AtomicBoolean glued = new AtomicBoolean(false);
    
    private LimeCoreGlue() {}

    /** Wires all various components together. */
    public static void install() {
        // Only glue once.
        if(!glued.compareAndSet(false, true))
            return;
        
        SimppManager simppManager = SimppManager.instance();
        SimppSettingsManager settingsManager = SimppSettingsManager.instance();
        LimeProps.instance().getFactory().setRemoteSettingManager(settingsManager);
        settingsManager.updateSimppSettings(simppManager.getPropsString());
    }

}
