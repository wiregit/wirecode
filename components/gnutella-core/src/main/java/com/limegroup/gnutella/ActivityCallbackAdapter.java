package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import org.limewire.io.IpPort;

import com.google.inject.Singleton;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * Provides a default implementation of <code>ActivityCallback</code> where
 * all the methods are either empty or return <code>false</code>. You can extend 
 * this class when you are only need specific methods.
 */
@Singleton
public class ActivityCallbackAdapter implements ActivityCallback {

    public void acceptChat(InstantMessenger ctr) {
        
    }

    public void acceptedIncomingChanged(boolean status) {
        
    }

    public void addUpload(Uploader u) {
        
    }

    public void browseHostFailed(GUID guid) {
        
    }

    public void chatErrorMessage(InstantMessenger chatter, String str) {
        
    }

    public void chatUnavailable(InstantMessenger chatter) {
        
    }

    public void componentLoading(String component) {
        
    }

    public void fileManagerLoaded() {
        
    }

    public void fileManagerLoading() {
        
    }

    public void handleAddressStateChanged() {
        
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        
    }

    public void handleFileEvent(FileManagerEvent evt) {
        
    }

    public boolean handleMagnets(MagnetOptions[] magnets) {
        return false;
    }

    public void handleQueryResult(RemoteFileDesc rfd, HostData data,
            Set<? extends IpPort> locs) {
        
    }

    public void handleQueryString(String query) {
        
    }

    public void handleSharedFileUpdate(File file) {
        
    }

    public void handleTorrent(File torrentFile) {
        
    }

    public void installationCorrupted() {
        
    }

    public boolean isQueryAlive(GUID guid) {
        return false;
    }

    public void receiveMessage(InstantMessenger chr, String messsage) {
        
    }

    public void removeUpload(Uploader u) {
        
    }

    public void restoreApplication() {
        
    }

    public void setAnnotateEnabled(boolean enabled) {
        
    }

    public void updateAvailable(UpdateInformation info) {
        
    }

    public void uploadsComplete() {
        
    }

    public boolean warnAboutSharingSensitiveDirectory(File dir) {
        return false;
    }

    public void addDownload(Downloader d) {
        
    }

    public void downloadsComplete() {
        
    }

    public String getHostValue(String key) {
        return null;
    }

    public void promptAboutCorruptDownload(Downloader dloader) {
       
    }

    public void removeDownload(Downloader d) {
        
    }

    public void showDownloads() {
        
    }

}
