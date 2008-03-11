package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 *  Defines the interface of a callback to notify about asynchronous backend 
 *  events. The methods in this interface fall into the following categories:
 *
 *  <ul>
 *  <li>Query replies (for displaying results) and query strings 
 *     (for the monitor)</li>
 *  <li>Update in shared file statistics</li>
 *  <li>Change of connection state</li>
 *  <li>New or dead uploads or downloads</li>
 *  <li>New chat requests and chat messages</li>
 *  <li>Error messages</li>
 *  </ul>
 */
public interface ActivityCallback extends DownloadCallback, FileEventListener, ConnectionLifecycleListener
{
    
    /**
     * The address of the program has changed or we've
     * just accepted our first incoming connection.
     */
    public void handleAddressStateChanged();

    /**
     * Notifies the UI that a new query result has come in to the backend.
     * 
     * @param rfd the descriptor for the remote file
     * @param data the data for the host returning the result
     * @param locs the <tt>Set</tt> of alternate locations for the file
     */
	public void handleQueryResult(RemoteFileDesc rfd, HostData data, Set<? extends IpPort> locs);

    /**
     * Add a query string to the monitor screen
     */
    public void handleQueryString( String query );

    /** Add an uploader to the upload window */
    public void addUpload(Uploader u);

    /** Remove an uploader from the upload window. */
    public void removeUpload(Uploader u);    

	/**
     * Add a new incoming chat connection. This is invoked when the handshake
     * for a connection has been completed and the connection is ready for
     * sending and receiving of messages.
     */
	public void acceptChat(InstantMessenger ctr);

    /** A new message is available from the given chatter. */
	public void receiveMessage(InstantMessenger chr, String messsage);

	/** The given chatter is no longer available */
	public void chatUnavailable(InstantMessenger chatter);

	/** display an error message */
	public void chatErrorMessage(InstantMessenger chatter, String str);

    /** display an error message since the browse host failed. 
     *  @param guid The GUID of the browse host.
     */    
    public void browseHostFailed(GUID guid);
        
	/**
	 * Notification that the <code>FileManager</code> is beginning loading.
	 */
	public void fileManagerLoading();

    /**
     * Notifies a user that the file manager has completely loaded.
     */
    public void fileManagerLoaded();
    
    /**
     * Notifies that the user is attempting to share a sensitive
     * directory.  Returns true if the sensitive directory should be shared. 
     */
    public boolean warnAboutSharingSensitiveDirectory(File dir);
    
    /**
     * Notifies when a FileDesc was either added, removed, 
     * changed or renamed. This event is triggered by FileManager
     * or MetaFileManager.
     */
    public void handleFileEvent(FileManagerEvent evt);
    
    /** 
     * Notifies about connection life cycle related events.
     * This event is triggered by the ConnectionManager
     * 
     */
    public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt);
    
    /**
     * Notifies that the given shared file has new information.
     *
     * @param file The File that needs updating
     */    
    public void handleSharedFileUpdate(File file);

	/**
	 * Notification that an update became available.
	 */
	public void updateAvailable(UpdateInformation info);

	/**
	 * Sets the enabled/disabled state of file annotation.
	 */
	public void setAnnotateEnabled(boolean enabled);
    
    /** 
     * Notifies that all active uploads have been completed.
     */  
    public void uploadsComplete();

	/**
	 *  Tell to deiconify.
	 */
	public void restoreApplication();

    /**
     * @return true If the <code>guid</code> that maps to a query result screen 
     * is still available/viewable to the user.
     */
    public boolean isQueryAlive(GUID guid);
    
    /**
     * Indicates a component is loading.
     */
    public void componentLoading(String component);
    
    /** Notification that installation may be corrupted. */
    public void installationCorrupted();
	
	/**
	 * The core passes parsed magnets to the callback and asks it if it wants
	 * to handle them itself.
	 * <p>
	 * If this is the case the callback should return <code>true</code>, otherwise
	 * the core starts the downloads itself.
	 * @param magnets Array of magnet information to handle
	 * @return true if the callback handles the magnet links
	 */
	public boolean handleMagnets(MagnetOptions[] magnets);
	
	/**
	 * Indicates that the firewalled state of this has changed. 
	 */
	public void acceptedIncomingChanged(boolean status);
	
	/** Try to download the torrent file */
	public void handleTorrent(File torrentFile);
}
