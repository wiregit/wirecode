package com.limegroup.bittorrent.disk;

/**
 * Listener for callbacks from a <tt>TorrentDiskManager</tt>
 */
public interface DiskManagerListener {

	/**
	 * Notification that a disk error has happened.
	 */
	public void diskExceptionHappened();

	/**
	 * Notification that verification previously existing data
	 * has completed.
	 */
	public void verificationComplete();

	/**
	 * notification that a chunk has been completed and verified
	 * @param in the # of the verified chunk
	 */
	public void chunkVerified(int id);

}