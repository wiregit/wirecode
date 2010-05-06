package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for The LimeWire Store&#8482;. This is used by
 * {@link LWSManagerImpl} for the host name to which we connect for
 * authentication.
 */
public final class LWSSettings extends LimeProps {
    
    
    private LWSSettings() {}
    
    
    /**
     * Base32 encoded public key issued by store web-server
     * used for authenticating download requests.
     */
    public static final StringSetting LWS_PUBLIC_KEY = FACTORY.createRemoteStringSetting(
            "LWS_PUBLIC_KEY", "GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMALQOLFQHEP6MTTYBXPIXR4NDJQSXFRDO4RWJBS4OCG4C3B2RP2ICYADOS5S3M5LHS2BBRUNEEBZRDTPJBYCCKAJLDNWLMO7IYPL3BQIMHTHH5I5MDIT2YKJLC3OUZI25YHMVNS735UV4T7XVUJA5B4XSWK223JWCL63PFIAT33QYFQGRXEJ47T4DZT4M3KYGGFXO6DZMLMLIPK", "LWSSettings.lwsPublicKey");

    /**
     * The hostname to which we connect for authentication.
     * <br>e.g. <code>1.2.3.4</code>
     */
    public static final StringSetting LWS_AUTHENTICATION_HOSTNAME = FACTORY.createRemoteStringSetting(
            "LWS_AUTHENTICATION_HOSTNAME", "www.store.limewire.com", "LWSSettings.lwsAuthenticationHostname");

    /**
     * The port on which we connect for authentication. This can be
     * <code><= 0</code> for no port.
     * <br>e.g. <code>80</code>
     */
    public static final IntSetting LWS_AUTHENTICATION_PORT = FACTORY.createRemoteIntSetting(
            "LWS_AUTHENTICATION_PORT", 80, "LWSSettings.lwsAuthenticationPort", -Integer.MIN_VALUE,
            10000);
    
    /**
     * The path to the home page.
     */
    public static final StringSetting LWS_AUTHENTICATION_PATH = FACTORY.createRemoteStringSetting(
            "LWS_AUTHENTICATION_PATH", "/", "LWSSettings.lwsAuthenticationPath");
    
    /**
     * Allow us to turn on/off SSL messages to the Server.
     */
    public static final BooleanSetting LWS_USE_SSL = FACTORY.createRemoteBooleanSetting(
            "LWS_USE_SSL", false, "LWSSettings.lwsUseSSL");    
    

    /**
     * The entire prefix to put before a url is downloaded.  This is encoded/decoded many times
     * so needs to be all together.  This also makes it clearer.  <b>This HAS to end in a <code>/</code></b>.
     * <br>e.g. <code>1.2.3.4:80</code>
     */
    public static final StringSetting LWS_DOWNLOAD_PREFIX = FACTORY.createRemoteStringSetting(
            "LWS_DOWNLOAD_PREFIX", "www.store.limewire.com:80", "LWSSettings.lwsDownloadPrefix"); 
    
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
    
    /**
     * A list of country codes where its appropriate to show the LWS.
     */
    public static final StringArraySetting LWS_VALID_COUNTRY_CODES = FACTORY.createRemoteStringArraySetting(
            "LWS_VALID_COUNTRY_CODES", new String[]{"US"}, "LWSSettings.lwsValidCountryCodes");

    /**
     * True if the geolocation has been located for a new install,
     * false otherwise.
     */
    public static final BooleanSetting HAS_LOADED_LWS_GEO =
        FACTORY.createBooleanSetting("HAS_LOADED_LWS_GEO", false);
   
}
