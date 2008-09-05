package org.limewire.core.impl.download;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadListener;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.CoreSearch;
import org.limewire.core.impl.search.MediaTypeConverter;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.core.settings.MozillaSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.setting.FileSetting;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.internal.base.Objects;
import com.google.inject.name.Named;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;


@Singleton
public class CoreDownloadListManager implements DownloadListManager {
    
    private static final Log LOG = LogFactory.getLog(CoreDownloadListManager.class);
    
	private final EventList<DownloadItem> downloadItems;
	private final DownloadManager downloadManager;
	private final RemoteFileDescFactory remoteFileDescFactory;
	private final QueueTimeCalculator queueTimeCalculator;
    private final ScheduledExecutorService backgroundExecutor;
    private static final int PERIOD = 1000;
	
	@Inject
	public CoreDownloadListManager(DownloadManager downloadManager,
            DownloadListenerList listenerList, @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor,
            RemoteFileDescFactory remoteFileDescFactory) {
	    this.backgroundExecutor = Objects.nonNull(backgroundExecutor, "backgroundExecutor");
	    this.downloadManager = downloadManager;
	    this.remoteFileDescFactory = remoteFileDescFactory;
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = GlazedLists.threadSafeList(
	            new ObservableElementList<DownloadItem>(new BasicEventList<DownloadItem>(), downloadConnector));
	    this.queueTimeCalculator = new QueueTimeCalculator(downloadItems);
	    listenerList.addDownloadListener(new CoreDownloadListener(downloadItems, queueTimeCalculator));
	    
	  //TODO: change backgroundExecutor to listener - currently no listener for download progress
      //hack to force tables to update
	    Runnable command = new Runnable() {
            @Override
            public void run() {
                update();
            }
        };
        backgroundExecutor.scheduleAtFixedRate(command, 0, PERIOD, TimeUnit.MILLISECONDS);

	}

    // forces refresh
    private void update() {
        downloadItems.getReadWriteLock().writeLock().lock();
        try {
            // TODO use TransactionList for these for performance (requires using GlazedLists from head)
            for (DownloadItem item : downloadItems) {
                if (item instanceof CoreDownloadItem)
                    ((CoreDownloadItem) item).fireDataChanged();
            }

        } finally {
            downloadItems.getReadWriteLock().writeLock().unlock();
        }
    }
	
	@Override
	public EventList<DownloadItem> getDownloads() {
		return downloadItems;
	}

	@Override
	public DownloadItem addDownload(Search search, List<? extends SearchResult> searchResults) throws SaveLocationException {
        File saveDir = null; // TODO
        String fileName = null; // TODO
        boolean overwrite = false; // TODO
        
        RemoteFileDesc[] files;
        List<RemoteFileDesc> alts = new ArrayList<RemoteFileDesc>();
        files = createRfdsAndAltsFromSearchResults(searchResults, alts);
        
        GUID queryGUID = null;
        if(search != null && search instanceof CoreSearch) {
            queryGUID = ((CoreSearch)search).getQueryGuid();
        }
                
        // determine per mediatype directory if saveLocation == null
        // and only pass it through if directory is different from default
        // save directory == !isDefault()
        if (/*saveDir == null && */ search != null) {
            FileSetting fs = SharingSettings.getFileSettingForMediaType
            (MediaTypeConverter.toMediaType(search.getCategory()));
            if (!fs.isDefault()) {
                saveDir = fs.getValue();
            }
        }
        
        Downloader downloader = downloadManager.download(files, alts, queryGUID, overwrite, saveDir, fileName);
        
        return new CoreDownloadItem(downloader, queueTimeCalculator);
	}

	
	private RemoteFileDesc[] createRfdsAndAltsFromSearchResults(
            List<? extends SearchResult> searchResults, List<RemoteFileDesc> altList) {
	    RemoteFileDesc[] rfds = new RemoteFileDesc[searchResults.size()];
        Set<IpPort> alts = new IpPortSet();
        
        for(int i = 0; i < searchResults.size(); i++) {
            RemoteFileDescAdapter rfdAdapter = (RemoteFileDescAdapter)searchResults.get(i);
            rfds[i] = rfdAdapter.getRfd();
            alts.addAll(rfdAdapter.getAlts());
        }        
        
        // Iterate through RFDs and remove matching alts.
        // Also store the first SHA1 capable RFD for collecting alts.
        RemoteFileDesc sha1RFD = null;
        for(int i = 0; i < rfds.length; i++) {
            RemoteFileDesc next = rfds[i];
            // this has been moved down until the download is actually started
            // next.setDownloading(true);
            next.setRetryAfter(0);
            if(next.getSHA1Urn() != null)
                sha1RFD = next;
            alts.remove(next); // Removes an alt that matches the IpPort of the RFD
        }

        // If no SHA1 rfd, just use the first.
        if(sha1RFD == null)
            sha1RFD = rfds[0];
        
        // Now iterate through alts & add more rfds.
        for(IpPort next : alts) {
            altList.add(remoteFileDescFactory.createRemoteFileDesc(sha1RFD, next));
        }
        
        return rfds;
    }


    private static class CoreDownloadListener implements DownloadListener {
	    
	    private List<DownloadItem> list;
	    private QueueTimeCalculator queueTimeCalculator;

        public CoreDownloadListener(List<DownloadItem> list, QueueTimeCalculator queueTimeCalculator){
	        this.list = list;
	        this.queueTimeCalculator = queueTimeCalculator;
	    }

        @Override
        public void downloadAdded(DownloadItem downloadItem) {
            //TODO better way of doing this
            ((CoreDownloadItem) downloadItem).setQueueTimeCalculator(queueTimeCalculator);
            list.add(downloadItem);
        }

        @Override
        public void downloadRemoved(DownloadItem downloadItem) {
            //don't automatically remove finished downloads or downloads in error states
            if (downloadItem.getState() != DownloadState.DONE && downloadItem.getState() != DownloadState.ERROR) {
                list.remove(downloadItem);
            }
        }

    }


    @Override
    public void addDownload(final URI uri, final String fileName) {

        Runnable work = new Runnable() {
            @Override
            public void run() {
                URL url = null;
                URLConnection urlConnection = null;
                try {
                    
                    url = new URL(uri.toString());
                    LOG.debugf("Adding Download: {0}", url.toString());
                    LOG.debugf("File Name: {0}", fileName);

                    urlConnection = url.openConnection();
                    long size = urlConnection.getContentLength();
                    LOG.debugf("Download Size: {0}", size);

                    URN urn = null;

                    RemoteFileDesc rfd = null;
                    rfd = remoteFileDescFactory.createUrlRemoteFileDesc(url, fileName, urn, size);
                    File saveDir = new File(MozillaSettings.DOWNLOAD_DIR.getValue()
                            .getAbsolutePath());

                    LOG.debugf("Download Save Directory: {0}", saveDir.toString());

                    saveDir.mkdirs();
                    boolean overwrite = true;
                    LOG.debugf("Download Starting");

                    // TODO instead of starting download, we will want to
                    // integrate with the file dialog for the new UI.
                    downloadManager.downloadFromStore(rfd, overwrite, saveDir, fileName);
                } catch (IOException e) {
                    LOG.error("error adding download: " + uri.toString(), e);
                } catch (URISyntaxException e) {
                    LOG.error("error adding download: " + uri.toString(), e);
                } catch (HttpException e) {
                    LOG.error("error adding download: " + uri.toString(), e);
                } catch (InterruptedException e) {
                    LOG.error("error adding download: " + uri.toString(), e);
                } finally {
                    if (urlConnection != null) {
                        try {
                            if (urlConnection.getInputStream() != null) {
                                urlConnection.getInputStream().close();
                            }
                        } catch (IOException ignored) {
                        }
                        try {
                            if (urlConnection.getOutputStream() != null) {
                                urlConnection.getOutputStream().close();
                            }
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        };

        backgroundExecutor.execute(work);
    }
}
