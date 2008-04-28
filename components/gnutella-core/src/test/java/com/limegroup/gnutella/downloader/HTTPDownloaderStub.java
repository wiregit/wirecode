package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.InetAddress;

import org.limewire.io.NetworkInstanceUtils;

import com.google.inject.Provider;
import com.limegroup.gnutella.BandwidthManager;
import com.limegroup.gnutella.CreationTimeCache;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.PushEndpointCache;
import com.limegroup.gnutella.PushEndpointFactory;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.AlternateLocationCollection;
import com.limegroup.gnutella.altlocs.AlternateLocationFactory;
import com.limegroup.gnutella.http.ProblemReadingHeaderException;
import com.limegroup.gnutella.statistics.TcpBandwidthStatistics;
import com.limegroup.gnutella.tigertree.ThexReaderFactory;

/**
 * stubbed out HTTPDownloader.  Extend this stub to override specific behavior
 */
public class HTTPDownloaderStub extends HTTPDownloader {

    public HTTPDownloaderStub(RemoteFileDesc rfd, VerifyingFile incompleteFile,
            NetworkManager networkManager, AlternateLocationFactory alternateLocationFactory,
            DownloadManager downloadManager, CreationTimeCache creationTimeCache,
            BandwidthManager bandwidthManager, Provider<PushEndpointCache> pushEndpointCache,
            PushEndpointFactory pushEndpointFactory, RemoteFileDescFactory remoteFileDescFactory,
            ThexReaderFactory thexReaderFactory, TcpBandwidthStatistics tcpBandwidthStatistics,
            NetworkInstanceUtils networkInstanceUtils) {
        super(null, rfd, incompleteFile, false, false, networkManager, alternateLocationFactory,
                downloadManager, creationTimeCache, bandwidthManager, pushEndpointCache,
                pushEndpointFactory, remoteFileDescFactory, thexReaderFactory,
                tcpBandwidthStatistics, networkInstanceUtils);
    }

    public void addFailedAltLoc(AlternateLocation loc) {

    }

    public void addSuccessfulAltLoc(AlternateLocation loc) {

	}
	public boolean browseEnabled() {
		return false;
	}
	public boolean chatEnabled() {
		return false;
	}
	public void connectHTTP(int start, int stop, boolean supportQueueing)
			throws IOException, TryAgainLaterException, FileNotFoundException,
			NotSharingException, QueuedException, RangeNotAvailableException,
			ProblemReadingHeaderException, UnknownCodeException {


	}
	public void initializeTCP() throws IOException {

	}
	public void consumeBodyIfNecessary() {

	}
	public void doDownload(VerifyingFile commonOutFile) throws IOException {

	}
	AlternateLocationCollection getAltLocsReceived() {
		return null;
	}
	public long getAmountRead() {
		return 0;
	}
	public long getAmountToRead() {
		return 0;
	}
	public float getAverageBandwidth() {
		return 0f;
	}
	public String getFileName() {
		return null;
	}
	public byte[] getGUID() {
		return null;
	}
	public long getIndex() {
		return 0l;
	}
	public InetAddress getInetAddress() {
		return null;
	}
	public long getInitialReadingPoint() {
		return 0;
	}
	public float getMeasuredBandwidth() throws InsufficientDataException {
		return 0f;
	}
	public int getPort() {
		return 0;
	}
	public RemoteFileDesc getRemoteFileDesc() {
		return null;
	}
	public long getTotalAmountRead() {
		return 0;
	}
	public String getVendor() {
		return null;
	}
	public boolean hasHashTree() {
		return true;
	}
	public boolean isActive() {
		return false;
	}
	public boolean isHTTP11() {
		return false;
	}
	public boolean isPush() {
		return false;
	}
	public void measureBandwidth() {
		
	}
	public ConnectionStatus requestHashTree() {
		return null;
	}
	public void stop() {
		
	}
	public void stopAt(int stop) {
		
	}
	public String toString() {
		return "HTTPDownloader stub";
	}
}
