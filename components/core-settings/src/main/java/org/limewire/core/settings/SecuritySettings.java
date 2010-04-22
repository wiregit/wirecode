package org.limewire.core.settings;

import org.limewire.setting.IntSetting;
import org.limewire.setting.LongSetting;
import org.limewire.setting.StringSetting;

/**
 * Security Settings.
 */
public final class SecuritySettings extends LimeProps {
    
    private SecuritySettings() {}
    
    /**
     * The interval in which a new KeyGenerator(s) will be created.
     */
    public static final LongSetting CHANGE_QK_EVERY
        = FACTORY.createRemoteLongSetting("CHANGE_QK_EVERY", 
                6L*60L*60L*1000L, "change_qk_every", 1L*60L*60L*1000L, 24L*60L*60L*1000L);
    
    /**
     * The grace period for which an old QK stays valid.
     */
    public static final LongSetting QK_GRACE_PERIOD
        = FACTORY.createRemoteLongSetting("QK_GRACE_PERIOD", 
                20L*60L*1000L, "qk_grace_period", 1L*60L*1000L, 24L*60L*60L*1000L);
    
    public static final StringSetting MY_ID=
        FACTORY.createStringSetting("MY_ID", "");

    public static final StringSetting MY_IDENTITY_SIGNATURE=
        FACTORY.createStringSetting("MY_IDENTITY_SIGNATURE", "");

    public static final StringSetting SIGNATURE_PUBLIC_KEY= 
        FACTORY.createStringSetting("SIGNATURE_PUBLIC_KEY", "");

    public static final StringSetting SIGNATURE_PRIVATE_KEY= 
        FACTORY.createStringSetting("SIGNATURE_PRIVATE_KEY", "");

    public static final StringSetting DH_PUBLIC_KEY= 
        FACTORY.createStringSetting("DH_PUBLIC_KEY", "");

    public static final StringSetting DH_PRIVATE_KEY= 
        FACTORY.createStringSetting("DH_PRIVATE_KEY", "");
    
    public static final IntSetting MULTI_INSTALLATION_MARK=
        FACTORY.createIntSetting("MULTI_INSTALLATION_MARK", 0);
}