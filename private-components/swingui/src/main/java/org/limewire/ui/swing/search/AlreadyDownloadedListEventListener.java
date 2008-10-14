package org.limewire.ui.swing.search;

import java.util.List;

import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

public class AlreadyDownloadedListEventListener implements ListEventListener<VisualSearchResult> {
    private final LibraryManager libraryManager;

    private final DownloadListManager downloadListManager;

    public AlreadyDownloadedListEventListener(LibraryManager libraryManager,
            DownloadListManager downloadListManager) {
        this.libraryManager = libraryManager;
        this.downloadListManager = downloadListManager;
    }

    @Override
    public void listChanged(ListEvent<VisualSearchResult> listChanges) {
        LocalFileList localFileList = libraryManager.getLibraryManagedList();
        //TODO bad to be using instanceof here.
        if (localFileList instanceof LibraryFileList) {
            LibraryFileList libraryFileList = (LibraryFileList) localFileList;
            final EventList<VisualSearchResult> eventList = listChanges.getSourceList();
            while (listChanges.next()) {
                boolean addOrUpdate = listChanges.getType() == ListEvent.INSERT
                        || listChanges.getType() == ListEvent.UPDATE;
                if (addOrUpdate) {
                    final VisualSearchResult visualSearchResult = eventList.get(listChanges
                            .getIndex());
                    //TODO should probably check more than just URN, can check the file save path as well.
                    URN urn = visualSearchResult.getURN();
                    if (libraryFileList.contains(urn)) {
                        //first checking library for file
                        visualSearchResult.setDownloadState(BasicDownloadState.LIBRARY);
                    } else {
                        //next checking download list
                        List<DownloadItem> downloads = downloadListManager.getDownloads();
                        // TODO instead of iterating through loop, it would be
                        // nice to lookup download by urn potentially.
                        for (DownloadItem downloadItem : downloads) {
                            if (urn.equals(downloadItem.getUrn())) {
                                downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(visualSearchResult));
                                visualSearchResult.setDownloadState(BasicDownloadState.DOWNLOADING);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
