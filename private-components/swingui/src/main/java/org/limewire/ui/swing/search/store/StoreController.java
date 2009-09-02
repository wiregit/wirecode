package org.limewire.ui.swing.search.store;

import java.io.File;

import org.limewire.core.api.Application;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreTrackResult;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.resultpanel.DownloadHandler;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A class that manages the interactions between the Store UI and various 
 * application and core services.
 */
public class StoreController {

    private final SearchResultsModel searchResultsModel;
    private final Application application;
    private final StoreManager storeManager;
    
    /**
     * Constructs a StoreController using the specified services.
     */
    @Inject
    public StoreController(
            @Assisted SearchResultsModel searchResultsModel,
            Application application,
            StoreManager storeManager) {
        this.searchResultsModel = searchResultsModel;
        this.application = application;
        this.storeManager = storeManager;
    }
    
    /**
     * Returns a handler for downloading search results.
     */
    public DownloadHandler getDownloadHandler() {
        // TODO implement by using factory to create concrete handler
        return new DownloadHandler() {
            @Override
            public void download(VisualSearchResult vsr) {
            }
            
            @Override
            public void download(VisualSearchResult vsr, File saveFile) {
            }
        };
    }
    
    /**
     * Returns the URI text for the confirm download page.
     */
    public String getConfirmURI() {
        return storeManager.getConfirmURI();
    }
    
    /**
     * Returns the URI text for the specified visual store result.
     */
    public String getInfoURI(VisualStoreResult vsr) {
        // Get URI for info page.
        String infoURI = vsr.getStoreResult().getInfoURI();
        
        // Add query parameters for client info.
        infoURI = application.addClientInfoToUrl(infoURI);
        
        return infoURI;
    }
    
    /**
     * Returns the URI text for the login page.
     */
    public String getLoginURI() {
        return storeManager.getLoginURI();
    }

    /**
     * Returns the current style for Lime Store results.
     */
    public StoreStyle getStoreStyle() {
        return searchResultsModel.getStoreStyle();
    }
    
    /**
     * Returns true if the user is logged into the Lime Store.
     */
    public boolean isLoggedIn() {
        return storeManager.isLoggedIn();
    }
    
    /**
     * Returns true if store results are pay-as-you-go.
     */
    public boolean isPayAsYouGo() {
        // TODO also return true if user owns pay-as-you-go account
        return !storeManager.isLoggedIn();
    }
    
    /**
     * Initiates downloading of the specified visual store result.
     */
    public void download(VisualStoreResult vsr) {
        if (!storeManager.isLoggedIn()) {
            startLogin();
            
        } else if (!storeManager.isDownloadApproved(vsr.getStoreResult())) {
            startApproval(vsr.getStoreResult());
                
        } else {
            // TODO implement
            System.out.println("StoreController.download: " + vsr.getHeading());
        }
    }

    /**
     * Initiates downloading of the specified store track result.
     */
    public void downloadTrack(StoreTrackResult str) {
        if (!storeManager.isLoggedIn()) {
            startLogin();
            
        } else if (!storeManager.isDownloadApproved(str)) {
            startApproval(str);
            
        } else {
            // TODO implement
            System.out.println("StoreController.downloadTrack: " + str.getProperty(FilePropertyKey.NAME));
        }
    }

    /**
     * Initiates streaming of the specified visual store result.
     */
    public void stream(VisualStoreResult vsr) {
        // TODO implement
        System.out.println("StoreController.stream: " + vsr.getHeading());
    }

    /**
     * Initiates streaming of the specified store track result.
     */
    public void streamTrack(StoreTrackResult str) {
        // TODO implement
        System.out.println("StoreController.streamTrack: " + str.getProperty(FilePropertyKey.NAME));
    }

    /**
     * Initiates the download approval process for the specified store result.
     */
    private void startApproval(StoreResult storeResult) {
        StoreBrowserPanel loginPanel = new StoreBrowserPanel(this);
        loginPanel.showDownload(storeResult);
    }

    /**
     * Initiates the download approval process for the specified track result.
     */
    private void startApproval(StoreTrackResult trackResult) {
        StoreBrowserPanel loginPanel = new StoreBrowserPanel(this);
        loginPanel.showDownload(trackResult);
    }

    /**
     * Initiates the login process for the store.
     */
    private void startLogin() {
        StoreBrowserPanel loginPanel = new StoreBrowserPanel(this);
        loginPanel.showLogin();
    }
}
