package org.limewire.ui.swing.downloads.table;

import java.io.File;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.KeywordAssistedSearchBuilder;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.DownloadExceptionHandler;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DownloadActionHandler {
    
    public final static String PAUSE_COMMAND = "pause";
    /**Cancels download, deletes file, and removes download from list*/
    public final static String CANCEL_COMMAND = "cancel";
    public final static String RESUME_COMMAND = "resume";
    public final static String TRY_AGAIN_COMMAND = "try again";
    public final static String LAUNCH_COMMAND = "launch";
    public final static String PREVIEW_COMMAND = "preview";
    public final static String PLAY_COMMAND = "play";
    /**Removes download from list.  Has no other effects.*/
    public final static String REMOVE_COMMAND = "remove";
    public final static String LOCATE_COMMAND = "locate";
    public final static String LIBRARY_COMMAND = "library";
    public final static String PROPERTIES_COMMAND = "properties";
    public final static String LINK_COMMAND = "link";
    public final static String SHARE_COMMAND = "share";
    public final static String CHANGE_LOCATION_COMMAND = "change location";
    public final static String SEARCH_AGAIN_COMMAND = "search again";
    
   // private static final String ERROR_URL = "http://wiki.limewire.org/index.php?title=User_Guide_Download";
    
    private final LibraryMediator libraryMediator;
    private DownloadListManager downloadListManager;
//    private ShareWidget<File> shareWidget = null;
    private LibraryManager libraryManager;
    private final FileInfoDialogFactory fileInfoFactory;
//    private final Provider<ShareWidgetFactory> shareFactory;
    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;
    private final SearchHandler searchHandler;
    private final KeywordAssistedSearchBuilder searchBuilder;
    
    @Inject
    public DownloadActionHandler(//Provider<ShareWidgetFactory> shareFactory, 
            DownloadListManager downloadListManager, 
            LibraryMediator libraryMediator, LibraryManager libraryManager, FileInfoDialogFactory fileInfoFactory,
            Provider<DownloadExceptionHandler> downloadExceptionHandler,
            SearchHandler searchHandler,
            KeywordAssistedSearchBuilder searchBuilder){
        this.downloadListManager = downloadListManager;
//        this.shareFactory = shareFactory;
        this.libraryMediator = libraryMediator;
        this.libraryManager = libraryManager;
        this.fileInfoFactory = fileInfoFactory;
        this.downloadExceptionHandler = downloadExceptionHandler;
        this.searchHandler = searchHandler;
        this.searchBuilder = searchBuilder;
    }

    public void performAction(final String actionCommmand, final DownloadItem item){
        if (actionCommmand == CANCEL_COMMAND) {
            item.cancel();
        } else if (actionCommmand == PAUSE_COMMAND) {
            item.pause();
        } else if (actionCommmand == RESUME_COMMAND) {
            item.resume();
        } else if (actionCommmand == TRY_AGAIN_COMMAND) {
            item.resume();
        } else if (actionCommmand == LINK_COMMAND){
            //Do nothing for now
           // NativeLaunchUtils.openURL(ERROR_URL);
        } else if (actionCommmand == PREVIEW_COMMAND || actionCommmand == LAUNCH_COMMAND || actionCommmand == PLAY_COMMAND){
            if (item.isLaunchable()) {
                DownloadItemUtils.launch(item);
            }
        } else if (actionCommmand == LOCATE_COMMAND){
            if(item.getDownloadingFile() != null) {
                NativeLaunchUtils.launchExplorer(item.getDownloadingFile());
            }
        } else if (actionCommmand == PROPERTIES_COMMAND){
            if(item.getState() != DownloadState.DONE) {
                fileInfoFactory.createFileInfoDialog(item, FileInfoType.DOWNLOADING_FILE);
            } else {
                // if finished downloading, try showing all the information from the localFileItem
                LocalFileItem localItem = libraryManager.getLibraryManagedList().getFileItem(item.getLaunchableFile());
                if(localItem != null)
                    fileInfoFactory.createFileInfoDialog(localItem, FileInfoType.LOCAL_FILE);
                else  // if can't find the localFileItem, revert to the downloadItem
                    fileInfoFactory.createFileInfoDialog(item, FileInfoType.DOWNLOADING_FILE);
            }
        } else if (actionCommmand == REMOVE_COMMAND){
            downloadListManager.remove(item);
        } else if (actionCommmand == SHARE_COMMAND){
//            if(shareWidget == null)
//                shareWidget = shareFactory.get().createFileShareWidget();
//            shareWidget.setShareable(item.getDownloadingFile());
//            shareWidget.show(null);
        } else if( actionCommmand == LIBRARY_COMMAND) {
            File file = item.getState() == DownloadState.DONE ? item.getLaunchableFile() : item.getDownloadingFile();
            URN urn = item.getUrn();
            
            if(file != null) {
                libraryMediator.selectInLibrary(file);
            } else if (urn != null){
                libraryMediator.selectInLibrary(urn);
            }
        } else if (actionCommmand == CHANGE_LOCATION_COMMAND){
            changeSaveLocation(item);
        } else if (actionCommmand == SEARCH_AGAIN_COMMAND) {            
            searchHandler.doSearch(createSearchInfo(item));
        }
    }
    
    private SearchInfo createSearchInfo(DownloadItem item) {
        String title = item.getPropertyString(FilePropertyKey.TITLE);
        if(title == null) {
            title = FileUtils.getFilenameNoExtension(item.getFileName());
        }
        
        // make search based on on title and category
        SearchInfo search = searchBuilder.attemptToCreateAdvancedSearch(title, SearchCategory
                .forCategory(item.getCategory()));

        if (search != null) {
            return search;
        }

        // Fall back on the normal search
        return search = DefaultSearchInfo.createKeywordSearch(title, SearchCategory.forCategory(item.getCategory()));
    }
    
    private void changeSaveLocation(DownloadItem item){
        // Prompt user for a new directory.
        File saveDir = FileChooser.getInputDirectory(GuiUtils.getMainFrame(), item.getSaveFile().getParentFile());
        
        if (saveDir == null || saveDir.equals(item.getSaveFile().getParentFile())){
            //nothing to see here.  move along.
            return;
        }
        setSaveFile(item, saveDir, false);        
    }
    
    private void setSaveFile(DownloadItem item, File saveDir, boolean overwrite){
        try {
            // Update save file in DownloadItem.
            item.setSaveFile(saveDir, overwrite);
        } catch (DownloadException ex) {
            downloadExceptionHandler.get().handleDownloadException(new ChangeLocationDownloadAction(item), ex, false);
        }
    }
    
    /**
     * Calls changeSaveLocation() on downloadCanceled()
     */
    private class ChangeLocationDownloadAction implements DownloadAction{
        private DownloadItem item;

        ChangeLocationDownloadAction(DownloadItem item){
            this.item = item;
        }
        @Override
        public void download(File saveFile, boolean overwrite) throws DownloadException {
            setSaveFile(item, saveFile, overwrite);
        }

        @Override
        public void downloadCanceled(DownloadException ignored) {
            // do nothing
        }        
    }
}
