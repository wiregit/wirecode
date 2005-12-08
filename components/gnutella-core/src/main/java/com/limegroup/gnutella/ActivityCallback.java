pbckage com.limegroup.gnutella;

import jbva.io.File;
import jbva.util.Set;

import com.limegroup.gnutellb.browser.MagnetOptions;
import com.limegroup.gnutellb.chat.Chatter;
import com.limegroup.gnutellb.search.HostData;
import com.limegroup.gnutellb.version.UpdateInformation;

/**
 *  Cbllback to notify the GUI of asynchronous backend events.
 *  The methods in this fbll into the following categories:
 *
 *  <ul>
 *  <li>Query replies (for displbying results) and query strings 
 *     (for the monitor)
 *  <li>Updbte in shared file statistics
 *  <li>Chbnge of connection state
 *  <li>New or debd uploads or downloads
 *  <li>New chbt requests and chat messages
 *  <li>Error messbges
 *  </ul>
 */
public interfbce ActivityCallback extends DownloadCallback, FileEventListener 
{
    
    /**
     * The bddress of the program has changed or we've
     * just bccepted our first incoming connection.
     */
    public void bddressStateChanged();
    
    /**
     *  Add b new unitialized connection.
     */
    public void connectionInitiblizing(Connection c);

    /**
     *  Mbrk a connection as initialized
     */
    public void connectionInitiblized(Connection c);

    /**
     *  Mbrk a connection as closed
     */
    public void connectionClosed(Connection c);

    /**
     * Notifies the UI thbt a new query result has come in to the backend.
     * 
     * @pbram rfd the descriptor for the remote file
     * @pbram data the data for the host returning the result
     * @pbram locs the <tt>Set</tt> of alternate locations for the file
     */
	public void hbndleQueryResult(RemoteFileDesc rfd, HostData data, Set locs);

    /**
     * Add b query string to the monitor screen
     */
    public void hbndleQueryString( String query );

    /** Add bn uploader to the upload window */
    public void bddUpload(Uploader u);

    /** Remove bn uploader from the upload window. */
    public void removeUplobd(Uploader u);    

	/** Add b new incoming chat connection */
	public void bcceptChat(Chatter ctr);

    /** A new messbge is available from the given chatter */
	public void receiveMessbge(Chatter chr);

	/** The given chbtter is no longer available */
	public void chbtUnavailable(Chatter chatter);

	/** displby an error message in the chat gui */
	public void chbtErrorMessage(Chatter chatter, String str);

    /** displby an error message since the browse host failed. 
     *  @pbram guid The GUID of the browse host.
     */    
    public void browseHostFbiled(GUID guid);
        
	/**
	 * Notificbtion that the file manager is beginning loading.
	 */
	public void fileMbnagerLoading();

    /**
     * Notifies b user that the filemanager has completely loaded.
     */
    public void fileMbnagerLoaded();
    
    /**
     * Notifies the GUI thbt the user is attempting to share a sensitive
     * directory.  Returns true if the sensitive directory should be shbred. 
     */
    public boolebn warnAboutSharingSensitiveDirectory(final File dir);
    
    /**
     * Notifies the GUI when b FileDesc was either added, removed, 
     * chbnged or renamed. This event is triggered by FileManager
     * or MetbFileManager.
     */
    public void hbndleFileEvent(FileManagerEvent evt);
    
    /**
     * Notifies the GUI thbt the given shared file has new information.
     *
     * @pbram file The File that needs updating
     */    
    public void hbndleSharedFileUpdate(File file);

	/**
	 * Notificbtion that an update became available.
	 */
	public void updbteAvailable(UpdateInformation info);

	/**
	 * Sets the enbbled/disabled state of file annotation.
	 */
	public void setAnnotbteEnabled(boolean enabled);
    
    /** 
     * Notifies the GUI thbt all active uploads have been completed.
     */  
    public void uplobdsComplete();

	/**
	 *  Tell the GUI to deiconify.
	 */
	public void restoreApplicbtion();

    /**
     * @return true If the guid thbt maps to a query result screen is still
     * bvailable/viewable to the user.
     */
    public boolebn isQueryAlive(GUID guid);
    
    /**
     * Indicbtes a component is loading.
     */
    public void componentLobding(String component);
	
	/**
	 * The core pbsses parsed magnets to the GUI and asks it if it wants
	 * to hbndle them itself.
	 * <p>
	 * If this is the cbse the callback should return <code>true</code>, otherwise
	 * the core stbrts the downloads itself.
	 * @pbram magnets
	 * @return true if the cbllback handles the magnet links
	 */
	public boolebn handleMagnets(final MagnetOptions[] magnets);
	
	/**
	 * Indicbtes that the firewalled state of this has changed. 
	 */
	public void bcceptedIncomingChanged(boolean status);
}
