package com.limegroup.gnutella.torrent;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerState;
import org.gudy.azureus2.core3.global.GlobalManagerDownloadRemovalVetoException;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderFactory;
import org.gudy.azureus2.core3.torrentdownloader.impl.TorrentDownloaderManager;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.DisplayFormatters;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.SaveLocationException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.ManagedDownloader;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;

public class AzTorrentDownloader implements Downloader {
	
	private static final Log LOG = LogFactory.getLog(AzTorrentDownloader.class);
	
	private final AzureusManager azManager = RouterService.getAzureusManager();
	
	private static final File outputDir = SharingSettings.getSaveDirectory();
	
	private DownloadManager dlmanager;
	
	/**
     * A map of attributes associated with the download. The attributes
     * may be used by GUI, to keep some additional information about
     * the download.
     */
    protected Map attributes = new HashMap();
	
	public AzTorrentDownloader(String torrentURL) throws MalformedURLException{
		new URL(torrentURL);
		addRemoteTorrent(torrentURL);
	}
	
	//TODO build abstract supertype and then implement AzRemoteTorrentDownloader, 
	//AzExistingTorrentDownloader and AzLocalTorrentDownloader
	public AzTorrentDownloader(DownloadManager manager) {
		dlmanager = manager;
		RouterService.getCallback().addDownload(this);
	}
	
	private void addRemoteTorrent(String url) {
		TorrentDownloader downloader = TorrentDownloaderFactory.create(new TorrentDownloaderCallBackInterface() {
			public void TorrentDownloaderEvent(int state, TorrentDownloader inf) {
				if( state == TorrentDownloader.STATE_FINISHED )
				{
					System.out.println("torrent file download complete. starting torrent");
					TorrentDownloaderManager.getInstance().remove(inf);
					downloadTorrent( inf.getFile().getAbsolutePath(), outputDir.getAbsolutePath() );
				}
				else
					TorrentDownloaderManager.getInstance().TorrentDownloaderEvent(state, inf);
			}
		}, url, null, null, true);
		TorrentDownloaderManager.getInstance().add(downloader);
	}
	
	/**
	 * begins the download of the torrent in the specified file, downloading
	 * it to the specified output directory. We also annotate the download with the
	 * current username
	 * @param filename
	 * @param outputDir
	 */
	private void downloadTorrent( String filename, String outputDir )
	{
		dlmanager = azManager.getGlobalManager().addDownloadManager(filename, outputDir);
		dlmanager.getDownloadState().setAttribute(DownloadManagerState.AT_USER, CommonUtils.getUserName());
		RouterService.getCallback().addDownload(this);
	}
	
	public void stop() {
		if(dlmanager!=null) {
			dlmanager.stopIt(DownloadManager.STATE_STOPPED, false, false);
			try {
				azManager.getGlobalManager().removeDownloadManager(dlmanager);
			} catch (GlobalManagerDownloadRemovalVetoException e) {
				e.printStackTrace();
			}
		}
	}

	public void pause() {
		dlmanager.pause();
	}

	public boolean isPaused() {
		return dlmanager.isPaused();
	}

	public boolean isInactive() {
		switch(dlmanager.getState()) {
		case DownloadManager.STATE_ERROR:
		case DownloadManager.STATE_QUEUED:
		case DownloadManager.STATE_READY:
		case DownloadManager.STATE_STOPPED:
		case DownloadManager.STATE_WAITING:
			//TODO maybe a few more?
			return true;
		}
		return false;
	}

	public boolean isRelocatable() {
		return false; //TODO for now
	}

	public int getInactivePriority() {
		// TODO for now
		return 0;
	}

	public boolean resume() {
//		dlmanager.resume();
		dlmanager.setForceStart(true);
		return true; //TODO for now
	}

	public File getFile() {
		return new File(dlmanager.getTorrentFileName());
	}

	public File getDownloadFragment() {
		return null; //TODO for now
	}

	public void setSaveFile(File saveDirectory, String fileName,
			boolean overwrite) throws SaveLocationException {
//		dlmanager.setTorrentSaveDir() unsecure 
		//TODO do nothing for now
	}

	public File getSaveFile() {
		return new File(dlmanager.getTorrentFileName());//TODO for now
	}

	public int getState() {
		return dlmanager.getState();
	}

	public int getRemainingStateTime() {
		return Integer.MAX_VALUE; //TODO for now
	}

	public int getContentLength() {
		//TODO temporary hack
		return (int)(dlmanager.getSize());
	}

	public int getAmountRead() {
		return (int)(dlmanager.getStats().getTotalGoodDataBytesReceived());
	}

	public int getAmountPending() {
		return (int)(dlmanager.getStats().getTotalProtocolBytesReceived()-dlmanager.getSize());
	}

	public int getNumHosts() {
		return dlmanager.getNbPeers()+dlmanager.getNbSeeds(); //TODO for now
	}

	public String getVendor() {
//		return dlmanager.get
		return "Azureus"; //TODO for now
	}

	public Endpoint getChatEnabledHost() {
		return null; //TODO for now
	}

	public boolean hasChatEnabledHost() {
		return false; //TODO for now
	}

	public void discardCorruptDownload(boolean delete) {
		//TODO for now
	}

	public RemoteFileDesc getBrowseEnabledHost() {
//		TODO for now
		return null;
	}

	public boolean hasBrowseEnabledHost() {
		return false;//TODO for now
	}

	public int getQueuePosition() {
		return -1; //for now
	}

	public int getNumberOfAlternateLocations() {
		return 0;//TODO for now
	}

	public int getNumberOfInvalidAlternateLocations() {
		return 0;//TODO for now
	}

	public int getPossibleHostCount() {
		return dlmanager.getTrackerScrapeResponse().getPeers();
	}

	public int getBusyHostCount() {
		return 0; //TODO for now
	}

	public int getQueuedHostCount() {
		return 0; //TODO for now
	}

	public boolean isCompleted() {
		return dlmanager.isDownloadComplete();
	}

	public int getAmountVerified() {
		return 0; //TODO for now
	}

	public int getChunkSize() {
		return 0; //TODO for now
	}

	public int getAmountLost() {
		return 0; //TODO for now
	}

	public URN getSHA1Urn() {
		//TODO for now
		return null;
	}

	public Object setAttribute(String key, Object value) {
		return attributes.put( key, value );
	}

	public Object getAttribute(String key) {
		return attributes.get( key );
	}

	public Object removeAttribute(String key) {
		return attributes.remove( key );
	}

	public void measureBandwidth() {
		// TODO Auto-generated method stub

	}

	public float getMeasuredBandwidth() throws InsufficientDataException {
		//		TODO temporary hack
		long l = dlmanager.getStats().getDataReceiveRate() + dlmanager.getStats().getProtocolReceiveRate();
		return l/1024;
	}

	public float getAverageBandwidth() {
//		TODO temporary hack
		long l = dlmanager.getStats().getDataReceiveRate() + dlmanager.getStats().getProtocolReceiveRate();
		return l/1024;
	}
	
}
