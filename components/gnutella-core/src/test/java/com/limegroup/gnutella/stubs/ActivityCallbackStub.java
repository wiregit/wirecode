package com.limegroup.gnutella.stubs;

import java.io.File;
import java.util.Set;

import com.google.inject.Singleton;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 * A stub for ActivityCallback.  Does nothing.
 */
@Singleton
public class ActivityCallbackStub implements ActivityCallback {
    
    //don't delete corrupt file on detection
    public boolean delCorrupt = false;
    //if corruptness was queried
    public boolean corruptChecked = false;

    public void componentLoading(String component) {}
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {}
    public void knownHost(Endpoint e) { }
    public void handleNoInternetConnection() { }
    public void installationCorrupted() {}
    //public void handleQueryReply( QueryReply qr ) { }

	public void handleQueryResult(RemoteFileDesc rfd, 
	                              HostData data,
	                              Set alts) {}
    public void handleQueryString( String query ) { }    
    public void addDownload(Downloader d) { }    
    public void removeDownload(Downloader d) { }    
    public void addUpload(Uploader u) { }
    public void removeUpload(Uploader u) { }    	
	public void acceptChat(InstantMessenger ctr) { }
	public void receiveMessage(InstantMessenger chr, String message) { }	
	public void chatUnavailable(InstantMessenger chatter) { }	
	public void chatErrorMessage(InstantMessenger chatter, String str) { }
    public void addSharedDirectory(final File directory, final File parent) { }
    public void addSharedFile(final FileDesc file, final File parent) { }
    public boolean warnAboutSharingSensitiveDirectory(final File dir) { return false; }
	public void clearSharedFiles() { }           
    public void downloadsComplete() { }
    public void uploadsComplete() { }
    public void error(int errorCode) { }
    public void error(int errorCode, Throwable t) { }
    public void error(Throwable t) { }
    public void promptAboutCorruptDownload(Downloader dloader) {
        corruptChecked = true;
        dloader.discardCorruptDownload(delCorrupt);
    }    
    public void browseHostFailed(GUID guid) {}
	public void restoreApplication() {}
	public void showDownloads() {}
    public void setAnnotateEnabled(boolean enabled) {}
    public String getHostValue(String key) { return null;}
    public void handleSharedFileUpdate(File file) { }
    public void handleFileEvent(FileManagerEvent evt) {}
    public void fileManagerLoaded() {}
    public void updateAvailable(UpdateInformation uc) {}
    public void showError(String message, String messageKey) {}
    public boolean isQueryAlive(GUID guid) {
        return false;
    }
    public void handleAddressStateChanged() {}
    public void fileManagerLoading() {}
	public boolean handleMagnets(final MagnetOptions[] magnets) {
		return false;
	}
	public void acceptedIncomingChanged(boolean status) { }
	public void handleTorrent(File torrentFile) {
	}
}
