package com.limegroup.bittorrent;

import java.io.IOException;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.http.HTTPRequestMethod;

/**
 * This class enables the GUI to treat a Torrent like any other upload. I'm
 * not actually happy with this solution but I don't want to spend time on the GUI
 * 
 * TODO find a proper solution.
 */
public class BTUploader implements Uploader {
	private ManagedTorrent _torrent;
	
	private BTMetaInfo _info;

	private SimpleBandwidthTracker _tracker;

	public BTUploader(ManagedTorrent torrent, BTMetaInfo info) {
		_torrent = torrent;
		_tracker = new SimpleBandwidthTracker();
		_info = info;
	}

	public void stop() {
		_torrent.stop();
	}

	public String getFileName() {
		return _info.getName();
	}

	public long getFileSize() {
		return _info.getTotalSize();
	}

	public long getAmountRequested() {
		// not used in GUI...
		return 0;
	}

	public FileDesc getFileDesc() {
		return _info.getFileDesc();
	}

	public int getIndex() {
		return _info.getFileDesc().getIndex();
	}

	public long amountUploaded() {
		return _tracker.getTotalAmount();
	}

	public long getTotalAmountUploaded() {
		return _tracker.getTotalAmount();
	}

	public String getHost() {
		return "Multiple";
	}

	public int getState() {
		if (_torrent.hasStopped())
			return Uploader.INTERRUPTED;
		return Uploader.UPLOADING;
	}

	public int getLastTransferState() {
		return Uploader.UPLOADING;
	}

	public void setState(int state) {
		// do nothing
	}

	public void writeResponse() throws IOException {
		// do nothing
	}

	public boolean isChatEnabled() {
		return false;
	}

	public boolean isBrowseHostEnabled() {
		return false;
	}

	public int getGnutellaPort() {
		return 0;
	}

	public String getUserAgent() {
		return "BitTorrent";
	}

	public boolean isHeaderParsed() {
		return true;
	}

	public boolean supportsQueueing() {
		return false;
	}

	public HTTPRequestMethod getMethod() {
		return HTTPRequestMethod.GET;
	}

	public int getQueuePosition() {
		return 0;
	}

	public boolean isInactive() {
		return false;
	}

	public void measureBandwidth() {
		_tracker.measureBandwidth();
	}

	public float getMeasuredBandwidth() {
		if (_torrent.hasStopped())
			return 0.f;
		measureBandwidth();
		return _tracker.getMeasuredBandwidth();
	}

	public float getAverageBandwidth() {
		return _tracker.getAverageBandwidth();
	}
	
	void wroteBytes(int written) {
		_tracker.count(written);
	}
}
