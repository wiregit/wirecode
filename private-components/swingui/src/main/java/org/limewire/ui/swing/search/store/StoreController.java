package org.limewire.ui.swing.search.store;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.http.cookie.Cookie;
import org.limewire.core.api.Application;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.store.StoreDownloadToken;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.core.api.search.store.StoreManager.AttributeKey;
import org.limewire.core.api.search.store.TrackResult;
import org.limewire.ui.swing.player.Audio;
import org.limewire.ui.swing.player.PlayerMediator;
import org.limewire.ui.swing.search.model.VisualStoreResult;

import com.google.inject.Inject;

/**
 * A class that manages the interactions between the Store UI and various 
 * application and core services.
 */
public class StoreController {

    private final Application application;
    private final StoreManager storeManager;
    private final PlayerMediator playerMediator;
    
    /**
     * Constructs a StoreController using the specified services.
     */
    @Inject
    public StoreController(
            Application application,
            StoreManager storeManager, @Audio PlayerMediator playerMediator) {
        this.application = application;
        this.storeManager = storeManager;
        this.playerMediator = playerMediator;
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
     * Sets the user cookies on successful login.
     */
    public void login(List<Cookie> cookieList) {
        storeManager.setUserAttribute(AttributeKey.COOKIES, cookieList);
    }
    
    /**
     * Logs out of the store.
     */
    public void logout() {
        storeManager.logout();
    }
    
    /**
     * Initiates downloading of the specified visual store result.
     */
    public void download(VisualStoreResult vsr) {
        // Validate download.  Display browser dialog if required, or perform
        // download if approved.
        StoreDownloadToken downloadToken = storeManager.validateDownload(vsr.getStoreResult());
        switch (downloadToken.getStatus()) {
        case LOGIN_REQ:
        case CONFIRM_REQ:
            new StoreBrowserPanel(this).showDownload(downloadToken, vsr);
            break;
            
        case APPROVED:
            doDownload(vsr);
            break;
            
        default:
            throw new IllegalStateException("Unknown download status " + downloadToken.getStatus());
        }
    }

    /**
     * Initiates downloading of the specified store track result.
     */
    public void downloadTrack(TrackResult str) {
        // Validate download.  Display browser dialog if required, or perform
        // download if approved.
        StoreDownloadToken downloadToken = storeManager.validateDownload(str);
        switch (downloadToken.getStatus()) {
        case LOGIN_REQ:
        case CONFIRM_REQ:
            new StoreBrowserPanel(this).showDownload(downloadToken, str);
            break;
            
        case APPROVED:
            doDownloadTrack(str);
            break;
            
        default:
            throw new IllegalStateException("Unknown download status " + downloadToken.getStatus());
        }
    }
    
    /**
     * Performs download of specified store result.
     */
    void doDownload(VisualStoreResult vsr) {
        // TODO implement
        System.out.println("StoreController.doDownload: " + vsr.getHeading());
    }
    
    /**
     * Performs download of specified store track result.
     */
    void doDownloadTrack(TrackResult str) {
        // TODO implement
        System.out.println("StoreController.doDownloadTrack: " + str.getProperty(FilePropertyKey.TITLE));
    }

    /**
     * Initiates streaming of the specified visual store result.
     */
    public void stream(VisualStoreResult vsr) {
        try {
            playerMediator.play(new URL(vsr.getStoreResult().getStreamURI()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Initiates streaming of the specified store track result.
     */
    public void streamTrack(TrackResult str) {
        try {
            playerMediator.play(new URL(str.getStreamURI()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
