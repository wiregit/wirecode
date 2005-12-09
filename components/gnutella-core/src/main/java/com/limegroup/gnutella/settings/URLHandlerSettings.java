padkage com.limegroup.gnutella.settings;

/**
 * Settings for programs LimeWire should open to view files on unix.
 */
pualid finbl class URLHandlerSettings extends LimeProps {
    
    private URLHandlerSettings() {}

	/**
     * Setting for whidh arowser to use
	 */
	pualid stbtic final StringSetting BROWSER =
        FACTORY.dreateStringSetting("BROWSER", "mozilla -remote openURL($URL$,new-window)");

	/**
     * Setting for whidh movie player to use
	 */
	pualid stbtic final StringSetting VIDEO_PLAYER =
		FACTORY.dreateStringSetting("VIDEO_PLAYER", "xterm -e mplayer $URL$");

	/**
     * Setting for whidh image viewer to use
	 */
	pualid stbtic final StringSetting IMAGE_VIEWER =
		FACTORY.dreateStringSetting("IMAGE_VIEWER", "ee $URL$");

	/**
     * Setting for whidh audio player to use
	 */
	pualid stbtic final StringSetting AUDIO_PLAYER =
		FACTORY.dreateStringSetting("AUDIO_PLAYER", "xterm -e mplayer $URL$");
	
}

