package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.FloatSetting;
import org.limewire.setting.IntSetting;

/**
 * Settings for uploads.
 */
public final class UploadSettings extends LimeProps {
    
    private UploadSettings() {}
    
    
    /**
     * The maximum number of pushes to allow from a single host
     * within a 30 second span.
     */
    public static final IntSetting MAX_PUSHES_PER_HOST =
        FACTORY.createIntSetting("MAX_PUSHES_PER_HOST", 5);

    /**
     * The maximum percentage of estimated upload bandwidth to use for uploads.
     */
    public static final IntSetting UPLOAD_SPEED =
        FACTORY.createIntSetting("UPLOAD_SPEED", 100);

    /**
     * Setting for the size of the upload queue.
     */
    public static final IntSetting UPLOAD_QUEUE_SIZE =
        FACTORY.createIntSetting("UPLOAD_QUEUE_SIZE", 10);
        
    /**
     * Setting for the number of uploads per person.
     */
    public static final IntSetting UPLOADS_PER_PERSON =
        FACTORY.createIntSetting("UPLOADS_PER_PERSON_2", 3);
        
    /**
     * The maximum number of upstream bytes per second ever passed by
     * this node.
     */
    public static final IntSetting MAX_UPLOAD_BYTES_PER_SEC =
        FACTORY.createExpirableIntSetting("MAX_UPLOAD_BYTES_PER_SEC", 0);
     
    /**
     * The maximum number of simultaneous uploads to allow.
     */
    public static final IntSetting HARD_MAX_UPLOADS =
        FACTORY.createIntSetting("HARD_MAX_UPLOADS", 20);
    
    /**
     * The "soft" maximum number of simultaneous uploads to allow,
     * i.e., the minimum number of people to allow before determining
     * whether to allow more uploads.
     */
    public static final IntSetting SOFT_MAX_UPLOADS =
        FACTORY.createIntSetting("SOFT_MAXIMUM_UPLOADS_2", 5);   

    /**
     * Settings whether to expire the different types of meshes.
     */
    public static final BooleanSetting EXPIRE_LEGACY =
        FACTORY.createRemoteBooleanSetting("EXPIRE_LEGACY",true,"AlternateLocation.expireLegacy");
    public static final BooleanSetting EXPIRE_PING =
        FACTORY.createRemoteBooleanSetting("EXPIRE_PING",true,"AlternateLocation.expirePing");
    public static final BooleanSetting EXPIRE_RESPONSE =
        FACTORY.createRemoteBooleanSetting("EXPIRE_RESPONSES",true,"AlternateLocation.expireResponse");
    
    /**
     * Settings for the number of times each altloc should be given out
     * (larger == more times).
     */
    public static final FloatSetting LEGACY_BIAS =
        FACTORY.createRemoteFloatSetting("LEGACY_BIAS",1f,"AlternateLocation.legacyBias",0.0f,100.0f);
    public static final FloatSetting PING_BIAS =
        FACTORY.createRemoteFloatSetting("PING_BIAS",1f,"AlternateLocation.pingBias",0.0f,100.0f);
    public static final FloatSetting RESPONSE_BIAS = // send altlocs in responses more often by default
        FACTORY.createRemoteFloatSetting("RESPONSE_BIAS",3f,"AlternateLocation.responseBias",0.0f,100.0f);
    

    /**
     * Settings for the speed at which the number of times an altloc can be given out regrows 
     * (smaller == faster).
     */
    public static final FloatSetting LEGACY_EXPIRATION_DAMPER =
        FACTORY.createRemoteFloatSetting("LEGACY_DAMPER",
                (float)Math.E/2,"AlternateLocation.legacyDamper",
                (float)Math.E/100,
                (float)Math.E-0.1f);
    public static final FloatSetting PING_EXPIRATION_DAMPER =
        FACTORY.createRemoteFloatSetting("PING_DAMPER",
                    (float)Math.E/2,"AlternateLocation.pingDamper",
                    (float)Math.E/100,
                    (float)Math.E-0.1f);
    public static final FloatSetting RESPONSE_EXPIRATION_DAMPER =
        FACTORY.createRemoteFloatSetting("RESPONSE_DAMPER",
                (float)Math.E/2,"AlternateLocation.responseDamper",
                (float)Math.E/100,
                (float)Math.E-0.1f);
    
    /**
     * How much to throttle the Thex upload speed.
     */
    public static final IntSetting THEX_UPLOAD_SPEED =
        FACTORY.createRemoteIntSetting("THEX_UPLOAD_SPEED",512,
                "THEXUploadState.ThexUploadSpeed",256,4*1024);
    
    public static final BooleanSetting CHECK_DUPES =
        FACTORY.createBooleanSetting("CHECK_DUPE_UPLOADS", true);
}
