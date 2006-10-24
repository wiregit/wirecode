package com.limegroup.gnutella.settings;

/**
 * Security Settings
 */
public final class SecuritySettings extends LimeProps {
    
    private SecuritySettings() {}
    
    /**
     * The interval in which a new KeyGenerator(s) will be created
     */
    public static final LongSetting CHANGE_QK_EVERY
        = FACTORY.createSettableLongSetting("CHANGE_QK_EVERY", 
                6L*60L*60L*1000L, "change_qk_every", 1L*60L*60L*1000L, 24L*60L*60L*1000L);
}
