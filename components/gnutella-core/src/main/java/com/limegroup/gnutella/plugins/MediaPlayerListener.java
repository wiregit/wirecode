package com.limegroup.gnutella.plugins;

public interface MediaPlayerListener {

	/**
	 * - signifies when the file has finished 'playing'
	 */
	public void playComplete();

	/**
	 * - informs client that the play (frame) position has
	 *   changed to value
	 */
	public void updatePlayPosition(int value);

    /**
	 * setUpSeek
	 *
	 * - called before playing the media file
	 * Use this to setup any presentation of the media.
	 */
	public void setUpSeek(int lengthInFrames);
}
