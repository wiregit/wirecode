package org.limewire.core.impl.download;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.glazedlists.GlazedListsFactory;
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
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.io.Connectable;
import org.limewire.io.Address;
import org.limewire.setting.FileSetting;
import org.limewire.xmpp.api.client.FileMetaData;
import org.limewire.xmpp.api.client.LimePresence;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.net.address.FirewalledAddress;
import com.limegroup.gnutella.downloader.RemoteFileDescFactory;


@Singleton
public class CoreDownloadListManager implements DownloadListManager {
    
	private final EventList<DownloadItem> downloadItems;
	private final DownloadManager downloadManager;
	private final RemoteFileDescFactory remoteFileDescFactory;
	private final QueueTimeCalculator queueTimeCalculator;
    
    private static final int PERIOD = 1000;
	
	@Inject
	public CoreDownloadListManager(DownloadManager downloadManager,
            DownloadListenerList listenerList, @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor,
            RemoteFileDescFactory remoteFileDescFactory) {
	    this.downloadManager = downloadManager;
	    this.remoteFileDescFactory = remoteFileDescFactory;
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = GlazedListsFactory.threadSafeList(
	            GlazedListsFactory.observableElementList(new BasicEventList<DownloadItem>(), downloadConnector));
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

    @Override
    public DownloadItem addDownload(LimePresence presence, FileMetaData fileMeta) throws IOException {

        RemoteFileDesc rfd = createRfdFromChatResult(presence, fileMeta);
        File saveDir = null;
        String fileName = null;
        boolean overwrite = false;

        RemoteFileDesc[] files = { rfd };
        List<RemoteFileDesc> altLocList = Collections.emptyList();

        Downloader downloader = downloadManager.download(files, altLocList, null, overwrite, saveDir, fileName);

        return new CoreDownloadItem(downloader, queueTimeCalculator);
    }

    private RemoteFileDesc createRfdFromChatResult(LimePresence presence, FileMetaData fileMeta) throws IOException {
        Connectable publicAddress;
        Address address = presence.getPresenceAddress();
        byte[] clientGuid = null;
        boolean firewalled;
        Set<Connectable> proxies = null;
        int fwtVersion = 0;
        Set<URN> urns = fileMeta.getURNs();

        if (address instanceof FirewalledAddress) {
            firewalled = true;
            FirewalledAddress fwAddress = (FirewalledAddress) address;
            publicAddress = fwAddress.getPublicAddress();
            clientGuid = fwAddress.getClientGuid().bytes();
            proxies = fwAddress.getPushProxies();
            fwtVersion = fwAddress.getFwtVersion();
        } else {
            assert address instanceof Connectable;
            firewalled = false;
            publicAddress = (Connectable) address;
        }

        return remoteFileDescFactory.createRemoteFileDesc(publicAddress.getAddress(), publicAddress.getPort(),
                fileMeta.getIndex(), fileMeta.getName(), fileMeta.getSize(), clientGuid,
                0, false, 0, true, null, urns, false,
                firewalled, null, proxies, fileMeta.getCreateTime().getTime(), fwtVersion,
                publicAddress.isTLSCapable());
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
}
