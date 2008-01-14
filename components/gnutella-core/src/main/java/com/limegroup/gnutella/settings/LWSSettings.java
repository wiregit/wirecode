package com.limegroup.gnutella.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for The LimeWire Store&#8482;. This is used by
 * {@link LWSManagerImpl} for the host name to which we connect for
 * authentication.
 */
public final class LWSSettings extends LimeProps {
    
    
    private LWSSettings() {}

    /**
     * The hostname to which we connect for authentication.
     */
    public static final StringSetting LWS_AUTHENTICATION_HOSTNAME = FACTORY.createRemoteStringSetting(
            "LWS_AUTHENTICATION_HOSTNAME", "localhost", "LWSSettings.lwsAuthenticationHostname");

    /**
     * The port on which we connect for authentication. This can be
     * <code><= 0</code> for no port.
     */
    public static final IntSetting LWS_AUTHENTICATION_PORT = FACTORY.createRemoteIntSetting(
            "LWS_AUTHENTICATION_PORT", 8080, "LWSSettings.lwsAuthenticationPort", -Integer.MIN_VALUE,
            10000);
    
    /**
     * The entire prefix to put before a url is downloaded.  This is encoded/decoded many times
     * so needs to be all together.  This also makes it clearer.  <b>This HAS to end in a <code>/</code></b>.
     */
    public static final StringSetting LWS_DOWNLOAD_PREFIX = FACTORY.createRemoteStringSetting(
            "LWS_DOWNLOAD_PREFIX", "localhost:8080", "LWSSettings.lwsDownloadPrefix"); 
    
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
    
    /*
     * These are mainly for the demos when we are using HTTP authentication for accessing the store.
     * Previously we could just send a normal HTTP request from the client to the Store web server,
     * but since adding HTTP authentication to simply access the Store site, we need to use
     * basic authentication to get thru.
     */
    
    /**
     * The username with which we use to connect the client to the Store web server.
     */
    public static final StringSetting LWS_AUTHENTICATION_USERNAME = FACTORY.createRemoteStringSetting(
            "LWS_AUTHENTICATION_USERNAME", "browse", "LWSSettings.lwsAuthenticationUsername"); 
    
    /**
     * The username with which we use to connect the client to the Store web server.
     */
    public static final StringSetting LWS_AUTHENTICATION_PASSWORD = FACTORY.createRemoteStringSetting(
            "LWS_AUTHENTICATION_PASSWORD", "browse", "LWSSettings.lwsAuthenticationPassword");      

}
