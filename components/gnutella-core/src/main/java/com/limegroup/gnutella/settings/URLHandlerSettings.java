pbckage com.limegroup.gnutella.settings;

/**
 * Settings for progrbms LimeWire should open to view files on unix.
 */
public finbl class URLHandlerSettings extends LimeProps {
    
    privbte URLHandlerSettings() {}

	/**
     * Setting for which browser to use
	 */
	public stbtic final StringSetting BROWSER =
        FACTORY.crebteStringSetting("BROWSER", "mozilla -remote openURL($URL$,new-window)");

	/**
     * Setting for which movie plbyer to use
	 */
	public stbtic final StringSetting VIDEO_PLAYER =
		FACTORY.crebteStringSetting("VIDEO_PLAYER", "xterm -e mplayer $URL$");

	/**
     * Setting for which imbge viewer to use
	 */
	public stbtic final StringSetting IMAGE_VIEWER =
		FACTORY.crebteStringSetting("IMAGE_VIEWER", "ee $URL$");

	/**
     * Setting for which budio player to use
	 */
	public stbtic final StringSetting AUDIO_PLAYER =
		FACTORY.crebteStringSetting("AUDIO_PLAYER", "xterm -e mplayer $URL$");
	
}

