package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import org.limewire.io.IpPort;

import com.google.inject.Singleton;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.version.UpdateInformation;

@Singleton
public class ActivityCallbackAdapter implements ActivityCallback {

    public void acceptChat(Chatter ctr) {
        // TODO Auto-generated method stub
        
    }

    public void acceptedIncomingChanged(boolean status) {
        // TODO Auto-generated method stub
        
    }

    public void addUpload(Uploader u) {
        // TODO Auto-generated method stub
        
    }

    public void browseHostFailed(GUID guid) {
        // TODO Auto-generated method stub
        
    }

    public void chatErrorMessage(Chatter chatter, String str) {
        // TODO Auto-generated method stub
        
    }

    public void chatUnavailable(Chatter chatter) {
        // TODO Auto-generated method stub
        
    }

    public void componentLoading(String component) {
        // TODO Auto-generated method stub
        
    }

    public void fileManagerLoaded() {
        // TODO Auto-generated method stub
        
    }

    public void fileManagerLoading() {
        // TODO Auto-generated method stub
        
    }

    public void handleAddressStateChanged() {
        // TODO Auto-generated method stub
        
    }

    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public void handleFileEvent(FileManagerEvent evt) {
        // TODO Auto-generated method stub
        
    }

    public boolean handleMagnets(MagnetOptions[] magnets) {
        // TODO Auto-generated method stub
        return false;
    }

    public void handleQueryResult(RemoteFileDesc rfd, HostData data,
            Set<? extends IpPort> locs) {
        // TODO Auto-generated method stub
        
    }

    public void handleQueryString(String query) {
        // TODO Auto-generated method stub
        
    }

    public void handleSharedFileUpdate(File file) {
        // TODO Auto-generated method stub
        
    }

    public void handleTorrent(File torrentFile) {
        // TODO Auto-generated method stub
        
    }

    public void installationCorrupted() {
        // TODO Auto-generated method stub
        
    }

    public boolean isQueryAlive(GUID guid) {
        // TODO Auto-generated method stub
        return false;
    }

    public void receiveMessage(Chatter chr, String messsage) {
        // TODO Auto-generated method stub
        
    }

    public void removeUpload(Uploader u) {
        // TODO Auto-generated method stub
        
    }

    public void restoreApplication() {
        // TODO Auto-generated method stub
        
    }

    public void setAnnotateEnabled(boolean enabled) {
        // TODO Auto-generated method stub
        
    }

    public void updateAvailable(UpdateInformation info) {
        // TODO Auto-generated method stub
        
    }

    public void uploadsComplete() {
        // TODO Auto-generated method stub
        
    }

    public boolean warnAboutSharingSensitiveDirectory(File dir) {
        // TODO Auto-generated method stub
        return false;
    }

    public void addDownload(Downloader d) {
        // TODO Auto-generated method stub
        
    }

    public void downloadsComplete() {
        // TODO Auto-generated method stub
        
    }

    public String getHostValue(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    public void promptAboutCorruptDownload(Downloader dloader) {
        // TODO Auto-generated method stub
        
    }

    public void removeDownload(Downloader d) {
        // TODO Auto-generated method stub
        
    }

    public void showDownloads() {
        // TODO Auto-generated method stub
        
    }

}
