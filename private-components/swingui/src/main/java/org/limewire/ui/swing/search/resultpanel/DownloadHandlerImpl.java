package org.limewire.ui.swing.search.resultpanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.store.StoreController;

class DownloadHandlerImpl implements DownloadHandler {
    
    private final SearchResultsModel searchResultsModel;
    private final StoreController storeController;
    private final List<DownloadPreprocessor> downloadPreprocessors = new ArrayList<DownloadPreprocessor>();
    private final LibraryMediator libraryMediator;
    private final MainDownloadPanel mainDownloadPanel;
    
    /**
     * Starts all downloads from searches.  Navigates to Library or Downloads 
     * without downloading if the file is in either of those locations.
     */
    public DownloadHandlerImpl(SearchResultsModel searchResultsModel,
            StoreController storeController,
            LibraryMediator libraryMediator, MainDownloadPanel mainDownloadPanel) {
        this.searchResultsModel = searchResultsModel;
        this.storeController = storeController;
        this.libraryMediator = libraryMediator;
        this.mainDownloadPanel = mainDownloadPanel;

        this.downloadPreprocessors.add(new LicenseWarningDownloadPreprocessor());
    }


    @Override
    public void download(final VisualSearchResult vsr) {
        download(vsr, null);
    }

    /**
     * Downloads the file specified in vsr if it is not already downloading 
     * or in Library.  Navigates to Downloads or Library if it is already in 
     * the locations.
     */
    @Override
    public void download(final VisualSearchResult vsr, File saveFile) {
        if (maybeNavigate(vsr)){
            //do not download if we navigate away
            return;
        }

        // execute the download preprocessors
        for (DownloadPreprocessor preprocessor : downloadPreprocessors) {
            boolean shouldDownload = preprocessor.execute(vsr);
            if (!shouldDownload) {
                // do not download!
                return;
            }
        }

        // Start download.
        if (vsr instanceof VisualStoreResult) {
            storeController.download((VisualStoreResult) vsr);
        } else {
            searchResultsModel.download(vsr, saveFile);
        }
    }
    
    private boolean maybeNavigate(VisualSearchResult vsr) {
        if (vsr.getDownloadState() == BasicDownloadState.DOWNLOADED
                || vsr.getDownloadState() == BasicDownloadState.DOWNLOADING) {
            mainDownloadPanel.selectAndScrollTo(vsr.getUrn());
            return true;
        } else if (vsr.getDownloadState() == BasicDownloadState.LIBRARY) {
            libraryMediator.selectInLibrary(vsr.getUrn());
            return true;
        }
        return false;
    }

}
