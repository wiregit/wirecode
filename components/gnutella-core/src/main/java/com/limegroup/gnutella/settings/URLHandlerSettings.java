package com.limegroup.gnutella.settings;

/**
 * Settings for programs LimeWire should open to view files on unix.
 */
public final class URLHandlerSettings extends LimeProps {
    
    private URLHandlerSettings() {}

	/**
     * Setting for which browser to use
	 */
	public static final StringSetting BROWSER =
        FACTORY.createStringSetting("BROWSER", "mozilla -remote openURL($URL$,new-window)");

	/**
     * Setting for which movie player to use
	 */
	public static final StringSetting VIDEO_PLAYER =
		FACTORY.createStringSetting("VIDEO_PLAYER", "xterm -e mplayer $URL$");

	/**
     * Setting for which image viewer to use
	 */
	public static final StringSetting IMAGE_VIEWER =
		FACTORY.createStringSetting("IMAGE_VIEWER", "ee $URL$");

	/**
     * Setting for which audio player to use
	 */
	public static final StringSetting AUDIO_PLAYER =
		FACTORY.createStringSetting("AUDIO_PLAYER", "xterm -e mplayer $URL$");
	
}

