package com.limegroup.gnutella.plugins;

/** This interface is a more fully functional Player interface, with more
    standard functions.  This interface is also a MinimalPlayer.
*/
public interface MediaPlayerInterface extends MinimalPlayerInterface {

    /** final variable used to keep track of MediaPlayer status
     */
	public static final int STATUS_PLAYING = 0;
    /** final variable used to keep track of MediaPlayer status
     */
	public static final int STATUS_PAUSED  = 1;
    /** final variable used to keep track of MediaPlayer status
     */
	public static final int STATUS_STOPPED = 2;

	/**
	 * getStatus
	 *
	 * - called by client to determine what
	 *   the MediaPlayerInterface is doing at that momemt
	 * - returns STATUS_PLAYING, STATUS_STOPPED, STATUS_PAUSED
	 *
	 */
	public int getStatus();


    /** Pause play of the currently playing media.
     */
    public void pause();
    /** Unpause play of the currently playing media.
     */
    public void unpause();

    /** @return the 'frame length' of the media file.  a 'frame' may be seconds,
        motion frames, or some other media dependent entity. namely, this value
        can be used to set up seek or progress bars.
    */
    public int getFrameLength();

    /** The concept of a frame is media dependent.  in general, a media file has
        a 'length' (which can be retrieved by getFrameLength()).  this allows
        you to seek (negatively or positively) the media.
    */
    public void seek(int numFrames);


    /** Add a listener for this instance.
     */
    public void addListener(MediaPlayerListener mpl);

}
