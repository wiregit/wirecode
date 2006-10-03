package com.limegroup.bittorrent;


import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.util.EventDispatcher;

/**
 * A facade for the GUI to treat a single BitTorrent download as a single upload.
 */
public class BTUploader implements Uploader, TorrentEventListener {
	
	private final ManagedTorrent _torrent;
	
	private final BTMetaInfo _info;
	
	private long startTime, stopTime;
	
	private final EventDispatcher<TorrentEvent, TorrentEventListener> dispatcher;

	public BTUploader(ManagedTorrent torrent, BTMetaInfo info,
			EventDispatcher<TorrentEvent, TorrentEventListener> dispatcher) {
		_torrent = torrent;
		_info = info;
		this.dispatcher = dispatcher;
		dispatcher.addEventListener(this);
	}

	public void stop() {
		TorrentEvent stopping = new TorrentEvent(this,
				TorrentEvent.Type.STOP_REQUESTED,
				_torrent);
		dispatcher.dispatchEvent(stopping);
	}

	public String getFileName() {
		return _info.getName();
	}

	public long getFileSize() {
		return _info.getFileSystem().getTotalSize();
	}

	public FileDesc getFileDesc() {
		return _info.getFileDesc();
	}

	public int getIndex() {
		// negative will make sure it never conflicts with regular uploads
		return 0 - Math.abs(_info.getURN().hashCode());
	}
 
	public long amountUploaded() {
		return _torrent.getTotalUploaded();
	}

	public long getTotalAmountUploaded() {
		return _torrent.getTotalUploaded();
	}

	public String getHost() {
		return "Swarm for "+getFileName();
	}

	public int getState() {
		if (!_torrent.isActive()) {
			if (_torrent.isComplete() && _torrent.getRatio() > 1)
				return Uploader.COMPLETE;
			return Uploader.INTERRUPTED;
		}
		return Uploader.UPLOADING;
	}

	public int getLastTransferState() {
		return Uploader.UPLOADING;
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

	public int getQueuePosition() {
		return 0;
	}

	public boolean isInactive() {
		switch(_torrent.getState()) {
		case PAUSED:
		case STOPPED:
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
		return _torrent.getMeasuredBandwidth(false);
	}

	public void handleTorrentEvent(TorrentEvent evt) {
		if (evt.getTorrent() != _torrent)
			return;
		
		switch(evt.getType()) {
		case STARTED : torrentStarted(); break;
		case STOP_APPROVED: _torrent.stop(); break;
		case STOPPED : 
			torrentStopped();
			dispatcher.removeEventListener(this);
			break;
            
        // the below don't need any special handling...
        case COMPLETE:
        case STARTING:
        case STOP_REQUESTED:
		}
	}
	
	public float getAverageBandwidth() {
		long now = stopTime > 0 ? stopTime : System.currentTimeMillis();
		long runTime = (now - startTime);
		return runTime > 0 ? getTotalAmountUploaded() / runTime : 0;
	}
	
	private void torrentStarted() {
		startTime = System.currentTimeMillis();
		stopTime = 0;
		RouterService.getCallback().addUpload(this);
	}
	
	private void torrentStopped() {
		RouterService.getCallback().removeUpload(this);
		stopTime = System.currentTimeMillis();
	}
	
}
