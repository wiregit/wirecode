package com.limegroup.gnutella.tests.stubs;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.chat.*;
import com.limegroup.gnutella.security.*;
import java.io.*;

/**
 * A stub for ActivityCallback.  Does nothing.
 */
public class ActivityCallbackStub implements ActivityCallback {
    
    //don't delete corrupt file on detection
    public static boolean delCorrupt = false;

    public void connectionInitializing(Connection c) { }
    public void connectionInitialized(Connection c) { }
    public void connectionClosed(Connection c) { }
    public void knownHost(Endpoint e) { }
    public void handleQueryReply( QueryReply qr ) { }
    public void handleQueryString( String query ) { }    
    public void addDownload(Downloader d) { }    
    public void removeDownload(Downloader d) { }    
    public void addUpload(Uploader u) { }
    public void removeUpload(Uploader u) { }    	
	public void acceptChat(Chatter ctr) { }
	public void receiveMessage(Chatter chr) { }	
	public void chatUnavailable(Chatter chatter) { }	
	public void chatErrorMessage(Chatter chatter, String str) { }
    public void addSharedDirectory(final File directory, final File parent) { }
    public void addSharedFile(final File file, final File parent) { }
	public void clearSharedFiles() { }           
    public void downloadsComplete() { }
    public void uploadsComplete() { }
    public void error(int errorCode) { }
    public void error(int errorCode, Throwable t) { }
    public void error(Throwable t) { }
    public User getUserAuthenticationInfo(String host) { 
        return null;
    }    
    public void promptAboutCorruptDownload(Downloader dloader) {
        dloader.discardCorruptDownload(delCorrupt);
    }    
    public void browseHostFailed(GUID guid) {};
}
