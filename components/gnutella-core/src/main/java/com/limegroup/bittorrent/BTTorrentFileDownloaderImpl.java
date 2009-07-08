package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.bittorrent.BTData;
import org.limewire.bittorrent.BTDataImpl;
import org.limewire.bittorrent.bencoding.Token;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventListenerList;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.FileUtils;
import org.limewire.util.Objects;

import com.google.inject.Inject;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.AbstractCoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStateEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.util.LimeWireUtils;

public class BTTorrentFileDownloaderImpl extends AbstractCoreDownloader implements
        BTTorrentFileDownloader, EventListener<DownloadStateEvent> {

    private static Log LOG = LogFactory.getLog(BTTorrentFileDownloaderImpl.class);

    private static final int TIMEOUT = 5000;

    private final DownloadManager downloadManager;

    private final HttpExecutor httpExecutor;

    private final EventListenerList<DownloadStateEvent> eventListenerList;

    private DownloadState downloadStatus = DownloadState.QUEUED;

    private File torrentFile = null;

    private final File incompleteTorrentFile;

    /**
     * Something to shutdown if the user cancels the fetching
     */
    private volatile Shutdownable aborter;

    private URI torrentURI;

    @Inject
    public BTTorrentFileDownloaderImpl(DownloadManager downloadManager,
            SaveLocationManager saveLocationManager, HttpExecutor httpExecutor,
            ActivityCallback activityCallback) {
        super(saveLocationManager);
        this.downloadManager = Objects.nonNull(downloadManager, "downloadManager");
        this.httpExecutor = Objects.nonNull(httpExecutor, "httpExecutor");

        this.eventListenerList = new EventListenerList<DownloadStateEvent>();
        this.incompleteTorrentFile = new File(SharingSettings.INCOMPLETE_DIRECTORY.get(), UUID
                .randomUUID().toString()
                + ".torrent");
        addListener(this);
    }

    private void fetch() {
        final HttpGet get = new HttpGet(torrentURI);
        get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
        get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(), "close");

        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpClientParams.setRedirecting(params, true);
        aborter = httpExecutor.execute(get, params, this);
    }

    @Override
    public boolean requestComplete(HttpUriRequest method, HttpResponse response) {
        aborter = null;
        if (downloadStatus == DownloadState.ABORTED) {
            return false;
        }
        InputStream torrentDownloadStream = null;
        FileOutputStream torrentOutputStream = null;
        FileInputStream torrentInputStream = null;
        try {
            if (response.getStatusLine().getStatusCode() < 200
                    || response.getStatusLine().getStatusCode() >= 300) {
                throw new IOException("bad status code, downloading .torrent file "
                        + response.getStatusLine().getStatusCode());
            }

            if (response.getEntity() != null) {
                torrentDownloadStream = response.getEntity().getContent();
                torrentOutputStream = new FileOutputStream(incompleteTorrentFile);
                FileUtils.write(torrentDownloadStream, torrentOutputStream);
                torrentInputStream = new FileInputStream(incompleteTorrentFile);
                torrentOutputStream.close();
                Map<?, ?> torrentFileMap = (Map<?, ?>) Token.parse(torrentInputStream.getChannel());
                BTData btData = new BTDataImpl(torrentFileMap);

                downloadStatus = DownloadState.COMPLETE;

                // The torrent file is copied into the incomplete file
                // directory.
                torrentFile = new File(SharingSettings.INCOMPLETE_DIRECTORY.get(), btData.getName()
                        + ".torrent");
                if (torrentFile.exists()) {
                    // pass through, when trying to start the BTDownloader a
                    // savelocation exception will occur
                } else {
                    FileUtils.forceRename(incompleteTorrentFile, torrentFile);
                }
            } else {
                throw new IOException("invalid response");
            }
        } catch (IOException iox) {
            downloadStatus = DownloadState.INVALID;
            if (LOG.isErrorEnabled()) {
                LOG.error("Error downloading torrent: " + torrentURI, iox);
            }
        } finally {
            IOUtils.close(torrentInputStream);
            IOUtils.close(torrentDownloadStream);
            IOUtils.close(torrentOutputStream);
            httpExecutor.releaseResources(response);
            deleteIncompleteFiles();
        }

        eventListenerList.broadcast(new DownloadStateEvent(this, downloadStatus));
        return false;
    }

    @Override
    public boolean requestFailed(HttpUriRequest method, HttpResponse response, IOException exc) {
        downloadStatus = DownloadState.INVALID;
        downloadManager.remove(this, true);
        eventListenerList.broadcast(new DownloadStateEvent(this, DownloadState.INVALID));
        return false;
    }

    @Override
    public void discardCorruptDownload(boolean delete) {
    }

    @Override
    public long getAmountLost() {
        return 0;
    }

    @Override
    public int getAmountPending() {
        return 0;
    }

    @Override
    public long getAmountRead() {
        return 0;
    }

    @Override
    public long getAmountVerified() {
        return 0;
    }

    @Override
    public List<RemoteFileDesc> getRemoteFileDescs() {
        return Collections.emptyList();
    }

    @Override
    public int getBusyHostCount() {
        return 0;
    }

    @Override
    public int getChunkSize() {
        return 1;
    }

    @Override
    public long getContentLength() {
        return 0;
    }

    @Override
    public File getDownloadFragment() {
        return null;
    }

    @Override
    public File getFile() {
        return getSaveFile();
    }

    @Override
    public int getNumHosts() {
        return 0;
    }

    @Override
    public List<Address> getSourcesAsAddresses() {
        return Collections.emptyList();
    }

    @Override
    public int getNumberOfAlternateLocations() {
        return 0;
    }

    @Override
    public int getNumberOfInvalidAlternateLocations() {
        return 0;
    }

    @Override
    public int getPossibleHostCount() {
        return 0;
    }

    @Override
    public int getQueuePosition() {
        return 0;
    }

    @Override
    public int getQueuedHostCount() {
        return 0;
    }

    @Override
    public int getRemainingStateTime() {
        return 0;
    }

    @Override
    public URN getSha1Urn() {
        return null;
    }

    @Override
    public File getSaveFile() {
        String uri = torrentURI.toString();
        String name = null;
        if (uri.endsWith(".torrent")) {
            int slash = uri.lastIndexOf("/");
            if (slash != -1)
                name = uri.substring(slash);
        }

        // can't figure it out? show the uri
        if (name == null)
            name = uri;

        return new File(uri);
    }

    @Override
    public void setSaveFile(File saveDirectory, String fileName, boolean overwrite)
            throws DownloadException {
    }

    @Override
    public DownloadState getState() {
        return downloadStatus;
    }

    @Override
    public String getVendor() {
        return null;
    }

    @Override
    public boolean isCompleted() {
        return downloadStatus == DownloadState.COMPLETE;
    }

    @Override
    public boolean isInactive() {
        return false;
    }

    @Override
    public boolean isLaunchable() {
        return false;
    }

    @Override
    public boolean isPausable() {
        return false;
    }

    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean isRelocatable() {
        return false;
    }

    @Override
    public boolean isResumable() {
        return false;
    }

    @Override
    public void pause() {

    }

    @Override
    public boolean resume() {
        return false;
    }

    @Override
    public void stop() {
        finish();
        downloadManager.remove(this, true);
    }

    @Override
    public float getAverageBandwidth() {
        return 0;
    }

    @Override
    public float getMeasuredBandwidth() throws InsufficientDataException {
        return 0;
    }

    @Override
    public void measureBandwidth() {
    }

    @Override
    public int getTriedHostCount() {
        return -1;
    }

    @Override
    public String getCustomIconDescriptor() {
        return null;
    }

    @Override
    public void finish() {
        downloadStatus = DownloadState.ABORTED;
        if (aborter != null) {
            aborter.shutdown();
            aborter = null;
        }
    }

    @Override
    public GUID getQueryGUID() {
        return null;
    }

    @Override
    public void handleInactivity() {

    }

    @Override
    public void initialize() {

    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public boolean isQueuable() {
        return false;
    }

    @Override
    public void setInactivePriority(int priority) {

    }

    @Override
    public boolean shouldBeRemoved() {
        return isCompleted() || downloadStatus == DownloadState.ABORTED
                || downloadStatus == DownloadState.INVALID;
    }

    @Override
    public boolean shouldBeRestarted() {
        return downloadStatus == DownloadState.QUEUED;
    }

    @Override
    public void startDownload() {
        fetch();
    }

    @Override
    public DownloaderType getDownloadType() {
        return DownloaderType.TORRENTFETCHER;
    }

    @Override
    public void addListener(EventListener<DownloadStateEvent> listener) {
        eventListenerList.addListener(listener);
    }

    @Override
    public boolean removeListener(EventListener<DownloadStateEvent> listener) {
        return eventListenerList.removeListener(listener);
    }

    @Override
    public boolean allowRequest(HttpUriRequest request) {
        return true;
    }

    @Override
    public boolean isMementoSupported() {
        return false;
    }

    @Override
    public void initDownloadInformation(URI torrentURI, boolean overwrite) {
        this.torrentURI = torrentURI;
    }

    @Override
    protected DownloadMemento createMemento() {
        return null;
    }

    @Override
    protected File getDefaultSaveFile() {
        return getSaveFile();
    }

    @Override
    public boolean conflicts(URN urn, long fileSize, File... files) {
        return false;
    }

    @Override
    public boolean conflictsWithIncompleteFile(File incomplete) {
        return false;
    }

    @Override
    public void handleEvent(DownloadStateEvent event) {
        if (DownloadState.COMPLETE == event.getType()) {
            downloadManager.remove(this, true);
        }
    }

    @Override
    public void deleteIncompleteFiles() {
        FileUtils.forceDelete(incompleteTorrentFile);
    }

    @Override
    public File getTorrentFile() {
        return torrentFile;
    }
}
