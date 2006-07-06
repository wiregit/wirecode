package com.limegroup.bittorrent;

import java.io.IOException;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.http.HTTPRequestMethod;

/**
 * A facade for the GUI to treat a single BitTorrent download as a single upload.
 */
public class BTUploader implements Uploader, TorrentLifecycleListener {
	
	private final ManagedTorrent _torrent;
	
	private final BTMetaInfo _info;
	
	private final TorrentPrompt _prompt;

	private long startTime, stopTime;

	public BTUploader(ManagedTorrent torrent, BTMetaInfo info, TorrentPrompt prompt) {
		_torrent = torrent;
		_info = info;
		_prompt = prompt;
	}

	public void stop() {
		boolean stop = true;
		
		if (_torrent.isDownloading())
			stop = _prompt.promptAboutStopping();
		else if (_torrent.getState() == ManagedTorrent.SEEDING && 
				_torrent.getTotalUploaded() < _torrent.getTotalDownloaded())
			stop = _prompt.promptAboutSeeding();

		if (stop)
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
		return _torrent.getTotalUploaded();
	}

	public long getTotalAmountUploaded() {
		return _torrent.getTotalUploaded();
	}

	public String getHost() {
		return "Multiple";
	}

	public int getState() {
		if (!_torrent.isActive())
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
		switch(_torrent.getState()) {
		case ManagedTorrent.PAUSED:
		case ManagedTorrent.STOPPED:
			return true;
		}
		return false;
	}

	public void measureBandwidth() {
		_torrent.measureBandwidth();
	}

	public float getMeasuredBandwidth() throws InsufficientDataException {
		if (!_torrent.isActive())
			return 0.f;
		return _torrent.getMeasuredBandwidth(false) / 1024;
	}

	public float getAverageBandwidth() {
		long now = stopTime > 0 ? stopTime : System.currentTimeMillis();
		long runTime = (now - startTime);
		return runTime > 0 ? getTotalAmountUploaded() / runTime : 0;
	}
	
	public void torrentStarted(ManagedTorrent t) {
		if (t == _torrent) {
			startTime = System.currentTimeMillis();
			stopTime = 0;
			RouterService.getCallback().addUpload(this);
		}
	}
	
	public void torrentStopped(ManagedTorrent t) {
		if (t == _torrent) {
			RouterService.getCallback().removeUpload(this);
			stopTime = System.currentTimeMillis();
		}
	}
	
	public void torrentComplete(ManagedTorrent t){
		// nothing.
	}
}
