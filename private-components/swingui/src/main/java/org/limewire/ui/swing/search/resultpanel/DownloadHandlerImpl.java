package org.limewire.ui.swing.search.resultpanel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.DownloadItemPropertyListener;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

class DownloadHandlerImpl implements DownloadHandler {
    
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;
    private final Search search;
    private final DownloadListManager downloadListManager;    
    private List<DownloadPreprocessor> downloadPreprocessors = new ArrayList<DownloadPreprocessor>();
    private Navigator navigator;
    private LibraryNavigator libraryNavigator;
    
    /**
     * Starts all downloads from searches.  Navigates to Library or Downloads without downloading if the file is in either of those locations.
     */
    public DownloadHandlerImpl(Search search, SaveLocationExceptionHandler saveLocationExceptionHandler, DownloadListManager downloadListManager, 
            Navigator navigator, LibraryNavigator libraryNavigator) {
        this.search = search;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.downloadListManager = downloadListManager;
        this.navigator = navigator;
        this.libraryNavigator = libraryNavigator;

        this.downloadPreprocessors.add(new LicenseWarningDownloadPreprocessor());
    }


    @Override
    public void download(final VisualSearchResult vsr) {
        download(vsr, null);
    }

    /**
     * Downloads the file specified in vsr if it is not already downloading or in Library.  Navigates to Downloads or Library if it is already in the locations.
     */
    @Override
    public void download(final VisualSearchResult vsr, File saveFile) {
        if (maybeNavigate(vsr)){
            //do not download if we navigate away
            return;
        }
        
        try {
            // execute the download preprocessors
            for (DownloadPreprocessor preprocessor : downloadPreprocessors) {
                boolean shouldDownload = preprocessor.execute(vsr);
                if (!shouldDownload) {
                    // do not download!
                    return;
                }
            }

            // Add download to manager.  If save file is specified, then set
            // overwrite to true because the user has already confirmed it.
            DownloadItem di = (saveFile == null) ?
                    downloadListManager.addDownload(search, vsr.getCoreSearchResults()) :
                    downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, true);
            
            // Add listener, and initialize download state.
            di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
            vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
            
        } catch (final SaveLocationException sle) {
            if (sle.getErrorCode() == SaveLocationException.LocationCode.FILE_ALREADY_DOWNLOADING) {
                DownloadItem downloadItem = downloadListManager.getDownloadItem(vsr.getUrn());
                if (downloadItem != null) {
                    downloadItem.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                    vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                    if (saveFile != null) {
                        try {
                            // Update save file in DownloadItem.
                            downloadItem.setSaveFile(saveFile, true);
                        } catch (SaveLocationException ex) {
                            FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), 
                                    I18n.tr("Unable to relocate downloading file {0}", ex.getMessage()), 
                                    I18n.tr("Download"), JOptionPane.INFORMATION_MESSAGE);
                        }
                    }
                }
            } else {
                saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                    @Override
                    public void download(File saveFile, boolean overwrite)
                            throws SaveLocationException {
                        DownloadItem di = downloadListManager.addDownload(search, vsr.getCoreSearchResults(), saveFile, overwrite);
                        di.addPropertyChangeListener(new DownloadItemPropertyListener(vsr));
                        vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                    }
                }, sle, true);
            }
        }
    }
    
    private boolean maybeNavigate(VisualSearchResult vsr) {
        if (vsr.getDownloadState() == BasicDownloadState.DOWNLOADED
                || vsr.getDownloadState() == BasicDownloadState.DOWNLOADING) {
            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).select(vsr);
            return true;
        } else if (vsr.getDownloadState() == BasicDownloadState.LIBRARY) {
            libraryNavigator.selectInLibrary(vsr.getUrn(), vsr.getCategory());
            return true;
        }
        return false;
    }

}
