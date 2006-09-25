package com.limegroup.gnutella;

import java.io.File;
import java.util.Set;

import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.chat.Chatter;
import com.limegroup.gnutella.search.HostData;
import com.limegroup.gnutella.version.UpdateInformation;

/**
 *  Callback to notify the GUI of asynchronous backend events.
 *  The methods in this fall into the following categories:
 *
 *  <ul>
 *  <li>Query replies (for displaying results) and query strings 
 *     (for the monitor)
 *  <li>Update in shared file statistics
 *  <li>Change of connection state
 *  <li>New or dead uploads or downloads
 *  <li>New chat requests and chat messages
 *  <li>Error messages
 *  </ul>
 */
public interface ActivityCallback extends DownloadCallback, FileEventListener {
    
    /**
     * The address of the program has changed or we've
     * just accepted our first incoming connection.
     */
    public void addressStateChanged();
    
    /**
     *  Add a new unitialized connection.
     */
    public void connectionInitializing(ManagedConnection c);

    /**
     *  Mark a connection as initialized
     */
    public void connectionInitialized(ManagedConnection c);

    /**
     *  Mark a connection as closed
     */
    public void connectionClosed(ManagedConnection c);
    
    /**
     * Notify the UI that we are disconnected from the network
     * 
     */
    public void disconnected();
    

    /**
     * Notifies the UI that a new query result has come in to the backend.
     * 
     * @param rfd the descriptor for the remote file
     * @param data the data for the host returning the result
     * @param locs the <tt>Set</tt> of alternate locations for the file
     */
	public void handleQueryResult(RemoteFileDesc rfd, HostData data, Set<Endpoint> locs);

    /**
     * Add a query string to the monitor screen
     */
    public void handleQueryString( String query );

    /** Add an uploader to the upload window */
    public void addUpload(Uploader u);

    /** Remove an uploader from the upload window. */
    public void removeUpload(Uploader u);    

	/** Add a new incoming chat connection */
	public void acceptChat(Chatter ctr);

    /** A new message is available from the given chatter */
	public void receiveMessage(Chatter chr);

	/** The given chatter is no longer available */
	public void chatUnavailable(Chatter chatter);

	/** display an error message in the chat gui */
	public void chatErrorMessage(Chatter chatter, String str);

    /** display an error message since the browse host failed. 
     *  @param guid The GUID of the browse host.
     */    
    public void browseHostFailed(GUID guid);
        
	/**
	 * Notification that the file manager is beginning loading.
	 */
	public void fileManagerLoading();

    /**
     * Notifies a user that the filemanager has completely loaded.
     */
    public void fileManagerLoaded();
    
    /**
     * Notifies the GUI that the user is attempting to share a sensitive
     * directory.  Returns true if the sensitive directory should be shared. 
     */
    public boolean warnAboutSharingSensitiveDirectory(final File dir);
    
    /**
     * Notifies the GUI when a FileDesc was either added, removed, 
     * changed or renamed. This event is triggered by FileManager
     * or MetaFileManager.
     */
    public void handleFileEvent(FileManagerEvent evt);
    
    /**
     * Notifies the GUI that the given shared file has new information.
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
     * Notifies the GUI that all active uploads have been completed.
     */  
    public void uploadsComplete();

	/**
	 *  Tell the GUI to deiconify.
	 */
	public void restoreApplication();

    /**
     * @return true If the guid that maps to a query result screen is still
     * available/viewable to the user.
     */
    public boolean isQueryAlive(GUID guid);
    
    /**
     * Indicates a component is loading.
     */
    public void componentLoading(String component);
	
	/**
	 * The core passes parsed magnets to the GUI and asks it if it wants
	 * to handle them itself.
	 * <p>
	 * If this is the case the callback should return <code>true</code>, otherwise
	 * the core starts the downloads itself.
	 * @param magnets
	 * @return true if the callback handles the magnet links
	 */
	public boolean handleMagnets(final MagnetOptions[] magnets);
	
	/**
	 * Indicates that the firewalled state of this has changed. 
	 */
	public void acceptedIncomingChanged(boolean status);
	
	/** Try to download the torrent file */
	public void handleTorrent(File torrentFile);
}
