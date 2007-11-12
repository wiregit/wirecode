package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for The Lime Wire Store&#8482;. This is used by
 * {@link LWSManagerImpl} for the host name to which we connect for
 * authentication.
 */
public final class LWSSettings extends LimeProps {
    
    
    private LWSSettings() {}

    /**
     * The hostname to which we connect for authentication.
     */
    public static final StringSetting LWS_AUTHENTICATION_HOSTNAME = FACTORY.createRemoteStringSetting(
            "LWS_AUTHENTICATION_HOSTNAME", "", "LWSSettings.lwsAuthenticationHostname");

    /**
     * The port on which we connect for authentication. This can be
     * <code><= 0</code> for no port.
     */
    public static final IntSetting LWS_AUTHENTICATION_PORT = FACTORY.createRemoteIntSetting(
            "LWS_AUTHENTICATION_PORT", 8080, "LWSSettings.lwsAuthenticationPort", -Integer.MIN_VALUE,
            10000);
    
    /**
     * The hostname to which we connect for downloads.
     */
    public static final StringSetting LWS_DOWNLOAD_HOSTNAME = FACTORY.createRemoteStringSetting(
            "LWS_DOWNLOAD_HOSTNAME", "", "LWSSettings.lwsDownloadHostname");

    /**
     * The port on which we connect for downloads. This can be
     * <code><= 0</code> for no port.
     */
    public static final IntSetting LWS_DOWNLOAD_PORT = FACTORY.createRemoteIntSetting(
            "LWS_DOWNLOAD_PORT", 80, "LWSSettings.lwsDownloadPost", -Integer.MIN_VALUE,
            10000);   
    
    /**
     * The hostname to which we connect for adding to playlists.
     */
    public static final StringSetting LWS_ADD_TO_PLAYLIST_HOSTNAME = FACTORY.createRemoteStringSetting(
            "LWS_ADD_TO_PLAYLIST_HOSTNAME", "", "LWSSettings.lwsAddToPlaylistHostname");

    /**
     * The port on which we connect for adding to playlists. This can be
     * <code><= 0</code> for no port.
     */
    public static final IntSetting LWS_ADD_TO_PLAYLIST_PORT = FACTORY.createRemoteIntSetting(
            "LWS_ADD_TO_PLAYLIST_PORT", 80, "LWSSettings.lwsAddToPlaylistPost", -Integer.MIN_VALUE,
            10000);  

    /**
     * Allow us to disable the lws server.
     */
    public static final BooleanSetting LWS_IS_ENABLED = FACTORY.createRemoteBooleanSetting(
            "LWS_IS_ENABLED", true, "LWSSettings.lwsIsEnabled");

}
